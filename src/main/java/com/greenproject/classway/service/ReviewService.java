package com.greenproject.classway.service;

import com.greenproject.classway.dto.MainReviewResponse;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.Reservation;
import com.greenproject.classway.entity.Review;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ReviewService {
    private static final byte MAIN_REVIEW_RATING = 5;
    private static final int MAIN_REVIEW_SIZE = 3;

    private final ReviewRepository reviewRepository;

    // 메인 후기: 공개 클래스의 5점 후기 중 최신 3개 (숨김, 삭제 제외)
    @Transactional(readOnly = true)
    public List<MainReviewResponse> getMainReviews() {
        List<Review> reviews = reviewRepository.findMainReviews(
                MAIN_REVIEW_RATING, ClassStatus.PUBLISHED, PageRequest.of(0, MAIN_REVIEW_SIZE));

        List<MainReviewResponse> result = new ArrayList<>();
        for (Review review : reviews) {
            Reservation reservation = review.getReservation();
            ClassEntity clazz = reservation.getClazz();

            result.add(MainReviewResponse.builder()
                    .reviewId(review.getReservationId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .writerNickname(reservation.getUser().getNickname())
                    .classId(clazz.getClassId())
                    .classTitle(clazz.getTitle())
                    .classType(clazz.getClassType().name())
                    .thumbnailUrl(clazz.getThumbnailUrl())
                    .build());
        }
        return result;
    }
}