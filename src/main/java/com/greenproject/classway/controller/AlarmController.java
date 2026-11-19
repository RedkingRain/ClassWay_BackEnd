package com.greenproject.classway.controller;

import com.greenproject.classway.dto.AlarmListResponse;
import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Alarm API", description = "헤더 알림 목록 / 읽음 처리 API 모듈입니다.")
@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "알림 목록 조회 API", description = "로그인한 회원의 최신 알림 20개와 안 읽은 알림 개수")
    @GetMapping
    public ResponseEntity<ApiResponse<AlarmListResponse>> getAlarms() {
        AlarmListResponse result = alarmService.getAlarms();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // read-all을 {alarmId}보다 먼저 둬서 주소가 헷갈리지 않게
    @Operation(summary = "알림 모두 읽음 API", description = "로그인한 회원의 안 읽은 알림을 모두 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAllAlarms() {
        alarmService.readAllAlarms();
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "알림 읽음 API", description = "본인 알림 1개를 읽음 처리 (다른 회원 알림은 404)")
    @PatchMapping("/{alarmId}/read")
    public ResponseEntity<ApiResponse<Void>> readAlarm(@PathVariable Integer alarmId) {
        alarmService.readAlarm(alarmId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}