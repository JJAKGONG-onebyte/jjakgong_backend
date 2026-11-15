package com.onebyte.backend.domain.material.storage;

import com.onebyte.backend.domain.material.entity.MaterialFileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 자료 파일 저장소. 로컬 디스크와 S3를 설정(app.storage.type)으로 바꿔 쓴다.
 * DB(study_material.file_path)에는 저장소 안에서의 경로(key)만 저장한다.
 */
public interface FileStorage {

    /** 파일을 저장하고 key를 반환한다. (예: materials/12/3f2a....pdf) */
    String store(Long userId, MaterialFileType type, MultipartFile file);

    /** key의 파일 내용을 연다. 호출한 쪽에서 닫아야 한다. */
    InputStream open(String key);

    /** 파일을 삭제한다. 이미 없어도 오류로 보지 않는다. */
    void delete(String key);
}
