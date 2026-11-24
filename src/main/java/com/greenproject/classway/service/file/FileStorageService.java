package com.greenproject.classway.service.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileStorageService {
    // 파일을 저장하고, DB에 저장할 파일 경로(또는 URL) 문자열 리스트를 반환
    List<String> storeFiles(List<MultipartFile> files);

    // storeFiles가 돌려준 경로(또는 URL)로 저장된 파일을 꺼냄
    Resource loadFile(String path);
}