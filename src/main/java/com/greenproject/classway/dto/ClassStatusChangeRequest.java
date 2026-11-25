package com.greenproject.classway.dto;

import com.greenproject.classway.global.ClassStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassStatusChangeRequest {
    @NotNull(message = "변경할 상태값은 필수입니다.")
    private ClassStatus status; // 변경할 상태값: PUBLISHED 또는 HIDDEN
}
