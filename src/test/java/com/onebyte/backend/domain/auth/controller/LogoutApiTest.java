package com.onebyte.backend.domain.auth.controller;

import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그아웃 API HTTP 계약과 토큰 폐기 */
class LogoutApiTest extends ApiTestSupport {

    private String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    @Test
    void 로그아웃_응답과_재발급_차단() throws Exception {
        LoginResponse login = signupAndLogin("logout-api@test.com");

        mockMvc.perform(authorized(post("/api/auth/logout"), login.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."));

        mockMvc.perform(json(post("/api/auth/refresh"), refreshBody(login.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(authorized(post("/api/auth/logout"), login.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void 다른_사용자의_토큰은_그대로() throws Exception {
        LoginResponse me = signupAndLogin("logout-api-me@test.com");
        LoginResponse other = signupAndLogin("logout-api-other@test.com");

        mockMvc.perform(authorized(post("/api/auth/logout"), me.accessToken())).andExpect(status().isOk());

        mockMvc.perform(json(post("/api/auth/refresh"), refreshBody(other.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void 토큰이_없거나_잘못되면_401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
