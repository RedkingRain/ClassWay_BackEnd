package com.greenproject.classway.service;

import com.greenproject.classway.dto.WishToggleResponse;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.entity.Wish;
import com.greenproject.classway.entity.WishId;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.repository.ClassEntityRepository;
import com.greenproject.classway.repository.UserRepository;
import com.greenproject.classway.repository.WishRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 찜하기 (CLASS-003)
@Service
@AllArgsConstructor
public class WishService {
    private final WishRepository wishRepository;
    private final UserRepository userRepository;
    private final ClassEntityRepository classEntityRepository;

    // 찜 토글: 안 했으면 추가, 했으면 해제
    @Transactional
    public WishToggleResponse toggleWish(Integer classId, String loginEmail) {
        User user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));

        // 비공개 클래스는 있는지조차 알려주지 않으려고 상세 조회와 같은 404
        ClassEntity clazz = classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));

        WishId wishId = new WishId(user.getId(), classId);
        boolean wished;
        if (wishRepository.existsById(wishId)) {
            wishRepository.deleteById(wishId);
            classEntityRepository.decreaseWishCount(classId);
            wished = false;
        } else {
            // createdAt은 DB 기본값(CURRENT_TIMESTAMP)이 채워서 null
            wishRepository.save(new Wish(wishId, user, clazz, null));
            classEntityRepository.increaseWishCount(classId);
            wished = true;
        }

        // @Modifying은 이미 꺼내둔 clazz 숫자를 바꾸지 않아서 DB에서 새로 읽음
        Integer wishCount = classEntityRepository.findWishCount(classId);

        return WishToggleResponse.builder()
                .wished(wished)
                .wishCount(wishCount)
                .build();
    }
}