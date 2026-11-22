package com.onebyte.backend.domain.schedule.controller;

import com.onebyte.backend.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 일정 API 6개의 HTTP 계약 (상태 코드, 응답 구조, 필드명, null 노출, 날짜 형식, 오류 코드, 권한) */
class ScheduleApiTest extends ApiTestSupport {

    private static final String NORMAL_BODY = """
            {"title":"데이터베이스 과제 제출","date":"2026-10-08","time":"14:00",
             "deadlineDate":"2026-10-10","priority":"HIGH","recurrence":null}
            """;
    private static final String RECURRING_BODY = """
            {"title":"영어 단어 복습","date":"2026-10-08","time":null,"deadlineDate":null,"priority":"MEDIUM",
             "recurrence":{"frequency":"DAILY","endDate":"2026-10-10","count":null}}
            """;

    private String token;

    @BeforeEach
    void setUp() {
        token = signupAndLogin("schedule-api@test.com").accessToken();
    }

    private long create(String body) throws Exception {
        MvcResult result = mockMvc.perform(json(authorized(post("/api/schedules"), token), body))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(result).get("scheduleId").asLong();
    }

    private void expectError(MockHttpServletRequestBuilder request, int status, String code) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").isString());
    }

    // ===== 등록 =====

    @Test
    void 일반_일정_등록_응답() throws Exception {
        mockMvc.perform(json(authorized(post("/api/schedules"), token), NORMAL_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.scheduleId").isNumber())
                .andExpect(jsonPath("$.data.title").value("데이터베이스 과제 제출"))
                .andExpect(jsonPath("$.data.date").value("2026-10-08"))
                .andExpect(jsonPath("$.data.time").value("14:00"))
                .andExpect(jsonPath("$.data.deadlineDate").value("2026-10-10"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.recurrence").value(nullValue()))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern(DATE_TIME_PATTERN)));
    }

    @Test
    void 반복_일정_등록_응답() throws Exception {
        mockMvc.perform(json(authorized(post("/api/schedules"), token), RECURRING_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.time").value(nullValue()))
                .andExpect(jsonPath("$.data.deadlineDate").value(nullValue()))
                .andExpect(jsonPath("$.data.recurrence.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.recurrence.endDate").value("2026-10-10"))
                .andExpect(jsonPath("$.data.recurrence.count").value(nullValue()));
    }

    @Test
    void 등록_오류_코드() throws Exception {
        expectError(json(authorized(post("/api/schedules"), token), "{\"title\":\"  \",\"date\":\"2026-10-08\"}"),
                400, "TITLE_REQUIRED");
        expectError(json(authorized(post("/api/schedules"), token),
                        "{\"title\":\"x\",\"date\":\"2026-10-08\",\"recurrence\":{\"frequency\":\"DAILY\",\"endDate\":\"2026-10-01\"}}"),
                400, "INVALID_RECURRENCE");
        expectError(json(authorized(post("/api/schedules"), token),
                        "{\"title\":\"x\",\"date\":\"2026-10-08\",\"recurrence\":{\"frequency\":\"DAILY\",\"endDate\":\"2026-10-31\",\"count\":3}}"),
                400, "INVALID_RECURRENCE");
        expectError(json(authorized(post("/api/schedules"), token), "{\"title\":\"x\",\"date\":\"2026/10/08\"}"),
                400, "INVALID_DATE_FORMAT");
        expectError(json(authorized(post("/api/schedules"), token), "{\"title\":\"x\",\"date\":\"2026-10-08\",\"time\":\"25:00\"}"),
                400, "INVALID_DATE_FORMAT");
        expectError(json(authorized(post("/api/schedules"), token), "{\"title\":\"x\",\"date\":\"2026-10-08\",\"priority\":\"URGENT\"}"),
                400, "INVALID_INPUT");
        expectError(json(authorized(post("/api/schedules"), token), "{\"title\":"),
                400, "INVALID_INPUT");
    }

    // ===== 목록 =====

    @Test
    void 목록_응답은_발생일별이고_필드명과_null을_명세대로_노출() throws Exception {
        create(NORMAL_BODY);
        create(RECURRING_BODY);

        mockMvc.perform(authorized(get("/api/schedules?startDate=2026-10-01&endDate=2026-10-31"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-10-31"))
                .andExpect(jsonPath("$.data.schedules", hasSize(4)))
                .andExpect(jsonPath("$.data.schedules[*].occurrenceDate",
                        contains("2026-10-08", "2026-10-08", "2026-10-09", "2026-10-10")))
                .andExpect(jsonPath("$.data.schedules[0].isRecurring").value(true))
                .andExpect(jsonPath("$.data.schedules[0].recurring").doesNotExist())
                .andExpect(jsonPath("$.data.schedules[0].time").value(nullValue()))
                .andExpect(jsonPath("$.data.schedules[1].isRecurring").value(false))
                .andExpect(jsonPath("$.data.schedules[1].time").value("14:00"));
    }

    @Test
    void 일정이_없으면_빈_배열() throws Exception {
        mockMvc.perform(authorized(get("/api/schedules?startDate=2026-10-01&endDate=2026-10-31"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules", hasSize(0)));
    }

    @Test
    void 목록_오류_코드() throws Exception {
        expectError(authorized(get("/api/schedules?startDate=2026-10-31&endDate=2026-10-01"), token), 400, "INVALID_DATE_RANGE");
        expectError(authorized(get("/api/schedules?startDate=2026-01-01&endDate=2027-01-02"), token), 400, "INVALID_DATE_RANGE");
        expectError(authorized(get("/api/schedules?startDate=2026-10-01"), token), 400, "INVALID_INPUT");
        expectError(authorized(get("/api/schedules?startDate=20261001&endDate=2026-10-31"), token), 400, "INVALID_DATE_FORMAT");
    }

    // ===== 상세 =====

    @Test
    void 상세_응답() throws Exception {
        long id = create(RECURRING_BODY);

        mockMvc.perform(authorized(get("/api/schedules/" + id + "?occurrenceDate=2026-10-09"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(id))
                .andExpect(jsonPath("$.data.date").value("2026-10-09"))
                .andExpect(jsonPath("$.data.occurrenceDate").value("2026-10-09"))
                .andExpect(jsonPath("$.data.recurrence.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.createdAt").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.updatedAt").value(matchesPattern(DATE_TIME_PATTERN)));

        expectError(authorized(get("/api/schedules/" + id), token), 400, "INVALID_INPUT");
        expectError(authorized(get("/api/schedules/" + id + "?occurrenceDate=2026-10-20"), token), 404, "SCHEDULE_NOT_FOUND");
        expectError(authorized(get("/api/schedules/999999999"), token), 404, "SCHEDULE_NOT_FOUND");
    }

    // ===== 수정 =====

    @Test
    void 수정_응답과_수정_범위() throws Exception {
        long normal = create(NORMAL_BODY);
        long recurring = create(RECURRING_BODY);

        mockMvc.perform(json(authorized(patch("/api/schedules/" + normal), token), "{\"time\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.time").value(nullValue()))
                .andExpect(jsonPath("$.data.title").value("데이터베이스 과제 제출"))
                .andExpect(jsonPath("$.data.updatedAt").value(matchesPattern(DATE_TIME_PATTERN)));

        mockMvc.perform(json(authorized(patch("/api/schedules/" + recurring + "?occurrenceDate=2026-10-09&scope=THIS"), token),
                        "{\"title\":\"시험 대비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(recurring))
                .andExpect(jsonPath("$.data.title").value("시험 대비"));

        mockMvc.perform(json(authorized(patch("/api/schedules/" + recurring + "?occurrenceDate=2026-10-10&scope=ALL_FUTURE"), token),
                        "{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(not((int) recurring)))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));

        expectError(json(authorized(patch("/api/schedules/" + normal + "?scope=THIS"), token), "{\"title\":\"x\"}"),
                400, "INVALID_UPDATE_SCOPE");
        expectError(json(authorized(patch("/api/schedules/" + recurring + "?occurrenceDate=2026-10-08"), token), "{\"title\":\"x\"}"),
                400, "INVALID_UPDATE_SCOPE");
        expectError(json(authorized(patch("/api/schedules/" + normal), token), "{}"), 400, "INVALID_INPUT");
    }

    // ===== 완료 =====

    @Test
    void 완료_응답과_검증() throws Exception {
        long id = create(RECURRING_BODY);

        mockMvc.perform(json(authorized(patch("/api/schedules/" + id + "/completion?occurrenceDate=2026-10-08"), token),
                        "{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(id))
                .andExpect(jsonPath("$.data.occurrenceDate").value("2026-10-08"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.updatedAt").value(matchesPattern(DATE_TIME_PATTERN)));

        expectError(json(authorized(patch("/api/schedules/" + id + "/completion?occurrenceDate=2026-10-08"), token), "{}"),
                400, "INVALID_INPUT");
        expectError(json(authorized(patch("/api/schedules/" + id + "/completion?occurrenceDate=2026-10-08"), token),
                "{\"completed\":\"yes\"}"), 400, "INVALID_INPUT");
        expectError(json(authorized(patch("/api/schedules/" + id + "/completion"), token), "{\"completed\":true}"),
                400, "INVALID_INPUT");
    }

    // ===== 삭제 =====

    @Test
    void 삭제_응답과_삭제_범위() throws Exception {
        long normal = create(NORMAL_BODY);
        long recurring = create(RECURRING_BODY);

        expectError(authorized(delete("/api/schedules/" + recurring), token), 400, "INVALID_DELETE_SCOPE");
        expectError(authorized(delete("/api/schedules/" + recurring + "?scope=THIS"), token), 400, "INVALID_DELETE_SCOPE");
        expectError(authorized(delete("/api/schedules/" + normal + "?scope=ALL"), token), 400, "INVALID_DELETE_SCOPE");

        mockMvc.perform(authorized(delete("/api/schedules/" + recurring + "?scope=THIS&occurrenceDate=2026-10-09"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(authorized(delete("/api/schedules/" + normal), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(authorized(get("/api/schedules?startDate=2026-10-01&endDate=2026-10-31"), token))
                .andExpect(jsonPath("$.data.schedules[*].occurrenceDate", contains("2026-10-08", "2026-10-10")));
    }

    // ===== 인증·권한 =====

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/schedules"),
                Arguments.of(HttpMethod.GET, "/api/schedules?startDate=2026-10-01&endDate=2026-10-31"),
                Arguments.of(HttpMethod.GET, "/api/schedules/1"),
                Arguments.of(HttpMethod.PATCH, "/api/schedules/1"),
                Arguments.of(HttpMethod.PATCH, "/api/schedules/1/completion"),
                Arguments.of(HttpMethod.DELETE, "/api/schedules/1"));
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void 토큰이_없거나_잘못되면_401(HttpMethod method, String uri) throws Exception {
        expectError(json(request(method, uri), "{}"), 401, "UNAUTHORIZED");
        expectError(json(request(method, uri).header("Authorization", "Bearer invalid.token"), "{}"), 401, "UNAUTHORIZED");
    }

    @Test
    void 다른_사용자의_일정은_404이고_목록에_보이지_않는다() throws Exception {
        long id = create(NORMAL_BODY);
        String other = signupAndLogin("schedule-api-other@test.com").accessToken();

        expectError(authorized(get("/api/schedules/" + id), other), 404, "SCHEDULE_NOT_FOUND");
        expectError(json(authorized(patch("/api/schedules/" + id), other), "{\"title\":\"x\"}"), 404, "SCHEDULE_NOT_FOUND");
        expectError(json(authorized(patch("/api/schedules/" + id + "/completion"), other), "{\"completed\":true}"),
                404, "SCHEDULE_NOT_FOUND");
        expectError(authorized(delete("/api/schedules/" + id), other), 404, "SCHEDULE_NOT_FOUND");
        mockMvc.perform(authorized(get("/api/schedules?startDate=2026-10-01&endDate=2026-10-31"), other))
                .andExpect(jsonPath("$.data.schedules", hasSize(0)));

        mockMvc.perform(authorized(get("/api/schedules/" + id), token)).andExpect(status().isOk());
    }
}
