package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FindEmailResponse {
    private List<EmailInfo> emails;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class EmailInfo {
        private String email;
        private String provider;
    }
}
