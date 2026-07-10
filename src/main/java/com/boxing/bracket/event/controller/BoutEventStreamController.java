package com.boxing.bracket.event.controller;

import com.boxing.bracket.event.service.BoutEventStreamService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class BoutEventStreamController {

    private final BoutEventStreamService boutEventStreamService;

    public BoutEventStreamController(@Lazy BoutEventStreamService boutEventStreamService) {
        this.boutEventStreamService = boutEventStreamService;
    }

    @GetMapping("/stream")
    public SseEmitter stream(
            @RequestParam Long tournamentId,
            @RequestParam(required = false) Long ringId
    ) {
        return boutEventStreamService.subscribe(tournamentId, ringId);
    }
}
