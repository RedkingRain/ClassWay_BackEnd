package com.greenproject.classway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DuplicateCheckResponse {
    @JsonProperty("isDuplicate")
    private boolean isDuplicate;
}
