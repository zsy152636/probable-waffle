package com.example.quiz.sys.controller;

import com.example.quiz.sys.common.Result;
import com.example.quiz.sys.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        Map<String, Object> data = authService.login(body.get("username"), body.get("password"));
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String auth) {
        String token = auth.replace("Bearer ", "");
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return Result.ok(authService.getUserInfo(userId));
    }
}
