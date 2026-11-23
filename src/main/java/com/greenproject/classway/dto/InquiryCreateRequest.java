package com.greenproject.classway.dto;

import com.greenproject.classway.global.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InquiryCreateRequest {
    @NotNull
    private InquiryCategory category;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String content;

    @Size(max = 64)
    private String orderNo;

    private List<MultipartFile> files;
}