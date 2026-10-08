package com.iot.controller;

import com.iot.dto.AuthDTO;
import com.iot.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        try {
            AuthDTO.AuthResponse response = authService.register(request);
            return ResponseEntity.ok(Map.of("code", 200, "data", response, "message", "注册成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        try {
            AuthDTO.AuthResponse response = authService.login(request);
            return ResponseEntity.ok(Map.of("code", 200, "data", response, "message", "登录成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }
}
