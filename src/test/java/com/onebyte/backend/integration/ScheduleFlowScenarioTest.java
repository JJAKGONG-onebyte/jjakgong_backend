package com.onebyte.backend.integration;

import com.onebyte.backend.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 → 로그인 → 반복 일정 등록 → 완료 → THIS 수정 → ALL_FUTURE 분할 → THIS 삭제
 * → 목록·상세·대시보드 반영 → ALL 삭제 → 로그아웃까지 HTTP로 이어서 검증한다.
 */
class ScheduleFlowScenarioTest extends ApiTestSupport {

    private final LocalDate d0 = LocalDate.now();
    private final LocalDate d1 = d0.plusDays(1);
    private final LocalDate d2 = d0.plusDays(2);
    private final LocalDate d3 = d0.plusDays(3);
    private final LocalDate d4 = d0.plusDays(4);

    @Test
    void 일정_전체_흐름() throws Exception {
        // 회원가입 · 로그인
        Long characterId = baseCharacterRepository.findAll().get(0).getCharacterId();
        mockMvc.perform(json(post("/api/auth/signup"), """
                        {"email":"flow@test.com","password":"test1234","nickname":"flow","characterId":%d}
                        """.formatted(characterId)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(json(post("/api/auth/login"),
                        "{\"email\":\"flow@test.com\",\"password\":\"test1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokens = readData(login);
        String token = tokens.get("accessToken").asString();
        String refreshToken = tokens.get("refreshToken").asString();

        // 오늘부터 5일간 매일 반복
        MvcResult created = mockMvc.perform(json(authorized(post("/api/schedules"), token), """
                        {"title":"영어 단어 복습","date":"%s","time":"20:00","priority":"LOW",
                         "recurrence":{"frequency":"DAILY","endDate":"%s"}}
                        """.formatted(d0, d4)))
                .andExpect(status().isCreated())
                .andReturn();
        long seriesId = readData(created).get("scheduleId").asLong();

        // 오늘 회차 완료
        mockMvc.perform(json(authorized(patch("/api/schedules/%d/completion?occurrenceDate=%s".formatted(seriesId, d0)), token),
                        "{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));

        // 내일 회차만 제목 변경
        mockMvc.perform(json(authorized(patch("/api/schedules/%d?occurrenceDate=%s&scope=THIS".formatted(seriesId, d1)), token),
                        "{\"title\":\"단어 시험\"}"))
                .andExpect(status().isOk());

        // 3일 뒤부터 시간 변경 (시리즈 분할)
        MvcResult split = mockMvc.perform(json(authorized(
                        patch("/api/schedules/%d?occurrenceDate=%s&scope=ALL_FUTURE".formatted(seriesId, d3)), token),
                        "{\"time\":\"07:00\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long futureId = readData(split).get("scheduleId").asLong();
        assertThat(futureId).isNotEqualTo(seriesId);

        // 2일 뒤 회차만 삭제
        mockMvc.perform(authorized(delete("/api/schedules/%d?scope=THIS&occurrenceDate=%s".formatted(seriesId, d2)), token))
                .andExpect(status().isOk());

        // 목록 반영
        mockMvc.perform(authorized(get("/api/schedules?startDate=%s&endDate=%s".formatted(d0, d4)), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules[*].occurrenceDate",
                        contains(d0.toString(), d1.toString(), d3.toString(), d4.toString())))
                .andExpect(jsonPath("$.data.schedules[*].title",
                        contains("영어 단어 복습", "단어 시험", "영어 단어 복습", "영어 단어 복습")))
                .andExpect(jsonPath("$.data.schedules[*].time", contains("20:00", "20:00", "07:00", "07:00")))
                .andExpect(jsonPath("$.data.schedules[*].completed", contains(true, false, false, false)));

        // 상세 반영
        mockMvc.perform(authorized(get("/api/schedules/%d?occurrenceDate=%s".formatted(seriesId, d1)), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("단어 시험"))
                .andExpect(jsonPath("$.data.recurrence.endDate").value(d2.toString()));
        mockMvc.perform(authorized(get("/api/schedules/%d?occurrenceDate=%s".formatted(seriesId, d2)), token))
                .andExpect(status().isNotFound());

        // 대시보드 오늘 일정 = 일정 목록(오늘)
        MvcResult todayList = mockMvc.perform(authorized(
                        get("/api/schedules?startDate=%s&endDate=%s".formatted(d0, d0)), token))
                .andReturn();
        MvcResult dashboard = mockMvc.perform(authorized(get("/api/dashboard"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todaySchedules[0].scheduleId").value(seriesId))
                .andExpect(jsonPath("$.data.todaySchedules[0].completed").value(true))
                .andReturn();
        JsonNode listItems = readData(todayList).get("schedules");
        JsonNode dashboardItems = readData(dashboard).get("todaySchedules");
        assertThat(dashboardItems.size()).isEqualTo(listItems.size());
        for (int i = 0; i < listItems.size(); i++) {
            for (String field : new String[]{"scheduleId", "title", "time", "priority", "completed", "occurrenceDate"}) {
                assertThat(dashboardItems.get(i).get(field)).isEqualTo(listItems.get(i).get(field));
            }
        }

        // 앞쪽 시리즈 전체 삭제 → 뒤쪽 시리즈만 남음
        mockMvc.perform(authorized(delete("/api/schedules/%d?scope=ALL".formatted(seriesId)), token))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/schedules?startDate=%s&endDate=%s".formatted(d0, d4)), token))
                .andExpect(jsonPath("$.data.schedules[*].occurrenceDate", contains(d3.toString(), d4.toString())));

        // 로그아웃 → 재발급 불가
        mockMvc.perform(authorized(post("/api/auth/logout"), token)).andExpect(status().isOk());
        mockMvc.perform(json(post("/api/auth/refresh"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
