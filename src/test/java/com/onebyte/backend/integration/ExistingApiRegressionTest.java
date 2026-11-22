package com.onebyte.backend.integration;

import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 일정·대시보드·로그아웃 추가 후 기존 API가 그대로 동작하는지 확인하는 스모크 테스트.
 * 응답 구조의 세부 검증은 각 담당자 범위라 정상 응답 여부만 본다.
 * (파일 업로드는 로컬 저장소에 파일을 남기므로 제외)
 */
class ExistingApiRegressionTest extends ApiTestSupport {

    @Test
    void 기존_API_스모크() throws Exception {
        LoginResponse login = signupAndLogin("regression@test.com");
        String token = login.accessToken();

        mockMvc.perform(get("/api/exam")).andExpect(status().isOk());
        // 명세상 인증 불필요(회원가입 화면)지만 현재 SecurityConfig permitAll에 없어 토큰을 붙여 동작만 확인한다
        mockMvc.perform(authorized(get("/api/characters"), token)).andExpect(status().isOk());
        mockMvc.perform(json(post("/api/auth/refresh"), "{\"refreshToken\":\"" + login.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());

        mockMvc.perform(authorized(get("/api/users/me"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/users/me/character"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/study/settings"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/materials"), token)).andExpect(status().isOk());

        MvcResult started = mockMvc.perform(json(authorized(post("/api/study-sessions"), token), "{}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = readData(started).get("sessionId").asLong();
        mockMvc.perform(authorized(get("/api/study-sessions/active"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/study-sessions/" + sessionId + "/end"), token)).andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/study/stats/streak"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/study/stats/weekly"), token)).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/study/records"), token)).andExpect(status().isOk());
    }
}
