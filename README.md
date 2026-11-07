# 📢 백엔드 개발 환경 안내

## 1. 시작하기

저장소: https://github.com/JJAKGONG-onebyte/jjakgong_backend
기본 작업 브랜치: `develop` (main에는 작업하지 않습니다)

```bash
git clone https://github.com/JJAKGONG-onebyte/jjakgong_backend.git
cd jjakgong_backend
git checkout develop
docker compose up -d
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- 확인: http://localhost:8080/api/exam → `ok`
- Swagger: http://localhost:8080/swagger-ui.html

## 2. 설치 목록

- **JDK 17** (Temurin 17 추천) → `java -version`에서 17 확인
- **Docker Desktop** (실행해 둔 상태로)
- **Git**
- **VS Code 확장**: Extension Pack for Java, Spring Boot Extension Pack, Gradle for Java, Lombok Annotations Support
- **DBeaver** (DB 확인용, 선택)
- 빌드 도구는 **Gradle**로 통일 (Maven 사용 X)
- Spring Boot는 **4.1.2** 입니다. (3.x가 아닙니다!)

## 3. 브랜치 / PR 규칙

- `main`, `develop`에 **직접 push 금지**. 반드시 PR로만 합칩니다.
- 기능마다 브랜치를 새로 만듭니다.

```
feature/auth-login
feature/study-session
fix/jwt-expire-bug
```

- 작업 순서

```bash
git checkout develop
git pull
git checkout -b feature/기능이름
# 코딩...
git add .
git commit -m "feat: 로그인 API 구현"
git push -u origin feature/기능이름
# GitHub에서 develop 대상으로 PR 생성
```

- 머지 전에 `./gradlew build` 통과 확인

## 4. 커밋 메시지 규칙

| `feat:` | 기능 추가 | `feat: 회원가입 API 구현` |
| `fix:` | 버그 수정 | `fix: 토큰 만료 시간 오류 수정` |
| `refactor:` | 동작 변경 없는 구조 개선 | `refactor: 서비스 로직 분리` |
| `docs:` | 문서 수정 | `docs: README 수정` |
| `chore:` | 설정, 빌드, 의존성 | `chore: gradle 의존성 추가` |
| `test:` | 테스트 코드 | `test: 로그인 테스트 추가` |

## 5. API URL 통합본 (변경사항)

**모든 API는 `/api/...`로 통일. `/api/v1`은 사용하지 않습니다.**

| 변경 전                                            | 변경 후                                      |
| -------------------------------------------------- | -------------------------------------------- |
| `/api/v1/dashboard`                                | `/api/dashboard`                             |
| `/api/v1/auth/logout`                              | `/api/auth/logout`                           |
| `/api/v1/schedules/...`                            | `/api/schedules/...`                         |
| `POST /api/study-sessions/{problemId}/submissions` | `POST /api/problems/{problemId}/submissions` |

- AI 서버가 호출하는 내부 API는 `/internal/...` 유지
- 최종 명세서는 노션에 있는 통합본을 기준으로 합니다.

## 6. 패키지 구조

기본 패키지: `com.onebyte.backend`
(경로: `src/main/java/com/onebyte/backend`)

```
com.onebyte.backend
├── BackendApplication.java
├── global
│   ├── config       # SecurityConfig 등 설정
│   ├── entity       # BaseEntity
│   ├── exception    # CustomException, GlobalExceptionHandler
│   ├── response     # ApiResponse (공통 응답)
│   └── jwt          # JwtProvider
└── domain
    ├── auth
    ├── user
    └── (기능별로 추가: studysession, material, schedule 등)
```

각 도메인 폴더 안은 이렇게 통일합니다.

```
domain/user
├── controller   # 요청/응답 입구
├── service      # 비즈니스 로직
├── repository   # DB 접근 (JPA)
├── entity       # 테이블 매핑 클래스
└── dto          # 요청/응답 전용 객체
```

## 7. 공통 응답 / 예외 처리 규칙
* 모든 API 응답은 `ApiResponse`로 감쌉니다.
  * 성공: `{ "success": true, "data": { ... } }`
  * 실패: `{ "success": false, "error": { "code": "...", "message": "..." } }`
* 에러는 `throw new CustomException(ErrorCode.XXX)` 형태로 던집니다. (`GlobalExceptionHandler`가 알아서 응답으로 변환)
  * 새 에러는 `ErrorCode` enum에 추가해서 사용합니다.
* 로그인 사용자 ID는 컨트롤러에서 `@AuthenticationPrincipal Long userId`로 꺼냅니다.
* 날짜/시간 형식은 ISO 8601: `2026-11-06T14:30:00`
* 엔티티는 `BaseEntity`를 상속해서 `createdAt`, `updatedAt`을 자동 관리합니다.
* 엔티티를 응답으로 직접 반환하지 말고 DTO로 변환해서 반환합니다.

## 8. DB 사용 규칙

- **각자 자기 PC의 Docker DB를 사용**합니다. (공용 DB 없음)
- 로컬은 `ddl-auto: update`라서 엔티티를 바꾸면 테이블이 자동 변경됩니다.
- 꼬이면 DB를 초기화합니다. (데이터가 전부 삭제되니 로컬에서만!)

```bash
docker compose down -v
docker compose up -d
```

- 엔티티 구조를 바꾼 PR은 **PR 설명에 꼭 적어 주세요.**
