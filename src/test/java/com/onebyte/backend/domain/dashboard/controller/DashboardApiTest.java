package com.onebyte.backend.domain.dashboard.controller;

import com.onebyte.backend.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 대시보드 API HTTP 계약 (필드명, null 노출, 날짜 형식, 오류 코드, 인증) */
class DashboardApiTest extends ApiTestSupport {

    @Test
    void 신규_사용자_응답_구조() throws Exception {
        String token = signupAndLogin("dash-api@test.com").accessToken();

        mockMvc.perform(authorized(get("/api/dashboard"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.data.user.displayName").value("tester"))
                .andExpect(jsonPath("$.data.studyTime.todaySeconds").value(0))
                .andExpect(jsonPath("$.data.studyTime.goalSeconds").value(3000))
                .andExpect(jsonPath("$.data.studyTime.achievementRate").value(0))
                .andExpect(jsonPath("$.data.streak.days").value(0))
                .andExpect(jsonPath("$.data.character.characterId").isNumber())
                .andExpect(jsonPath("$.data.character.level").value(1))
                .andExpect(jsonPath("$.data.character.minutesToNextLevel").isNumber())
                .andExpect(jsonPath("$.data.character.levelProgressPercent").value(0))
                .andExpect(jsonPath("$.data.character.appearanceUrl").isString())
                .andExpect(jsonPath("$.data.todaySchedules", hasSize(0)))
                .andExpect(jsonPath("$.data.learningMaterials", hasSize(0)))
                .andExpect(jsonPath("$.data.recentStudyRecords", hasSize(0)))
                .andExpect(jsonPath("$.data.defaultLearningMaterialId").value(nullValue()));
    }

    @Test
    void 캐릭터가_없으면_character는_null로_노출() throws Exception {
        String token = tokenOfBareUser("dash-api-bare@test.com");

        mockMvc.perform(authorized(get("/api/dashboard"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.character").value(nullValue()));
    }

    @Test
    void 기준_날짜의_일정과_time_null_노출() throws Exception {
        String token = signupAndLogin("dash-api-schedule@test.com").accessToken();
        mockMvc.perform(json(authorized(post("/api/schedules"), token),
                        "{\"title\":\"과제\",\"date\":\"2026-10-08\",\"time\":\"14:00\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(json(authorized(post("/api/schedules"), token),
                        "{\"title\":\"복습\",\"date\":\"2026-10-08\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/dashboard?date=2026-10-08"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-10-08"))
                .andExpect(jsonPath("$.data.todaySchedules[*].title", contains("복습", "과제")))
                .andExpect(jsonPath("$.data.todaySchedules[0].time").value(nullValue()))
                .andExpect(jsonPath("$.data.todaySchedules[0].occurrenceDate").value("2026-10-08"))
                .andExpect(jsonPath("$.data.todaySchedules[1].time").value("14:00"))
                .andExpect(jsonPath("$.data.todaySchedules[1].priority").value("HIGH"))
                .andExpect(jsonPath("$.data.todaySchedules[1].completed").value(false));
    }

    @Test
    void 날짜_형식_오류와_미인증() throws Exception {
        String token = signupAndLogin("dash-api-error@test.com").accessToken();

        mockMvc.perform(authorized(get("/api/dashboard?date=2026/10/08"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_FORMAT"));
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
