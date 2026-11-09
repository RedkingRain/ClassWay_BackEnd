package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;

@Tag(name="ClassWay 회원 계정 API", description = "회원 계정 관리 API 모듈입니다.")
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> createBoard() {
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
