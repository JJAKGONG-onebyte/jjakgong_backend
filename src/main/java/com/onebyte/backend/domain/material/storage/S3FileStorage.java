package com.onebyte.backend.domain.material.storage;

import com.onebyte.backend.domain.material.entity.MaterialFileType;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * AWS S3에 저장 (배포용). app.storage.type=s3 일 때만 사용된다.
 * 자격 증명은 코드/설정에 넣지 않고 기본 자격 증명 체인(EC2 IAM 역할, 환경변수 등)을 사용한다.
 * 필요한 권한: 버킷의 materials/* 에 대한 s3:PutObject, s3:GetObject, s3:DeleteObject
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final S3Client s3;
    private final String bucket;

    public S3FileStorage(@Value("${app.storage.s3.bucket:}") String bucket,
                         @Value("${app.storage.s3.region:ap-northeast-2}") String region) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("STORAGE_TYPE=s3 이면 STORAGE_S3_BUCKET 설정이 필요합니다.");
        }
        this.bucket = bucket;
        this.s3 = S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public String store(Long userId, MaterialFileType type, MultipartFile file) {
        String key = "materials/" + userId + "/" + UUID.randomUUID() + "." + type.extension();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(type.mediaType().toString())
                .build();
        try (InputStream in = file.getInputStream()) {
            s3.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("S3 업로드 실패 key={}", key, e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
        return key;
    }

    @Override
    public InputStream open(String key) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new CustomException(ErrorCode.MATERIAL_FILE_MISSING);
        } catch (SdkException e) {
            log.error("S3 읽기 실패 key={}", key, e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("S3 삭제 실패 key={}", key, e);
        }
    }

    @PreDestroy
    void close() {
        s3.close();
    }
}
