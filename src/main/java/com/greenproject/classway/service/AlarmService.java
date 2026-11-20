package com.greenproject.classway.service;

import com.greenproject.classway.dto.AlarmListResponse;
import com.greenproject.classway.entity.Alarm;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.repository.AlarmRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 헤더 알림 목록, 읽음 처리 (MAIN-001), 토큰의 회원 본인 알림만 다룸
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;

    // 최신 알림 20개와 안 읽은 개수 (MAIN-001)
    @Transactional(readOnly = true)
    public AlarmListResponse getAlarms() {
        User user = getLoginUser();

        List<AlarmListResponse.AlarmItem> items = new ArrayList<>();
        for (Alarm alarm : alarmRepository.findTop20ByUser_IdOrderByAlarmIdDesc(user.getId())) {
            items.add(AlarmListResponse.AlarmItem.builder()
                    .alarmId(alarm.getAlarmId())
                    .type(alarm.getType().name())
                    .title(alarm.getTitle())
                    .linkUrl(alarm.getLinkUrl())
                    .isRead(alarm.getIsRead())
                    .createdAt(alarm.getCreatedAt())
                    .build());
        }

        return AlarmListResponse.builder()
                .alarms(items)
                .unreadCount(alarmRepository.countByUser_IdAndIsReadFalse(user.getId()))
                .build();
    }

    @Transactional
    public void readAlarm(Integer alarmId) {
        User user = getLoginUser();

        // 남의 알림 번호를 넣어도 찾지 못하게 주인 조건을 같이 걸고, 같은 404로 답함
        Alarm alarm = alarmRepository.findByAlarmIdAndUser_Id(alarmId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 알림입니다."));

        // 트랜잭션이 끝날 때 JPA가 바뀐 값을 UPDATE 해서 save를 부르지 않음
        alarm.setIsRead(true);
    }

    @Transactional
    public void readAllAlarms() {
        User user = getLoginUser();
        alarmRepository.markAllAsRead(user.getId());
    }

    private User getLoginUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다."));
    }
}