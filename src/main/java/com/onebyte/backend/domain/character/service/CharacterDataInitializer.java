package com.onebyte.backend.domain.character.service;

import com.onebyte.backend.domain.character.entity.BaseCharacter;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 기본 캐릭터가 하나도 없으면 초기 2종을 넣는다. (빈 테이블 기준 짝이=1, 꽁이=2). 이미지 경로는 임시값 */
@Component
@RequiredArgsConstructor
public class CharacterDataInitializer implements ApplicationRunner {

    private final BaseCharacterRepository baseCharacterRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (baseCharacterRepository.count() > 0) {
            return;
        }
        baseCharacterRepository.saveAll(List.of(
                BaseCharacter.builder().name("짝이").imageUrl("/characters/jjak.png").build(),
                BaseCharacter.builder().name("꽁이").imageUrl("/characters/ggong.png").build()
        ));
    }
}
