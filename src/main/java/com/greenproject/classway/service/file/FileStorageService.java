package com.greenproject.classway.service.file;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileStorageService {
    // 파일을 저장하고, DB에 저장할 파일 경로(또는 URL) 문자열 리스트를 반환
    List<String> storeFiles(List<MultipartFile> files);
}
