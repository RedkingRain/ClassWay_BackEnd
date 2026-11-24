package com.greenproject.classway.service;

import com.greenproject.classway.dto.InquiryCreateRequest;
import com.greenproject.classway.dto.InquiryFileDownload;
import com.greenproject.classway.entity.Inquiry;
import com.greenproject.classway.entity.InquiryFile;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.FileStatus;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.InquiryFileRepository;
import com.greenproject.classway.repository.InquiryRepository;
import com.greenproject.classway.repository.UserRepository;
import com.greenproject.classway.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 100; // file_url(255자)에 UUID와 폴더 경로가 같이 들어감
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "pdf");

    private final InquiryRepository inquiryRepository;
    private final InquiryFileRepository inquiryFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void createInquiry(InquiryCreateRequest request) {
        List<MultipartFile> files = getValidFiles(request.getFiles());
        User user = getLoginUser();

        Inquiry inquiry = new Inquiry();
        inquiry.setUser(user);
        inquiry.setCategory(request.getCategory());
        inquiry.setTitle(request.getTitle().trim());
        inquiry.setContent(request.getContent().trim());
        String orderNo = request.getOrderNo();
        inquiry.setOrderNo(orderNo == null || orderNo.isBlank() ? null : orderNo.trim());
        inquiryRepository.save(inquiry);

        // DB 저장이 실패해도 이미 저장된 파일은 폴더에 남음, 파일이 많아지면 정리 작업 추가
        List<String> paths = fileStorageService.storeFiles(files);
        for (int i = 0; i < files.size(); i++) {
            InquiryFile inquiryFile = new InquiryFile();
            inquiryFile.setInquiry(inquiry);
            inquiryFile.setFileName(files.get(i).getOriginalFilename());
            inquiryFile.setFileUrl(paths.get(i));
            inquiryFile.setStatus(FileStatus.ACTIVE);
            inquiryFileRepository.save(inquiryFile);
        }
    }

    @Transactional(readOnly = true)
    public InquiryFileDownload getInquiryFile(Integer inquiryId, Integer fileId) {
        User loginUser = getLoginUser();
        InquiryFile file = inquiryFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 파일입니다."));
        Inquiry inquiry = file.getInquiry();

        boolean isOwner = inquiry.getUser().getId().equals(loginUser.getId());
        boolean isAdmin = loginUser.getRole() == UserRole.ADMIN;
        // 남의 파일이 있다는 사실도 알리지 않으려고 모든 경우를 같은 404로 응답
        if (!inquiry.getInquiryId().equals(inquiryId) || inquiry.getIsDeleted() || !(isOwner || isAdmin)) {
            throw new ResourceNotFoundException("존재하지 않는 파일입니다.");
        }
        Resource resource = fileStorageService.loadFile(file.getFileUrl());
        // 디스크가 아니라 이름으로 판별해서 S3로 바뀌어도 그대로 쓸 수 있게 함
        String contentType = MediaTypeFactory.getMediaType(file.getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                .toString();
        return new InquiryFileDownload(resource, contentType, file.getFileName());
    }

    private List<MultipartFile> getValidFiles(List<MultipartFile> files) {
        List<MultipartFile> result = new ArrayList<>();
        if (files == null) {
            return result;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String name = file.getOriginalFilename();
            // 저장 코드가 원래 이름을 경로에 붙이므로 폴더를 벗어나는 이름(../)을 막음
            if (name == null || name.length() > MAX_FILE_NAME_LENGTH
                    || name.contains("/") || name.contains("\\") || name.contains("..")) {
                throw new InvalidStateException("파일 이름이 올바르지 않습니다.");
            }

            String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new InvalidStateException("JPG, PNG, PDF 파일만 첨부할 수 있습니다.");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new InvalidStateException("파일은 10MB까지 첨부할 수 있습니다.");
            }
            result.add(file);
        }

        if (result.size() > MAX_FILE_COUNT) {
            throw new InvalidStateException("파일은 최대 3개까지 첨부할 수 있습니다.");
        }
        return result;
    }

    private User getLoginUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다."));
    }
}