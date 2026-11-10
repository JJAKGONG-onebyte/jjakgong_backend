## 🔗 Related Issue
<!-- 연결할 이슈 번호를 적어주세요. PR이 머지되면 이슈가 자동으로 닫힙니다. -->
- Resolves: #

## 📝 Summary
<!-- 이 PR에서 무엇을 변경했는지 요약하여 적어주세요. -->

## 🚀 Key Changes
<!-- 어떤 부분이 변경되었는지 구체적으로 적어주세요. -->
- 
- 

## 🗄 Entity / DB Changes
<!-- 엔티티(테이블·컬럼)를 추가하거나 바꿨다면 꼭 적어주세요. 로컬 DB 초기화가 필요한지도 함께 적어주세요. -->
- 없음

## 💥 Breaking Changes
<!-- 기존 API 응답, 공통 코드(ApiResponse, ErrorCode 등)가 바뀌어 다른 사람에게 영향이 있다면 적어주세요. -->
- 없음

## 🧪 Test
<!-- 어떻게 확인했는지 적어주세요. (예: Swagger로 POST /api/study-sessions 호출 → 201 확인) -->
- 

## ✅ Self-Check List
- [ ] 스스로 코드를 한 번 이상 리뷰했습니다. (Self-review)
- [ ] `./gradlew build`가 통과합니다.
- [ ] 응답은 `ApiResponse`로 감싸고, 에러는 `CustomException(ErrorCode.XXX)`로 던졌습니다.
- [ ] 엔티티를 응답으로 직접 반환하지 않고 DTO로 변환했습니다.
- [ ] `System.out.println`, 불필요한 주석, 테스트용 임시 코드(`permitAll` 등)가 남아있지 않습니다.
- [ ] 비밀번호, API 키 같은 비밀값이 코드에 들어가지 않았습니다.
