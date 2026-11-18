package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartDeleteRequest {

    @NotEmpty(message = "삭제할 장바구니 항목을 선택해 주세요.")
    private List<Integer> cartItemIds;
}
