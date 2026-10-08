package com.iot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String email;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private Long userId;

        public AuthResponse(String token, String username, Long userId) {
            this.token = token;
            this.username = username;
            this.userId = userId;
        }
    }
}
