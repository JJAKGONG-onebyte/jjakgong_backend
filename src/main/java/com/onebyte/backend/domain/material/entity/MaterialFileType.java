package com.onebyte.backend.domain.material.entity;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/** 업로드 가능한 자료 형식. study_material.file_type에는 name()을 저장한다. */
public enum MaterialFileType {

    PDF("pdf", MediaType.APPLICATION_PDF),
    TXT("txt", new MediaType("text", "plain", StandardCharsets.UTF_8));

    private final String extension;
    private final MediaType mediaType;

    MaterialFileType(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    /** 파일명의 확장자로 형식을 찾는다. (대소문자 무시) */
    public static Optional<MaterialFileType> fromFilename(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return Optional.empty();
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (MaterialFileType type : values()) {
            if (type.extension.equals(ext)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
