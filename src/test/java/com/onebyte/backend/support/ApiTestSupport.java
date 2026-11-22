package com.onebyte.backend.support;

import com.onebyte.backend.domain.auth.dto.LoginRequest;
import com.onebyte.backend.domain.auth.dto.LoginResponse;
import com.onebyte.backend.domain.auth.dto.SignupRequest;
import com.onebyte.backend.domain.auth.service.AuthService;
import com.onebyte.backend.domain.character.repository.BaseCharacterRepository;
import com.onebyte.backend.domain.user.entity.User;
import com.onebyte.backend.domain.user.repository.UserRepository;
import com.onebyte.backend.global.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 수준 통합 테스트 공통 도우미.
 * 인증 필터·전역 예외 처리·JSON 직렬화를 실제 설정 그대로 거친다. 각 테스트는 트랜잭션으로 롤백된다.
 * (기존 contextLoads와 같이 로컬 MySQL이 필요하다)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestSupport {

    /** ISO 8601 시각 (오프셋·소수점 없음) */
    protected static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected AuthService authService;
    @Autowired protected UserRepository userRepository;
    @Autowired protected BaseCharacterRepository baseCharacterRepository;
    @Autowired protected JwtProvider jwtProvider;

    /** 회원가입(학습 설정·캐릭터 생성 포함) 후 로그인 */
    protected LoginResponse signupAndLogin(String email) {
        Long characterId = baseCharacterRepository.findAll().get(0).getCharacterId();
        authService.signup(new SignupRequest(email, "test1234", "tester", characterId));
        return authService.login(new LoginRequest(email, "test1234"));
    }

    /** 회원가입 흐름을 거치지 않은 사용자 (캐릭터·학습 설정 없음)의 access token */
    protected String tokenOfBareUser(String email) {
        User user = userRepository.save(User.create(email, "bare", "pw"));
        return jwtProvider.createAccessToken(user.getId());
    }

    protected static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder, String accessToken) {
        return builder.header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(body);
    }

    protected JsonNode readData(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }
}
