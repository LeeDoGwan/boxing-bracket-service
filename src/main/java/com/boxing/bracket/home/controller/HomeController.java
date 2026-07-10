package com.boxing.bracket.home.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.home.dto.HomeResponse;
import com.boxing.bracket.home.service.HomeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(@Lazy HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ApiResponse<HomeResponse> getHome(@RequestParam Long tournamentId) {
        return ApiResponse.success(homeService.getHome(tournamentId), "OK");
    }
}
