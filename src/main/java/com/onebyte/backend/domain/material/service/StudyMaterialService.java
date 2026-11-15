package com.onebyte.backend.domain.material.service;

import com.onebyte.backend.domain.material.dto.MaterialListResponse;
import com.onebyte.backend.domain.material.dto.MaterialRenameResponse;
import com.onebyte.backend.domain.material.dto.MaterialUploadResponse;
import com.onebyte.backend.domain.material.dto.SelectedMaterialResponse;
import com.onebyte.backend.domain.material.entity.MaterialFileType;
import com.onebyte.backend.domain.material.entity.StudyMaterial;
import com.onebyte.backend.domain.material.repository.StudyMaterialRepository;
import com.onebyte.backend.domain.material.storage.FileStorage;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.exception.CustomException;
import com.onebyte.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int ORIGIN_NAME_MAX_LENGTH = 255;

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final MaterialFileValidator fileValidator;

    @Transactional(readOnly = true)
    public MaterialListResponse getMaterials(Long userId) {
        return MaterialListResponse.from(studyMaterialRepository.findByUser_IdOrderByUploadedAtDescIdDesc(userId));
    }

    /** 업로드한다고 자동으로 선택되지는 않는다. */
    @Transactional
    public MaterialUploadResponse upload(Long userId, MultipartFile file) {
        MaterialFileType type = fileValidator.validate(file);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String originName = cleanFileName(file.getOriginalFilename(), type);
        String key = fileStorage.store(userId, type, file);
        try {
            StudyMaterial saved = studyMaterialRepository.save(StudyMaterial.builder()
                    .user(user)
                    .name(toDisplayName(originName))
                    .originFileName(truncate(originName, ORIGIN_NAME_MAX_LENGTH))
                    .fileType(type.name())
                    .fileSize(file.getSize())
                    .filePath(key)
                    .build());
            return MaterialUploadResponse.from(saved);
        } catch (RuntimeException e) {
            // DB 저장이 실패하면 방금 올린 파일이 고아로 남지 않게 지운다
            fileStorage.delete(key);
            throw e;
        }
    }

    /** 표시 이름만 바꾼다. 원본 파일과 과거 학습 기록의 자료명(스냅샷)은 바뀌지 않는다. */
    @Transactional
    public MaterialRenameResponse rename(Long userId, Long materialId, String name) {
        StudyMaterial material = getOwned(userId, materialId);
        material.rename(name.trim());
        return new MaterialRenameResponse(material.getId(), material.getName());
    }

    /**
     * 자료를 삭제한다. 학습 기록·문제의 material_id는 DB(ON DELETE SET NULL)에서 null이 되어 기록은 유지된다.
     * 저장소의 파일은 DB 삭제가 커밋된 뒤에 지운다.
     */
    @Transactional
    public void delete(Long userId, Long materialId) {
        StudyMaterial material = getOwned(userId, materialId);
        String key = material.getFilePath();

        studyMaterialRepository.delete(material);
        studyMaterialRepository.flush();

        deleteFileAfterCommit(key);
    }

    /** 학습할 자료를 하나만 선택한다. materialId가 null이면 선택을 해제한다. */
    @Transactional
    public SelectedMaterialResponse select(Long userId, Long materialId) {
        if (materialId == null) {
            studyMaterialRepository.clearSelected(userId);
            return new SelectedMaterialResponse(null);
        }
        if (!studyMaterialRepository.existsByIdAndUser_Id(materialId, userId)) {
            throw new CustomException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        studyMaterialRepository.selectOnly(userId, materialId);
        return new SelectedMaterialResponse(materialId);
    }

    /** 학습 화면의 "학습 자료" 탭에서 보여줄 파일을 연다 */
    @Transactional(readOnly = true)
    public MaterialFile openFile(Long userId, Long materialId) {
        StudyMaterial material = getOwned(userId, materialId);
        MaterialFileType type = MaterialFileType.valueOf(material.getFileType().toUpperCase(Locale.ROOT));
        InputStream stream = fileStorage.open(material.getFilePath());
        return new MaterialFile(material.getOriginFileName(), type.mediaType(), material.getFileSize(), stream);
    }

    private StudyMaterial getOwned(Long userId, Long materialId) {
        return studyMaterialRepository.findByIdAndUser_Id(materialId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATERIAL_NOT_FOUND));
    }

    private void deleteFileAfterCommit(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorage.delete(key);
                }
            });
        } else {
            fileStorage.delete(key);
        }
    }

    /** 브라우저가 보낸 경로 조각과 제어문자를 제거하고, 비어 있으면 기본 이름을 쓴다 */
    private String cleanFileName(String rawName, MaterialFileType type) {
        String name = rawName == null ? null : StringUtils.getFilename(StringUtils.cleanPath(rawName));
        if (name != null) {
            name = name.replaceAll("\\p{Cntrl}", "").trim();
        }
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("." + type.extension())) {
            return "자료." + type.extension();
        }
        return name;
    }

    /** 표시 이름은 최대 100자. 넘으면 확장자를 남기고 앞부분을 줄인다 */
    private String toDisplayName(String originName) {
        if (originName.length() <= NAME_MAX_LENGTH) {
            return originName;
        }
        int dot = originName.lastIndexOf('.');
        String ext = dot > 0 ? originName.substring(dot) : "";
        String base = dot > 0 ? originName.substring(0, dot) : originName;
        return truncate(base, NAME_MAX_LENGTH - ext.length()) + ext;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
