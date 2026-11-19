# 구현 판단 기록 (DECISIONS)

SPEC에 없거나 26.3 API 때문에 SPEC과 달라진 부분을 한 줄씩 기록한다.

## 버전
- 2026-11-19 기준 최신 안정판으로 고정: Minecraft 26.3, Fabric Loader 0.20.1, Loom 1.19.4, Fabric API 0.166.2+26.3, Gradle 9.8.2, Java 25.

## 26.3 API 차이
- `ResourceLocation` 대신 `Identifier`를 쓴다(26.x에서 이름이 바뀜).
- 시간: 26.3은 `level.getDayTime()` 대신 월드 시계(`ServerClockManager`, `WorldClock`)를 쓴다. 오버월드 시간 = `dimensionType().defaultClock()`의 `totalTicks`.
- `doDaylightCycle` 게임 규칙은 `GameRules.ADVANCE_TIME`(`advance_time`, 서버 전역 규칙)으로 바뀌었다. 끄면 잠을 자도 시간이 이동하지 않는다(`ServerLevel.tick`에서 확인).
- noise_settings에 `aquifers_enabled`가 없다. 대신 `aquifers` 객체가 있는데, 이 키를 빼면 대수층이 꺼진다(`NoiseChunk`가 `Aquifer.createDisabled` 사용).
- `SavedDataType`에는 `DataFixTypes`가 필요하다. `null`을 넘기고, Fabric API의 `SavedDataStorageMixin`이 이를 처리한다.
- HUD는 `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` 방식이다. F1 숨김 상태는 `minecraft.gui.hud.isHidden()`으로 확인한다.
- 생성 화면의 `GameTab`/`WorldTab` 내부 클래스에는 `this$0` 필드가 없다. 바깥 화면은 Mixin에서 `@Local(argsOnly = true)` 생성자 인자로 받는다.

## 구현 판단
- 저장 파일 위치는 `data/survival365/challenge.dat`이다(26.3에서 SavedData id가 `네임스페이스/경로`로 저장됨).
- "세계" 버튼의 값은 `WorldScenario`가 아니라 시나리오 `Identifier`로 순환한다. 표시 이름은 그때그때 `Scenarios.create(id).displayName()`으로 얻는다.
- 바그다드 저장 데이터는 `phase`, `phaseTicksLeft`, `phaseLength`로 둔다. `waveIndex`와 `spawnTimer`는 페이즈 경과 틱에서 계산되므로 저장하지 않는다.
- TNT 착지 높이는 `getHeight(MOTION_BLOCKING_NO_LEAVES)`의 반환값(지표 위 첫 빈칸)을 기준으로 하고, 이 높이에 닿을 때까지 떨어지는 틱을 시뮬레이션한다. 상수는 바닐라 `PrimedTnt` 값과 같다(중력 0.04, 공기 저항 0.98).
- 습격 몹은 스폰된 기준 플레이어를 공격 대상으로 정한다. 바로 그 플레이어 주변 16~32칸에 스폰되므로 대부분 가장 가까운 플레이어다.
- 습격 몹 스폰 사유는 `EntitySpawnReason.EVENT`이다. 바닐라에서 이 사유는 습격 대장 추첨을 건너뛴다. 그래도 `setPatrolLeader(false)`를 한 번 더 호출한다.
- 습격 몹의 스폰 공간은 `EntityType.getSpawnAABB`로 검사한다. 파괴수처럼 큰 몹은 자동으로 더 넓은 공간이 필요하다.
- HUD 경고 색 기준(`warningDays`)은 서버 설정값이므로 `CountdownSyncPayload`에 `warningDays` 필드를 추가해 보낸다.
- 사운드 id(하루 경과, D-DAY, 폭격 시작, 습격 시작)도 설정값으로 뺐다(`countdown.dayChangeSound`, `countdown.completeSound`, `baghdad.bombardmentStartSound`, `baghdad.raidStartSound`). 잘못된 id는 무시한다.
- 전용 서버의 "새 월드" 판정은 `ChallengeState`가 없고 오버월드 `gameTime == 0`인 경우다. 기존 월드에 모드를 새로 넣었을 때는 켜지지 않는다.
- 챌린지가 꺼진 월드에도 `ChallengeState(enabled=false)`를 저장한다. 그래서 나중에 그 월드를 다시 열어도 "새 월드"로 잘못 판정되지 않는다.
- 공통 Mixin이 필요 없어서 `survival365.mixins.json`(main)은 만들지 않았다. Mixin은 클라이언트 4개뿐이다.
- `/s365 skip`은 페이즈가 있는 시나리오 전용이라 바그다드가 `registerCommands`로 등록한다(`/s365 phase`와 같은 방식).
- 하루가 지날 때 액션바 문구는 `hud.survival365.day_passed`이고 인자로 "D-364"를 넘긴다.
- 자동 확인용으로 Fabric 클라이언트 게임테스트(`src/gametest`, `./gradlew runClientGameTest`)를 추가했다. SPEC §12 항목 중 자동화할 수 있는 것을 검사한다.

## 업데이트·배포
- 업데이트는 "알림만" 한다: 클라이언트 시작 시 GitHub `releases/latest`를 1회 조회하고, 월드 접속 시 채팅에 다운로드 링크를 띄운다. jar를 자동으로 내려받거나 교체하지 않는다 (서버/클라이언트 버전 불일치·파일 잠금 위험).
- 버전 비교는 태그의 `v`와 `-`/`+` 뒤를 버리고 점 단위 숫자로 비교한다. 조회 실패·404는 로그만 남긴다.
- `config/survival365.json`의 `updates.checkForUpdates`로 끌 수 있다. 요청에는 버전 비교용 User-Agent 외 개인 정보를 보내지 않는다.
- 릴리스는 GitHub Actions(`.github/workflows/build.yml`): `v*` 태그 푸시 시 `-Pversion=<태그>`로 빌드해 Release에 jar를 첨부한다.
