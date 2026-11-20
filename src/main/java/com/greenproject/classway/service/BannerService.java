package com.greenproject.classway.service;

import com.greenproject.classway.dto.BannerResponse;
import com.greenproject.classway.entity.Banner;
import com.greenproject.classway.repository.BannerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;

    // 지금 노출 중인 배너 (켜져 있고 기간 안), 순서 값 작은 순
    @Transactional(readOnly = true)
    public List<BannerResponse> getBanners() {
        List<Banner> banners = bannerRepository.findShowingBanners(LocalDateTime.now());

        List<BannerResponse> result = new ArrayList<>();
        for (Banner banner : banners) {
            if (!isSafeLink(banner.getLinkUrl())) {
                continue;
            }
            result.add(BannerResponse.builder()
                    .bannerId(banner.getBannerId())
                    .title(banner.getTitle())
                    .imageUrl(banner.getImageUrl())
                    .linkUrl(banner.getLinkUrl())
                    .build());
        }
        return result;
    }
    private boolean isSafeLink(String linkUrl) {
        if (linkUrl == null) {
            return false;
        }
        return linkUrl.startsWith("/") || linkUrl.startsWith("http://") || linkUrl.startsWith("https://");
    }
}
