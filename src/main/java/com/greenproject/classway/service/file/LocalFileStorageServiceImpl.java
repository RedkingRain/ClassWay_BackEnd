package com.greenproject.classway.service.file;

import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Primary // 현재는 이 로컬 구현체를 우선적으로 주입받아 사용하겠다는 의미.
public class LocalFileStorageServiceImpl implements FileStorageService {
    private final String uploadDir;

    // @Value 어노테이션을 통해 application.properties의 값을 주입.
    public LocalFileStorageServiceImpl(@Value("${spring.file.storage}") String uploadDir) {
        // 경로 마지막에 슬래시(/)가 없을 경우를 대비해 안전하게 처리
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }

    @Override
    public List<String> storeFiles(List<MultipartFile> files) {
        List<String> filePaths = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return filePaths;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = "";
            try {
                originalFilename = file.getOriginalFilename();
                String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

                Path filePath = Paths.get(uploadDir + savedFilename);
                Files.createDirectories(filePath.getParent());

                // 파일 로컬 디스크에 저장
                file.transferTo(filePath.toFile());

                // DB에 저장할 경로 값 (예: /uploadfiles/uuid_파일명 또는 절대 경로)
                // 웹에서 바로 접근하게 하려면 static resource 경로와 매핑할 수도 있습니다.
                filePaths.add(filePath.toString());

            } catch (IOException e) {
                throw new RuntimeException("로컬 파일 저장 실패: " + originalFilename, e);
            }
        }
        return filePaths;
    }

    @Override
    public Resource loadFile(String path) {
        Path realRoot;
        Path realPath;
        try {
            realRoot = Paths.get(uploadDir).toAbsolutePath().normalize().toRealPath();
            realPath = Paths.get(path).toRealPath();
        } catch (IOException e) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }
        // DB 경로가 조작돼도 업로드 폴더 밖의 파일은 읽지 못하게 막음
        if (!realPath.startsWith(realRoot)) {
            throw new AuthorizationFailureException("접근할 수 없는 파일입니다.");
        }
        if (!Files.isRegularFile(realPath)) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(realPath);
    }
}