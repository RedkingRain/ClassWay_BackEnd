package com.greenproject.classway.dto;

import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorClassDetailResponse {
    private Integer classId;        // 클래스 번호
    private ClassType classType;    // 진행 방식 ('ONLINE', 'OFFLINE')
    private ClassStatus status;     // 클래스 상태
    private String title;           // 클래스 명
    private Integer categoryId;     // 카테고리 번호
    private String summary;         // 클래스 한 줄 소개
    private Integer price;          // 가격(정가)
    private Integer discountRate;   // 할인율 (%)
    private Integer salePrice;      // 할인가 (할인 없으면 null)
    private String thumbnailUrl;    // 배너 이미지 파일 경로/URL
    private String description;     // 클래스 상세 소개
    private String address;         // 수업 장소
}
