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
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminBoutServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private AdminBoutService adminBoutService;

    @Test
    void getBoutsReturnsTournamentBouts() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(createBout(20L), createBout(21L)));

        List<AdminBoutResponse> responses = adminBoutService.getBouts(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getBoutId()).isEqualTo(20L);
        assertThat(responses.get(1).getBoutId()).isEqualTo(21L);
    }

    @Test
    void getBoutsRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.getBouts(99L))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void getBoutReturnsBout() {
        given(boutRepository.findById(20L)).willReturn(Optional.of(createBout(20L)));

        AdminBoutResponse response = adminBoutService.getBout(20L);

        assertThat(response.getBoutId()).isEqualTo(20L);
        assertThat(response.getBoutNumber()).isEqualTo(1);
    }

    @Test
    void getBoutRejectsMissingBout() {
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBoutService.getBout(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void createBoutSavesBout() {
        givenValidReferences();
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> {
            Bout bout = invocation.getArgument(0);
            ReflectionTestUtils.setField(bout, "id", 20L);
            return bout;
        });

        AdminBoutResponse response = adminBoutService.createBout(request());

        assertThat(response.getBoutId()).isEqualTo(20L);
        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getRingId()).isEqualTo(1L);
        assertThat(response.getRedAthleteId()).isEqualTo(10L);
        assertThat(response.getBlueAthleteId()).isEqualTo(11L);
        assertThat(response.getScheduledOrder()).isEqualTo(1);
    }

    @Test
    void createBoutRejectsMissingTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void createBoutRejectsMissingRing() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void createBoutRejectsRingFromDifferentTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 2L)));

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ring does not belong to tournament");
    }

    @Test
    void createBoutRejectsMissingAthlete() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 1L)));
        given(athleteRepository.existsById(10L)).willReturn(true);
        given(athleteRepository.existsById(11L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(AthleteNotFoundException.class)
                .hasMessage("Athlete not found");
    }

    @Test
    void createBoutRejectsSameAthlete() {
        AdminBoutRequest request = new AdminBoutRequest(1L, 1L, 1, "75", 10L, 10L, 3, 1, false);

        assertThatThrownBy(() -> adminBoutService.createBout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redAthleteId and blueAthleteId must be different");
    }

    @Test
    void importBoutsCreatesBoutsFromCsv() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 1L)));
        given(athleteRepository.existsById(10L)).willReturn(true);
        given(athleteRepository.existsById(11L)).willReturn(true);
        given(athleteRepository.existsById(12L)).willReturn(true);
        given(athleteRepository.existsById(13L)).willReturn(true);
        AtomicLong id = new AtomicLong(20L);
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> {
            Bout bout = invocation.getArgument(0);
            ReflectionTestUtils.setField(bout, "id", id.getAndIncrement());
            return bout;
        });

        AdminBoutImportResponse response = adminBoutService.importBouts(csvFile(
                "tournamentId,ringId,boutNumber,matchType,redAthleteId,blueAthleteId,totalRounds,scheduledOrder,eventBout\n"
                        + "1,1,1,75 - middle school,10,11,3,1,false\n"
                        + "1,1,2,80 - high school,12,13,3,2,true\n"
        ));

        assertThat(response.getImportedCount()).isEqualTo(2);
        assertThat(response.getBoutIds()).containsExactly(20L, 21L);
    }

    @Test
    void importBoutsRejectsMissingRequiredCsvValue() {
        assertThatThrownBy(() -> adminBoutService.importBouts(csvFile(
                "tournamentId,ringId,boutNumber,matchType,redAthleteId,blueAthleteId,totalRounds,scheduledOrder,eventBout\n"
                        + "1,1,,75 - middle school,10,11,3,1,false\n"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("row 1: boutNumber is required");
    }

    @Test
    void importBoutsCreatesBoutsFromExcel() throws IOException {
        givenValidReferences();
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> {
            Bout bout = invocation.getArgument(0);
            ReflectionTestUtils.setField(bout, "id", 30L);
            return bout;
        });

        AdminBoutImportResponse response = adminBoutService.importBouts(excelFile());

        assertThat(response.getImportedCount()).isEqualTo(1);
        assertThat(response.getBoutIds()).containsExactly(30L);
    }

    @Test
    void importBoutsRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "bouts.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> adminBoutService.importBouts(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only CSV or Excel bout import is supported");
    }

    @Test
    void updateBoutChangesBout() {
        Bout bout = createBout(20L);
        AdminBoutRequest request = new AdminBoutRequest(1L, 1L, 2, "80 - high school", 12L, 13L, 4, 2, true);
        givenValidReferences(12L, 13L);
        given(boutRepository.findById(20L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminBoutResponse response = adminBoutService.updateBout(20L, request);

        assertThat(response.getBoutId()).isEqualTo(20L);
        assertThat(response.getBoutNumber()).isEqualTo(2);
        assertThat(response.getMatchType()).isEqualTo("80 - high school");
        assertThat(response.getRedAthleteId()).isEqualTo(12L);
        assertThat(response.getBlueAthleteId()).isEqualTo(13L);
        assertThat(response.isEventBout()).isTrue();
    }

    @Test
    void updateBoutRejectsMissingBout() {
        givenValidReferences();
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBoutService.updateBout(99L, request()))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void updateBoutRejectsNullBoutId() {
        assertThatThrownBy(() -> adminBoutService.updateBout(null, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boutId is required");
    }

    @Test
    void deleteBoutDeletesExistingBout() {
        given(boutRepository.existsById(20L)).willReturn(true);

        adminBoutService.deleteBout(20L);

        then(boutRepository).should().deleteById(20L);
    }

    @Test
    void deleteBoutRejectsMissingBout() {
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.deleteBout(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    private void givenValidReferences() {
        givenValidReferences(10L, 11L);
    }

    private void givenValidReferences(Long redAthleteId, Long blueAthleteId) {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 1L)));
        given(athleteRepository.existsById(redAthleteId)).willReturn(true);
        given(athleteRepository.existsById(blueAthleteId)).willReturn(true);
    }

    private AdminBoutRequest request() {
        return new AdminBoutRequest(1L, 1L, 1, "75 - middle school", 10L, 11L, 3, 1, false);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "bouts.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile excelFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("bouts");
            String[] headers = {
                    "tournamentId", "ringId", "boutNumber", "matchType", "redAthleteId",
                    "blueAthleteId", "totalRounds", "scheduledOrder", "eventBout"
            };
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue(1);
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("75 - middle school");
            row.createCell(4).setCellValue(10);
            row.createCell(5).setCellValue(11);
            row.createCell(6).setCellValue(3);
            row.createCell(7).setCellValue(1);
            row.createCell(8).setCellValue(false);
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "bouts.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .totalRounds(3)
                .scheduledOrder(1)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Ring createRing(Long id, Long tournamentId) {
        Ring ring = Ring.builder()
                .tournamentId(tournamentId)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }
}
