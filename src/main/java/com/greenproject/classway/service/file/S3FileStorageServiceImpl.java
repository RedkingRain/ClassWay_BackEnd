package com.greenproject.classway.service.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

// @Service // 나중에 S3를 쓸 때 주석을 풀고, Local 구현체에서 @Primary를 떼어내거나 교체하면 됩니다.
public class S3FileStorageServiceImpl implements FileStorageService {

    // private final S3Client s3Client; (AWS S3 클라이언트 주입)

    @Override
    public List<String> storeFiles(List<MultipartFile> files) {
        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // TODO: AWS S3 파일 업로드 로직 구현
            // String s3Url = s3Uploader.upload(file);
            // fileUrls.add(s3Url); // DB에는 S3 URL이 저장됨
        }
        return fileUrls;
    }
}