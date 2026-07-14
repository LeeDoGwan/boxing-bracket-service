package com.boxing.bracket.bout.admin.service;

import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.admin.dto.AdminBoutImportResponse;
import com.boxing.bracket.bout.admin.dto.AdminBoutRequest;
import com.boxing.bracket.bout.admin.dto.AdminBoutResponse;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class AdminBoutService {

    private static final List<String> IMPORT_HEADERS = List.of(
            "tournamentId",
            "ringId",
            "boutNumber",
            "matchType",
            "redAthleteId",
            "blueAthleteId",
            "totalRounds",
            "scheduledOrder",
            "eventBout"
    );

    private final BoutRepository boutRepository;
    private final TournamentRepository tournamentRepository;
    private final RingRepository ringRepository;
    private final AthleteRepository athleteRepository;

    public AdminBoutService(
            BoutRepository boutRepository,
            TournamentRepository tournamentRepository,
            RingRepository ringRepository,
            AthleteRepository athleteRepository
    ) {
        this.boutRepository = boutRepository;
        this.tournamentRepository = tournamentRepository;
        this.ringRepository = ringRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBoutResponse> getBouts(Long tournamentId) {
        validateTournamentId(tournamentId);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .map(AdminBoutResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminBoutResponse getBout(Long boutId) {
        validateBoutId(boutId);
        return boutRepository.findById(boutId)
                .map(AdminBoutResponse::from)
                .orElseThrow(BoutNotFoundException::new);
    }

    public AdminBoutResponse createBout(AdminBoutRequest request) {
        validateRequest(request);

        Bout bout = Bout.builder()
                .tournamentId(request.getTournamentId())
                .ringId(request.getRingId())
                .boutNumber(request.getBoutNumber())
                .matchType(request.getMatchType())
                .redAthleteId(request.getRedAthleteId())
                .blueAthleteId(request.getBlueAthleteId())
                .totalRounds(request.getTotalRounds())
                .scheduledOrder(request.getScheduledOrder())
                .eventBout(request.isEventBout())
                .build();

        return AdminBoutResponse.from(boutRepository.save(bout));
    }

    public AdminBoutImportResponse importBouts(MultipartFile file) {
        validateImportFile(file);
        return isExcelFile(file) ? importExcelBouts(file) : importCsvBouts(file);
    }

    private AdminBoutImportResponse importCsvBouts(MultipartFile file) {

        List<AdminBoutResponse> importedBouts = new ArrayList<>();
        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {
            validateImportHeaders(parser);
            for (CSVRecord record : parser) {
                importedBouts.add(createBout(toImportRequest(record)));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("bout import file cannot be read");
        }

        if (importedBouts.isEmpty()) {
            throw new IllegalArgumentException("bout import file has no rows");
        }
        return AdminBoutImportResponse.from(importedBouts);
    }

    private AdminBoutImportResponse importExcelBouts(MultipartFile file) {
        List<AdminBoutResponse> importedBouts = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("bout import workbook has no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndexes = readExcelHeaders(sheet.getRow(0), formatter);
            validateImportHeaders(headerIndexes.keySet());
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (String header : IMPORT_HEADERS) {
                    Cell cell = row.getCell(headerIndexes.get(header), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(header, cell == null ? null : formatter.formatCellValue(cell));
                }
                importedBouts.add(createBout(toImportRequest(values, rowIndex + 1L)));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("bout import file cannot be read");
        }

        if (importedBouts.isEmpty()) {
            throw new IllegalArgumentException("bout import file has no rows");
        }
        return AdminBoutImportResponse.from(importedBouts);
    }

    public AdminBoutResponse updateBout(Long boutId, AdminBoutRequest request) {
        validateBoutId(boutId);
        validateRequest(request);

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        bout.updateSchedule(
                request.getTournamentId(),
                request.getRingId(),
                request.getBoutNumber(),
                request.getMatchType(),
                request.getRedAthleteId(),
                request.getBlueAthleteId(),
                request.getTotalRounds(),
                request.getScheduledOrder(),
                request.isEventBout()
        );

        return AdminBoutResponse.from(boutRepository.save(bout));
    }

    public void deleteBout(Long boutId) {
        validateBoutId(boutId);
        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        boutRepository.deleteById(boutId);
    }

    private void validateRequest(AdminBoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("bout request is required");
        }
        validateRequiredFields(request);
        if (request.getRedAthleteId() != null && request.getRedAthleteId().equals(request.getBlueAthleteId())) {
            throw new IllegalArgumentException("redAthleteId and blueAthleteId must be different");
        }
        if (!tournamentRepository.existsById(request.getTournamentId())) {
            throw new TournamentNotFoundException();
        }

        Ring ring = ringRepository.findById(request.getRingId())
                .orElseThrow(RingNotFoundException::new);
        if (!request.getTournamentId().equals(ring.getTournamentId())) {
            throw new IllegalArgumentException("ring does not belong to tournament");
        }

        if (!athleteRepository.existsById(request.getRedAthleteId())
                || !athleteRepository.existsById(request.getBlueAthleteId())) {
            throw new AthleteNotFoundException();
        }
    }

    private void validateRequiredFields(AdminBoutRequest request) {
        validateTournamentId(request.getTournamentId());
        if (request.getRingId() == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (request.getBoutNumber() == null) {
            throw new IllegalArgumentException("boutNumber is required");
        }
        if (request.getBoutNumber() <= 0) {
            throw new IllegalArgumentException("boutNumber must be positive");
        }
        if (request.getRedAthleteId() == null) {
            throw new IllegalArgumentException("redAthleteId is required");
        }
        if (request.getBlueAthleteId() == null) {
            throw new IllegalArgumentException("blueAthleteId is required");
        }
        if (request.getTotalRounds() != null && request.getTotalRounds() <= 0) {
            throw new IllegalArgumentException("totalRounds must be positive");
        }
        if (request.getScheduledOrder() != null && request.getScheduledOrder() <= 0) {
            throw new IllegalArgumentException("scheduledOrder must be positive");
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("bout import file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("bout import file name is required");
        }
        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        if (!normalizedFilename.endsWith(".csv")
                && !normalizedFilename.endsWith(".xls")
                && !normalizedFilename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only CSV or Excel bout import is supported");
        }
    }

    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        return filename.endsWith(".xls") || filename.endsWith(".xlsx");
    }

    private void validateImportHeaders(CSVParser parser) {
        validateImportHeaders(parser.getHeaderMap().keySet());
    }

    private void validateImportHeaders(Set<String> headers) {
        for (String header : IMPORT_HEADERS) {
            if (!headers.contains(header)) {
                throw new IllegalArgumentException(header + " column is required");
            }
        }
    }

    private Map<String, Integer> readExcelHeaders(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerIndexes = new LinkedHashMap<>();
        if (headerRow == null || headerRow.getLastCellNum() < 0) {
            return headerIndexes;
        }
        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            String header = normalize(formatter.formatCellValue(headerRow.getCell(cellIndex)));
            if (header != null) {
                headerIndexes.put(header, cellIndex);
            }
        }
        return headerIndexes;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (normalize(formatter.formatCellValue(cell)) != null) {
                return false;
            }
        }
        return true;
    }

    private AdminBoutRequest toImportRequest(CSVRecord record) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String header : IMPORT_HEADERS) {
            values.put(header, record.get(header));
        }
        return toImportRequest(values, record.getRecordNumber());
    }

    private AdminBoutRequest toImportRequest(Map<String, String> values, long rowNumber) {
        try {
            return new AdminBoutRequest(
                    parseLong(values.get("tournamentId"), "tournamentId", true),
                    parseLong(values.get("ringId"), "ringId", true),
                    parseInteger(values.get("boutNumber"), "boutNumber", true),
                    normalize(values.get("matchType")),
                    parseLong(values.get("redAthleteId"), "redAthleteId", true),
                    parseLong(values.get("blueAthleteId"), "blueAthleteId", true),
                    parseInteger(values.get("totalRounds"), "totalRounds", false),
                    parseInteger(values.get("scheduledOrder"), "scheduledOrder", false),
                    parseBoolean(values.get("eventBout"))
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + exception.getMessage());
        }
    }

    private Long parseLong(String value, String header, boolean required) {
        value = normalize(value);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(header + " is required");
            }
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(header + " must be a number");
        }
    }

    private Integer parseInteger(String value, String header, boolean required) {
        value = normalize(value);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(header + " is required");
            }
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(header + " must be a number");
        }
    }

    private boolean parseBoolean(String value) {
        String normalized = normalize(value);
        return normalized != null && Boolean.parseBoolean(normalized);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
