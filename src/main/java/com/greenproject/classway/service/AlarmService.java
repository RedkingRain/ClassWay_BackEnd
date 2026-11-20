package com.greenproject.classway.service;

import com.greenproject.classway.dto.AlarmListResponse;
import com.greenproject.classway.entity.Alarm;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.AlarmType;
import com.greenproject.classway.repository.AlarmRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 헤더 알림 목록 , 읽음 처리 (MAIN-001)
// 모든 기능은 로그인한 본인 알림만 다룸 (회원은 토큰으로 확인)
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;

    // 알림 목록 (최신 20개 + 안 읽은 개수)
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

    // 알림 1개 읽음
    @Transactional
    public void readAlarm(Integer alarmId) {
        User user = getLoginUser();

        // 번호 + 주인이 맞는 알림만 → 남의 알림 번호면 「없는 알림」
        Alarm alarm = alarmRepository.findByAlarmIdAndUser_Id(alarmId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 알림입니다."));

        // 값만 바꾸면 트랜잭션이 끝날 때 JPA가 알아서 UPDATE (save 안 해도 됨)
        alarm.setIsRead(true);
    }

    // 모두 읽음
    @Transactional
    public void readAllAlarms() {
        User user = getLoginUser();
        alarmRepository.markAllAsRead(user.getId());
    }

    // 알림 1개 보내기 (다른 서비스에서 호출, 예: 관리자 강사 승인·반려)
    // - 부르는 쪽 트랜잭션에 함께 묶임 → 부르는 쪽이 실패하면 알림도 같이 취소됨
    // - 제목은 DB 칸(alarms.title, 100자)보다 길면 잘라서 저장
    @Transactional
    public void send(User receiver, AlarmType type, String title, String linkUrl) {
        Alarm alarm = new Alarm();
        alarm.setUser(receiver);
        alarm.setType(type);
        alarm.setTitle(title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title);
        alarm.setLinkUrl(linkUrl);
        alarm.setIsRead(false);
        alarmRepository.save(alarm);
    }

    private static final int TITLE_MAX_LENGTH = 100; // alarms.title VARCHAR(100)

    // 지금 로그인한 회원 (장바구니 서비스와 같은 방식)
    private User getLoginUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다."));
    }
}