# CLAUDE.md — Survival 365 모드 (Claude Code 작업 규칙)

이 파일은 Claude Code가 이 저장소에서 작업할 때 항상 따르는 규칙이다.
전체 기능 명세는 `docs/SPEC.md`에 있다. **작업을 시작하기 전에 반드시 `docs/SPEC.md` 전체를 읽는다.**

## 프로젝트 한 줄 요약
마인크래프트 Java Edition용 Fabric 모드. 월드 생성 시 "365일 생존 난이도"를 켜면 D-365 카운트다운이 생기고,
"세계"(시나리오)를 고를 수 있다. 첫 시나리오는 "바그다드 마크"(항상 밤 + 끝없는 사막 + 주기적 TNT 폭격 + 습격).
앞으로 시나리오와 기능이 계속 추가되므로 **확장성을 최우선**으로 설계한다.

## 작업 방식
- 마일스톤으로 나누지 않는다. SPEC.md의 모든 기능을 한 번에 끝까지 구현한다.
- 구현 순서는 자유지만, 끝났을 때 `./gradlew build`가 성공하고 `./gradlew runClient`로 SPEC.md §12 확인 목록을 통과할 수 있어야 한다.
- 모르는 API는 추측하지 않는다. `./gradlew genSources` 후 실제 마인크래프트 소스와 Fabric API 소스를 열어서 클래스·메서드 이름과 시그니처를 확인한 뒤 작성한다. (26.x에서 바닐라 클래스 구조가 자주 바뀐다.)
- Mixin은 최소한으로 쓴다. Fabric API 이벤트로 가능한 것은 이벤트로 한다. Mixin을 쓸 때는 대상 메서드를 소스에서 확인하고 `@Inject`/`@ModifyArg` 위주로 쓰며 `@Overwrite`는 금지한다.
- 서버/클라이언트 코드를 분리한다. 클라이언트 전용 클래스(화면, HUD, 렌더링)는 `src/client/java` 소스셋에만 둔다 (Loom split source sets).
- 게임 로직(타이머, 폭격, 스폰, D-day 계산)은 전부 **서버 쪽**에서 돈다. 클라이언트는 표시만 한다. → 멀티플레이 서버에서도 동작해야 한다.
- 화면에 보이는 문자열은 하드코딩하지 않는다. 전부 번역 키를 쓰고 `ko_kr.json`, `en_us.json` 둘 다 채운다.
- 수치(타이머 길이, TNT 개수, 간격, 스폰 수 등)는 하드코딩하지 않는다. `config/survival365.json` 설정값으로 뺀다 (SPEC.md §9).

## 버전 고정
- 대상: 작업 시점의 **최신 안정판 Minecraft Java (26.x 계열, 비난독화 버전)**.
- 시작할 때 https://fabricmc.net/develop/ 에서 해당 버전의 권장 Fabric Loader / Loom / Fabric API 버전을 확인해 `gradle.properties`에 고정한다.
- Loom 플러그인 ID: `net.fabricmc.fabric-loom` (비난독화 버전용). `mappings` 줄은 넣지 않는다. Yarn 쓰지 않는다.
- Java: 해당 마인크래프트 버전이 요구하는 JDK (26.1 기준 Java 25).

## 식별자
- mod id: `survival365`
- 기본 패키지: `com.junseok.survival365`
- 모드 이름(표시): `Survival 365`

## 완료 기준 (매번 확인)
1. `./gradlew build` 성공, 경고성 Mixin 오류 없음.
2. `./gradlew runClient`로 새 월드 생성 → SPEC.md §12 수동 테스트 항목 전부 통과.
3. `./gradlew runServer`(전용 서버)로 실행해도 크래시 없음 (클라이언트 클래스가 서버에 로드되면 안 됨).
4. 새 시나리오를 추가하는 방법이 `docs/EXTENDING.md`에 정리되어 있음 (구현 후 작성).

## 하지 말 것
- 바닐라 클래스를 통째로 복사해서 덮어쓰기.
- 클라이언트에서 게임 상태(타이머·D-day)를 계산하기.
- 매 틱마다 모든 엔티티/청크를 순회하는 무거운 로직.
- 사용자에게 묻지 않고 SPEC.md에 없는 큰 게임플레이 변경 추가. (사소한 구현 판단은 스스로 하고 `docs/DECISIONS.md`에 한 줄씩 기록)
