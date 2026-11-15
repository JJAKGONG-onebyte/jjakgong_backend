package com.onebyte.backend.domain.material.service;

import com.onebyte.backend.domain.material.entity.MaterialFileType;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 업로드 파일 검사: 비어 있는지, 용량, 확장자, 실제 내용(PDF 시그니처 / TXT 바이너리 여부) */
@Component
public class MaterialFileValidator {

    /** 파일당 최대 용량 (10MB). multipart 설정(max-file-size)과 같은 값 */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final int HEAD_BYTES = 8192;
    private static final int PDF_SIGNATURE_RANGE = 1024;

    public MaterialFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        MaterialFileType type = MaterialFileType.fromFilename(file.getOriginalFilename())
                .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE));
        if (!contentMatches(file, type)) {
            throw new CustomException(ErrorCode.INVALID_FILE_CONTENT,
                    "올바른 " + type.name() + " 파일이 아니거나 손상된 파일입니다.");
        }
        return type;
    }

    private boolean contentMatches(MultipartFile file, MaterialFileType type) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(HEAD_BYTES);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
        return switch (type) {
            case PDF -> new String(head, 0, Math.min(PDF_SIGNATURE_RANGE, head.length), StandardCharsets.ISO_8859_1)
                    .contains("%PDF-");
            case TXT -> {
                for (byte b : head) {
                    if (b == 0) {
                        yield false;
                    }
                }
                yield true;
            }
        };
    }
}
