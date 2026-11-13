package com.greenproject.classway.dto;

import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.global.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignupTutorRequest {
    //-------회원 가입 공통---------
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;               // 이메일 (로그인 아이디)

    @NotBlank
    @Size(max = 20)
    private String password;            // 비밀번호 해시 (소셜 가입자는 비어 있음)

    @NotBlank
    @Size(max = 50)
    private String name;                // 이름

    @NotBlank
    @Size(max = 13)
    private String phone;               // 휴대폰 번호

    @NotBlank
    private String nickname;	// 사용자 활동명

    private List<MultipartFile> profile_image;	// 프로필 사진 주소

    @NotNull
    private UserRole role;	            //역할: STUDENT, TUTOR, ADMIN (강사는 승인 후 TUTOR)

    @NotNull
    private Boolean class_content_alarm;    // 클래스 신규 강의 컨텐츠 등록 알람 수신

    @NotNull
    private Boolean qna_alarm;              // 고객센터 1:1문의 및 강사 문의 답변 알림 수신

    @NotNull
    private Boolean offline_class_alarm;    // 오프라인 수업 일정 알림 수신

    @NotNull
    private Boolean marketing_agree;	// 마케팅 수신 동의 (선택 약관)

    //-------강사 전용 항목---------
    private String intro;	// 강사 소개

    private String career;	// 경력 사항

    private List<MultipartFile> document_file;	// 증빙 파일 주소 (포트폴리오·경력증명서 1개)
}
