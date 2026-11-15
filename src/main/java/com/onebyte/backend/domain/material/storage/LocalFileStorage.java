package com.onebyte.backend.domain.material.storage;

import com.onebyte.backend.domain.material.entity.MaterialFileType;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 서버 디스크에 저장 (기본값). 컨테이너 배포 시에는 app.storage.local-dir을 볼륨으로 마운트해야 파일이 유지된다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(@Value("${app.storage.local-dir:./uploads}") String localDir) {
        this.baseDir = Path.of(localDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long userId, MaterialFileType type, MultipartFile file) {
        String key = "materials/" + userId + "/" + UUID.randomUUID() + "." + type.extension();
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            log.error("자료 파일 저장 실패 key={}", key, e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
        return key;
    }

    @Override
    public InputStream open(String key) {
        Path path = resolve(key);
        if (!Files.exists(path)) {
            throw new CustomException(ErrorCode.MATERIAL_FILE_MISSING);
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            log.error("자료 파일 읽기 실패 key={}", key, e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException | CustomException e) {
            log.warn("자료 파일 삭제 실패 key={}", key, e);
        }
    }

    /** key가 저장 폴더 밖을 가리키지 않도록 확인 */
    private Path resolve(String key) {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
        return path;
    }
}
