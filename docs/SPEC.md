# Survival 365 — 기능 명세서 (SPEC)

> 이 문서는 Claude Code가 모드 전체를 한 번에 구현하기 위한 기준 문서다.
> 작업 규칙은 저장소 루트의 `CLAUDE.md`를 따른다.
> 문서에 적히지 않은 세부 구현은 Claude Code가 판단하고 `docs/DECISIONS.md`에 기록한다.

---

## 0. 목차
1. 개요
2. 기술 스택과 프로젝트 구조
3. 핵심 개념 (Challenge / Scenario / Day Clock)
4. 월드 생성 화면 (UI)
5. 365일 생존 난이도 공통 규칙
6. D-day 카운트다운 HUD
7. 시나리오: 바그다드 마크
8. 데이터 저장 · 네트워크 동기화
9. 설정 파일
10. 디버그/관리 명령어
11. 전용 서버 지원
12. 수동 테스트 확인 목록
13. 확장 가이드 (새 시나리오 추가)
14. 미정 사항 (기본값으로 구현하고 나중에 바꿀 것)

---

## 1. 개요

| 항목 | 내용 |
|---|---|
| 모드 이름 | Survival 365 |
| mod id | `survival365` |
| 플랫폼 | Minecraft Java Edition, Fabric |
| 대상 버전 | 최신 안정판 26.x (비난독화). 작업 시작 시 fabricmc.net/develop 에서 확인 후 고정 |
| 동작 범위 | 싱글플레이(통합 서버) + 멀티플레이 전용 서버 |
| 필수 의존성 | Fabric Loader, Fabric API |

**핵심 기능**
1. 월드 생성 화면에 **"365일 생존 난이도"** 토글. 켜면:
   - 인게임 좌측 상단에 `D-365` 표시, 하루가 지날 때마다 1씩 감소.
   - 난이도는 **어려움으로 고정** (변경 불가).
   - 하드코어는 **플레이어가 자유롭게** 선택 (고정 아님).
   - **"세계" 선택란**이 새로 나타남. 현재 선택지는 **"바그다드 마크"** 하나뿐 (앞으로 추가 예정).
2. **바그다드 마크** 시나리오: 항상 밤, 바다 없는 끝없는 사막, 보스바 10분 타이머 → TNT 폭격 2분 → 습격 몹 스폰 1분 → 정비 1분 → 반복.

---

## 2. 기술 스택과 프로젝트 구조

### 2.1 빌드
- Gradle + Fabric Loom (`net.fabricmc.fabric-loom` 플러그인, mappings 없음)
- Loom **split source sets** 사용: `src/main` (공통/서버), `src/client` (클라이언트 전용)
- `gradle.properties`에 `minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`, `mod_version` 고정
- 시작점: Fabric 공식 템플릿 생성기(https://fabricmc.net/develop/template/)와 같은 구조로 만든다. (mod id `survival365`, 패키지 `com.junseok.survival365`, split sources 켬, Kotlin 아님)

### 2.2 디렉터리 구조 (목표)

```
survival365/
├─ CLAUDE.md
├─ build.gradle
├─ settings.gradle
├─ gradle.properties
├─ docs/
│  ├─ SPEC.md            ← 이 문서
│  ├─ DECISIONS.md       ← 구현 중 내린 판단 기록 (Claude Code가 작성)
│  └─ EXTENDING.md       ← 새 시나리오 추가 방법 (Claude Code가 작성)
└─ src/
   ├─ main/
   │  ├─ java/com/junseok/survival365/
   │  │  ├─ Survival365.java                 # ModInitializer: 등록 진입점
   │  │  ├─ core/
   │  │  │  ├─ ChallengeOptions.java         # 월드 생성 시 선택값 (enabled, scenarioId)
   │  │  │  ├─ ChallengeState.java           # SavedData: 월드에 저장되는 진행 상태
   │  │  │  ├─ ChallengeManager.java         # 서버 시작/틱/플레이어 접속 이벤트 연결
   │  │  │  ├─ DayClock.java                 # D-day 계산 (시나리오별 방식 선택)
   │  │  │  ├─ RuleEnforcer.java             # 난이도 고정, 고정 시간 유지 등 규칙 강제
   │  │  │  └─ PendingChallengeOptions.java  # 생성 화면 → 통합 서버 전달용 임시 보관
   │  │  ├─ scenario/
   │  │  │  ├─ WorldScenario.java            # 시나리오 인터페이스
   │  │  │  ├─ Scenarios.java                # 시나리오 레지스트리 (id → 인스턴스)
   │  │  │  └─ baghdad/
   │  │  │     ├─ BaghdadScenario.java
   │  │  │     ├─ BaghdadCycle.java          # 페이즈 상태 기계
   │  │  │     ├─ BaghdadPhase.java          # enum: CALM, BOMBARDMENT, RAID, AFTERMATH
   │  │  │     ├─ TntBombardment.java        # TNT 낙하 로직
   │  │  │     ├─ RaidSpawner.java           # 습격 몹 스폰 로직
   │  │  │     └─ BaghdadConfig.java
   │  │  ├─ config/
   │  │  │  └─ ModConfig.java                # config/survival365.json 로드/저장 (Gson)
   │  │  ├─ network/
   │  │  │  └─ CountdownSyncPayload.java     # S2C: D-day 표시용 데이터
   │  │  ├─ command/
   │  │  │  └─ Survival365Command.java       # /s365 ...
   │  │  └─ mixin/                           # 공통 Mixin (필요할 때만)
   │  └─ resources/
   │     ├─ fabric.mod.json
   │     ├─ survival365.mixins.json
   │     ├─ assets/survival365/
   │     │  ├─ lang/ko_kr.json
   │     │  ├─ lang/en_us.json
   │     │  └─ icon.png
   │     └─ data/survival365/
   │        └─ worldgen/
   │           ├─ world_preset/baghdad.json
   │           └─ noise_settings/baghdad.json
   └─ client/
      ├─ java/com/junseok/survival365/client/
      │  ├─ Survival365Client.java           # ClientModInitializer
      │  ├─ hud/CountdownHud.java            # 좌측 상단 D-day 렌더링
      │  ├─ ClientCountdownState.java        # 서버에서 받은 값 보관
      │  └─ mixin/
      │     ├─ CreateWorldScreenMixin.java   # 생성 화면 버튼 추가/잠금
      │     └─ (필요 시 탭 관련 Mixin)
      └─ resources/
         └─ survival365.client.mixins.json
```

### 2.3 설계 원칙
- **게임 로직은 서버에서만.** 클라이언트는 ① 생성 화면 UI ② HUD 표시만 담당.
- **시나리오는 플러그인처럼.** 새 시나리오 = `WorldScenario` 구현 클래스 1개 + 레지스트리 등록 1줄 + 번역 키 + (필요 시) 데이터팩 JSON. 코어 코드는 건드리지 않아도 되게 한다.
- **모든 수치는 설정값.** §9 참고.
- **상태는 월드에 저장.** 저장 후 다시 열어도 D-day와 타이머가 이어져야 한다.

---

## 3. 핵심 개념

### 3.1 Challenge (365일 생존 난이도)
월드 단위 모드. 켜져 있으면 공통 규칙(§5)이 적용되고, 선택된 시나리오 1개가 동작한다.

### 3.2 WorldScenario 인터페이스 (확장 핵심)

```java
public interface WorldScenario {
    ResourceLocation id();                       // 예: survival365:baghdad  (26.x에서 이름이 Identifier 등으로 바뀌었으면 그에 맞춤)
    Component displayName();                     // 번역 키 scenario.survival365.baghdad
    Component description();                     // 생성 화면 툴팁용

    /** 이 시나리오가 강제하는 월드 프리셋. null이면 플레이어가 고른 월드 유형 유지 */
    @Nullable ResourceKey<WorldPreset> worldPreset();

    /** 시간 고정값 (틱). 비어 있으면 일반 낮/밤 순환 */
    OptionalLong fixedDayTime();

    /** 새 월드에서 처음 1회 */
    void onChallengeStart(MinecraftServer server, ChallengeState state);

    /** 매 서버 틱 (오버월드 기준 1회) */
    void tick(MinecraftServer server, ChallengeState state);

    /** 플레이어 접속 시 (보스바 추가 등) */
    void onPlayerJoin(ServerPlayer player, ChallengeState state);
    void onPlayerLeave(ServerPlayer player, ChallengeState state);

    /** 시나리오 전용 저장 데이터 */
    CompoundTag save();
    void load(CompoundTag tag);
}
```
- `Scenarios` 레지스트리는 등록 순서를 유지하는 `LinkedHashMap<ResourceLocation, Supplier<WorldScenario>>`.
- 생성 화면의 "세계" 버튼은 이 레지스트리 목록을 그대로 순환한다 → 시나리오를 등록만 하면 UI에 자동으로 뜬다.
- 세부 시그니처는 실제 API에 맞춰 조정해도 된다 (변경 시 DECISIONS.md 기록).

### 3.3 DayClock (D-day 계산)
- `남은 일수 = 365 - 경과일`
- **경과일 계산 방식은 시나리오에 따라 다르다:**
  - 일반 시간 흐름(`fixedDayTime` 없음): `(level.getDayTime() - 시작 dayTime) / 24000`. 잠자기로 밤을 넘기면 하루가 지난다.
  - 시간 고정 시나리오(바그다드처럼 항상 밤): 모드가 직접 세는 `elapsedTicks / 24000`. 서버가 실제로 돈 틱만 센다 (싱글플레이 일시정지 중에는 증가하지 않음). → 실제 20분 = 하루.
- `elapsedTicks`는 두 방식 모두 항상 누적 저장한다 (통계·확장용).
- 표시: 남은 일수 > 0이면 `D-n`, 0이면 `D-DAY`. (그 이후 처리는 §14 미정 사항)

---

## 4. 월드 생성 화면 (UI)

**구현 위치:** `src/client/.../mixin/CreateWorldScreenMixin.java` (+ 필요하면 `WorldCreationUiState`에 필드를 추가하는 Mixin/덕 인터페이스)

> 26.x의 `CreateWorldScreen`은 탭(게임 / 월드 / 추가 사항) 구조다. 실제 소스에서 "게임" 탭의 레이아웃 생성 지점을 확인하고 그 안에 버튼을 끼워 넣는다.

### 4.1 추가되는 요소 ("게임" 탭, 난이도 버튼 바로 아래)

| 요소 | 종류 | 동작 |
|---|---|---|
| **365일 생존 난이도** | `CycleButton<Boolean>` (끔/켬) | 기본값 끔 |
| **세계** | `CycleButton<WorldScenario>` | 365일 생존 난이도가 **켜졌을 때만 보임**. 선택지 = `Scenarios` 레지스트리 전체. 현재는 "바그다드 마크" 하나 (기본 선택) |

### 4.2 토글 켰을 때 다른 버튼에 미치는 영향
- **난이도 버튼**: 값을 `어려움`으로 바꾸고 비활성화(회색). 툴팁: "365일 생존 난이도에서는 어려움으로 고정됩니다".
- **게임 모드 버튼**(생존/하드코어/창조): **건드리지 않는다.** 하드코어는 자유.
  - 단, 창조 모드 선택은 막지 않는다 (테스트 편의). → §14 미정 사항.
- **월드 유형 버튼("월드" 탭)**: 선택된 시나리오가 `worldPreset()`을 가지면 그 프리셋으로 바꾸고 비활성화. 바그다드 → `survival365:baghdad`.
- 토글을 다시 끄면: 난이도·월드 유형 버튼을 **원래 값으로 복구**하고 다시 활성화.

### 4.3 선택값을 서버로 넘기는 방법
1. "새로운 월드 생성" 버튼을 누르는 순간 `ChallengeOptions(enabled, scenarioId)`를 `PendingChallengeOptions`(static 보관소)에 넣는다.
2. 통합 서버가 시작될 때(`ServerLifecycleEvents.SERVER_STARTED`) — 이 월드에 `ChallengeState`가 아직 없으면(=새 월드) pending 값을 읽어 `ChallengeState`를 만들고 pending을 비운다.
3. 생성 화면을 취소하고 나가면 pending을 비운다.
4. 이미 존재하는 월드를 열 때는 pending을 무시한다.

> 대안: 바닐라 게임 규칙(GameRule)에 실어 보내는 방식도 가능하나, 26.x에서 게임 규칙 API가 바뀌었을 수 있으므로 기본은 위 방식. 더 깔끔한 공식 경로가 있으면 그것을 쓰고 DECISIONS.md에 기록.

### 4.4 번역 키 (예시)
```json
"selectWorld.survival365.challenge": "365일 생존 난이도",
"selectWorld.survival365.challenge.info": "365일을 버텨라. 난이도는 어려움으로 고정됩니다.",
"selectWorld.survival365.scenario": "세계",
"selectWorld.survival365.difficulty_locked": "365일 생존 난이도에서는 어려움으로 고정됩니다",
"scenario.survival365.baghdad": "바그다드 마크",
"scenario.survival365.baghdad.desc": "끝없는 사막, 끝나지 않는 밤, 그리고 하늘에서 떨어지는 TNT."
```
영어(`en_us.json`)도 같은 키로 작성: "365-Day Survival", "World", "Baghdad Craft" 등.

---

## 5. 365일 생존 난이도 공통 규칙
`ChallengeState.enabled == true`인 월드에서 항상 적용 (`RuleEnforcer`).

1. **난이도 어려움 고정**
   - 서버 시작 시 `setDifficulty(HARD, true)` + `setDifficultyLocked(true)`.
   - 이후에도 다른 값이 되면(명령어 등) 즉시 HARD로 되돌린다 (20틱마다 확인이면 충분).
2. **하드코어**: 손대지 않는다.
3. **D-day 카운트다운**: §6.
4. **시나리오 실행**: 선택된 시나리오의 `tick()`을 매 서버 틱 호출.
5. **고정 시간 유지**: 시나리오가 `fixedDayTime`을 가지면
   - 시작 시 게임 규칙 `doDaylightCycle`(26.x 이름 확인) = false, 시간 = 고정값.
   - 20틱마다 시간이 고정값과 다르면 되돌린다.

---

## 6. D-day 카운트다운 HUD

**구현 위치:** `src/client/.../hud/CountdownHud.java`, Fabric API의 HUD 등록 API(현재 버전 기준 `HudElementRegistry` 계열 — 실제 이름 확인)

| 항목 | 값 |
|---|---|
| 위치 | 화면 좌측 상단, (6, 6) 픽셀 |
| 텍스트 | `D-365` … `D-1`, `D-DAY` |
| 크기 | 기본 글자의 2배 (`pose` 스케일 2.0) |
| 색 | 흰색 + 그림자. 남은 일수 ≤ 30이면 빨간색(#FF5555) |
| 숨김 | F1(HUD 숨김) 시 숨김. 디버그 화면(F3) 열렸을 때 숨김. 챌린지 꺼진 월드에서는 표시 안 함 |
| 갱신 | 서버가 보내는 `CountdownSyncPayload` 값만 사용 (클라이언트 계산 금지) |

- 보스바(화면 상단 중앙)와 겹치지 않는다.
- 하루가 바뀌는 순간: 서버가 모든 플레이어에게 액션바 메시지 `"D-364"`(번역 키 `hud.survival365.day_passed`)와 짧은 효과음을 보낸다 (선택: `config`에서 끌 수 있음).

---

## 7. 시나리오: 바그다드 마크

- id: `survival365:baghdad`
- 적용 차원: **오버월드만**. 네더/엔드에 있는 플레이어는 폭격·습격 대상에서 제외 (보스바는 모든 플레이어에게 보이되, 다른 차원에서는 그대로 시간만 흐름).
- 대상 플레이어: 생존/모험 모드 플레이어. 관전자·창조 모드는 제외.

### 7.1 지형: 끝없는 사막, 바다 없음
**구현:** 데이터팩 JSON (모드 리소스 안에 포함, 별도 설치 불필요)

`src/main/resources/data/survival365/worldgen/world_preset/baghdad.json`
- 오버월드: `noise` 생성기
  - `biome_source`: `{"type": "minecraft:fixed", "biome": "minecraft:desert"}` → 전부 사막
  - `settings`: `"survival365:baghdad"`
- 네더·엔드: 바닐라 그대로.

`src/main/resources/data/survival365/worldgen/noise_settings/baghdad.json`
- **바닐라 `minecraft:overworld` noise_settings를 그 버전의 원본에서 복사**해서 시작한다. (원본은 `./gradlew genSources` 후 클라이언트 jar의 `data/minecraft/worldgen/noise_settings/overworld.json`, 또는 바닐라 데이터 생성기 출력에서 얻는다. 직접 손으로 쓰지 말 것.)
- 수정할 값:
  - `"sea_level": -64` → 바닷물이 생기지 않음
  - `"aquifers_enabled": false` → 지하 대수층 물 제거
  - 나머지는 원본 유지.
- 결과: 바다 자리는 마른 분지(사막 계곡)가 된다. 사막 마을·사막 신전 등 사막 구조물은 그대로 생성된다.
- 이 프리셋은 일반 "월드 유형" 목록에는 **노출하지 않는다** (챌린지에서만 선택). 일반 목록에 넣는 태그(`minecraft:normal` world_preset 태그)에 추가하지 말 것.

### 7.2 항상 밤
- `fixedDayTime = 18000` (자정).
- §5-5의 규칙으로 유지. 잠을 자도 아침이 오지 않는다 (doDaylightCycle=false면 바닐라도 시간 건너뛰지 않음 — 실제 동작 확인).
- D-day는 §3.3의 `elapsedTicks` 방식 사용 → **실제 플레이 20분마다 하루 감소**.

### 7.3 사이클 (페이즈 상태 기계)

```
CALM (10분) → BOMBARDMENT (2분) → RAID (1분) → AFTERMATH (1분) → CALM …
```

| 페이즈 | 길이 | 보스바 텍스트 | 보스바 색 | 하는 일 |
|---|---|---|---|---|
| `CALM` | 600초 | `폭격까지 09:59` | 노랑 | 없음 (대기) |
| `BOMBARDMENT` | 120초 | `폭격 중! 01:59` | 빨강 | TNT 낙하 (§7.4) |
| `RAID` | 60초 | `습격! 00:59` | 보라 | 습격 몹 스폰 (§7.5) |
| `AFTERMATH` | 60초 | `정비 00:59` | 초록 | 스폰 없음. 이미 나온 몹은 남아 있음 |

- 보스바: `ServerBossEvent` 1개를 시나리오가 소유. 진행도 = 남은 시간 / 페이즈 길이 (줄어드는 막대).
- 텍스트는 번역 키 + 인자 (`bossbar.survival365.baghdad.calm`, `%s`에 `mm:ss`). 텍스트 갱신은 20틱(1초)마다.
- 플레이어 접속 시 보스바에 추가, 나갈 때 제거.
- 페이즈와 남은 틱은 `ChallengeState`의 시나리오 데이터에 저장 → 저장 후 재접속해도 이어짐.
- 첫 시작: 월드 생성 직후 `CALM` 10분부터.
- 페이즈 전환 시 효과음:
  - CALM → BOMBARDMENT: 경고음 (예: `BELL` 또는 `WITHER_SPAWN` 작게) + 타이틀 "폭격 시작"
  - BOMBARDMENT → RAID: `RAID_HORN` (습격 나팔) + 타이틀 "습격"
  - 사운드 종류는 설정에서 바꿀 수 있게.

### 7.4 TNT 폭격 (BOMBARDMENT, 2분)
**해석(기본값):** 20초마다 1회 "파도", 파도 하나 = 플레이어 머리 위 **3×3 격자로 TNT 9개, 4칸 간격**.
→ 2분 동안 페이즈 시작 0, 20, 40, 60, 80, 100초 시점에 총 6번. (모두 설정값)

**위치 계산 (플레이어마다, 파도마다)**
1. 기준점 = 그 순간 플레이어의 x, z (블록 좌표).
2. 격자 오프셋 dx, dz ∈ {-4, 0, +4} → 9칸.
3. 각 칸 (x+dx, z+dz)에서 지표 높이 `surfaceY` = 고도맵(`MOTION_BLOCKING_NO_LEAVES`) 값.
   - **플레이어 위치가 아니라 지표 기준**이다. → 땅굴을 파고 내려간 플레이어는 지표에서 터지므로 한동안 안전. 하지만 매 파도마다 지표가 파여 점점 깊어진다 ("한동안 버틸 수 있음").
4. 소환 높이 `spawnY = min(surfaceY + dropHeight, 월드최대높이 - 2)`, 기본 `dropHeight = 40`.
5. `PrimedTnt` 엔티티를 소환 (점화된 상태로 낙하).

**퓨즈(폭발까지 틱) 계산 — 중요**
- 기본 퓨즈(80틱)로 40칸 위에서 떨어뜨리면 공중에서 터질 수 있다.
- 낙하 시간을 시뮬레이션해서 퓨즈를 설정: 매 틱 `vy -= 0.04; y += vy; vy *= 0.98` (실제 바닐라 `PrimedTnt` 물리 상수를 소스에서 확인해 같은 값 사용) 를 `y ≤ surfaceY + 1`이 될 때까지 반복한 틱 수 = `fallTicks`.
- `fuse = fallTicks + fuseAfterLanding` (기본 `fuseAfterLanding = 10`) → 착지 후 0.5초 뒤 폭발.
- 폭발 위력은 바닐라 TNT 그대로 (지형 파괴 O).

**기타**
- 플레이어가 서로 가까이 있으면 TNT가 겹칠 수 있다. 같은 파도에서 이미 TNT를 놓은 칸(x, z)은 건너뛴다 (중복 제거).
- 서버 보호: 한 파도의 TNT 총합 상한 `maxTntPerWave` (기본 90).
- TNT를 놓을 위치의 청크가 로드되어 있지 않으면 그 칸은 건너뛴다.
- 소환된 TNT의 "점화한 엔티티(owner)"는 비워 둔다.

### 7.5 습격 몹 스폰 (RAID, 1분)
"습격 효과처럼" 주변에 약탈자 무리가 1분 동안 계속 나타난다. 바닐라 `Raid` 시스템(마을 필요)은 쓰지 않고 직접 스폰한다.

| 몹 | 엔티티 | 기본 가중치 |
|---|---|---|
| 약탈자 | `PILLAGER` | 40 |
| 변명자 | `VINDICATOR` | 30 |
| 마녀 | `WITCH` | 12 |
| 소환사 | `EVOKER` | 8 |
| 파괴수 | `RAVAGER` | 5 |

**스폰 규칙 (플레이어마다)**
- 간격: `raidSpawnIntervalTicks` = 100틱(5초)마다 1회 → 1분에 12회.
- 1회당 `raidGroupSize` = 2마리, 가중치 랜덤.
- 위치: 플레이어로부터 수평 거리 16~32칸의 랜덤 지점 → 그 지점의 지표 높이. 발밑이 단단하고 머리 위 2칸(파괴수는 3칸×3칸)이 비어 있으며 액체가 아닌 곳. 최대 10번 재시도, 실패하면 그 회차는 건너뜀.
- 스폰 직후:
  - `finalizeSpawn`(바닐라 초기화, 장비 지급 포함) 호출. 스폰 사유는 이벤트/명령 계열.
  - 가장 가까운 대상 플레이어를 공격 대상으로 지정.
  - 엔티티 태그 `survival365_raider` 부여 (정리 명령어용).
  - 습격 대장(깃발 든 약탈자)으로 만들지 않는다 (흉조 효과 방지).
- 상한: 플레이어 반경 64칸 안에 `survival365_raider` 태그 몹이 `maxRaidMobsPerPlayer`(기본 30) 이상이면 그 회차는 스폰 생략.
- 옵션 `ravagerRiderChance` (기본 0.3): 파괴수 스폰 시 약탈자를 태울 확률.
- 디스폰: 바닐라 규칙 따름 (따로 영구 지정 안 함). 설정 `raidersPersistent`로 바꿀 수 있게.

---

## 8. 데이터 저장 · 네트워크 동기화

### 8.1 ChallengeState (SavedData, 오버월드 데이터 저장소)
26.x의 `SavedData` 등록 방식(코덱 기반 `SavedDataType` 등)을 실제 소스로 확인 후 구현. 파일명 `survival365_challenge`.

| 필드 | 타입 | 설명 |
|---|---|---|
| `enabled` | boolean | 챌린지 활성 |
| `scenarioId` | String | 예: `survival365:baghdad` |
| `startDayTime` | long | 시작 시점 dayTime |
| `elapsedTicks` | long | 챌린지 시작 후 서버 틱 누적 |
| `lastAnnouncedDay` | int | 하루 지남 알림 중복 방지 |
| `completed` | boolean | D-DAY 도달 처리 여부 |
| `formatVersion` | int | 저장 형식 버전 (현재 1). 나중에 마이그레이션용 |
| `scenarioData` | CompoundTag | 시나리오 전용 (바그다드: phase, phaseTicksLeft, waveIndex, spawnTimer) |

- 변경 시 `setDirty()`.
- 존재하지 않는 시나리오 id가 저장돼 있으면 (모드 버전 변경 등) 로그 경고 후 챌린지는 유지하되 시나리오 tick은 건너뛴다. 크래시 금지.

### 8.2 CountdownSyncPayload (S2C)
| 필드 | 타입 |
|---|---|
| `enabled` | boolean |
| `daysLeft` | int |
| `scenarioId` | String |

- Fabric 네트워킹 (`PayloadTypeRegistry` + `ServerPlayNetworking`, `CustomPacketPayload` + `StreamCodec`). 실제 API 확인.
- 보내는 시점: 플레이어 접속 시, 차원 이동/리스폰 시, 하루가 바뀔 때.
- 클라이언트는 받은 값을 `ClientCountdownState`에 저장하고, 서버 접속 해제 시 초기화.
- 이 모드가 없는 클라이언트가 서버에 접속하는 경우는 지원하지 않는다 (fabric.mod.json `environment: "*"`).

---

## 9. 설정 파일
`config/survival365.json` — 서버 시작 시 로드, 없으면 기본값으로 생성. Gson 사용, 주석 대신 키 이름을 명확하게.

```json
{
  "countdown": {
    "totalDays": 365,
    "warningDays": 30,
    "announceDayChange": true
  },
  "baghdad": {
    "calmSeconds": 600,
    "bombardmentSeconds": 120,
    "raidSeconds": 60,
    "aftermathSeconds": 60,
    "fixedDayTime": 18000,

    "tntWaveIntervalSeconds": 20,
    "tntGridSize": 3,
    "tntSpacing": 4,
    "tntDropHeight": 40,
    "tntFuseAfterLanding": 10,
    "maxTntPerWave": 90,

    "raidSpawnIntervalTicks": 100,
    "raidGroupSize": 2,
    "raidMinDistance": 16,
    "raidMaxDistance": 32,
    "maxRaidMobsPerPlayer": 30,
    "ravagerRiderChance": 0.3,
    "raidersPersistent": false,
    "raidWeights": {
      "minecraft:pillager": 40,
      "minecraft:vindicator": 30,
      "minecraft:witch": 12,
      "minecraft:evoker": 8,
      "minecraft:ravager": 5
    }
  }
}
```
- 시나리오별 설정은 시나리오 id 이름의 섹션으로 둔다 → 새 시나리오는 새 섹션 추가.
- 잘못된 값(음수 등)은 기본값으로 대체하고 경고 로그.

---

## 10. 디버그/관리 명령어
`/s365` — 권한 레벨 2 (치트 허용/OP). 테스트를 빠르게 하기 위해 반드시 구현.

| 명령 | 동작 |
|---|---|
| `/s365 status` | 챌린지 켜짐 여부, 시나리오, 남은 일수, 현재 페이즈와 남은 시간 출력 |
| `/s365 day set <남은일수>` | D-day 강제 설정 (elapsedTicks 조정) |
| `/s365 phase <calm\|bombardment\|raid\|aftermath>` | 바그다드 페이즈 즉시 전환 (해당 페이즈 처음부터) |
| `/s365 skip <초>` | 현재 페이즈 남은 시간을 그만큼 줄임 |
| `/s365 cleanup` | `survival365_raider` 태그 몹 전부 제거 |
| `/s365 reload` | 설정 파일 다시 읽기 |

- 시나리오 전용 하위 명령은 시나리오가 직접 등록할 수 있게 한다 (예: `WorldScenario.registerCommands(builder)` 기본 구현 비어 있음).

---

## 11. 전용 서버 지원
전용 서버에는 월드 생성 화면이 없으므로 설정으로 대신한다.

1. `server.properties`:
   ```
   level-type=survival365\:baghdad
   difficulty=hard
   hardcore=false   # 원하면 true
   ```
2. `config/survival365-server.json` (없으면 기본값으로 생성):
   ```json
   { "enableOnNewWorld": true, "scenario": "survival365:baghdad" }
   ```
3. 서버가 **새 월드**를 만들 때(ChallengeState 없음) 이 값으로 ChallengeState 생성. 기존 월드에는 영향 없음.

---

## 12. 수동 테스트 확인 목록
`./gradlew runClient`로 확인. Claude Code는 구현 후 이 목록을 기준으로 코드 경로를 점검한다.

**생성 화면**
- [ ] "게임" 탭에 "365일 생존 난이도" 버튼이 보이고 기본값이 "끔"이다.
- [ ] 켜면 난이도가 "어려움"으로 바뀌고 회색으로 잠긴다.
- [ ] 켜면 "세계: 바그다드 마크" 버튼이 나타난다. 끄면 사라진다.
- [ ] 켜면 "월드" 탭의 월드 유형이 바그다드로 바뀌고 잠긴다. 끄면 원래 값으로 돌아온다.
- [ ] 하드코어 선택은 켜든 끄든 자유롭다.
- [ ] 끈 상태로 만든 월드는 바닐라와 완전히 같다 (HUD 없음, 보스바 없음).

**인게임 공통**
- [ ] 좌측 상단에 `D-365`가 보인다.
- [ ] `/s365 day set 1` → `D-1`, 하루가 지나면 `D-DAY`.
- [ ] `/difficulty easy` 해도 어려움으로 돌아온다.
- [ ] 저장 후 나갔다 다시 들어와도 D-day가 유지된다.

**바그다드**
- [ ] 주변이 모두 사막이고 바다가 없다 (멀리 이동해서 확인).
- [ ] 항상 밤이다. 침대에서 자도 아침이 오지 않는다. `/time set day` 해도 곧 밤으로 돌아온다.
- [ ] 상단에 보스바 "폭격까지 10:00"이 보이고 줄어든다.
- [ ] `/s365 phase bombardment` → 20초마다 머리 위 하늘에서 3×3 TNT 9개가 떨어져 지표에서 터진다. 공중 폭발 없음.
- [ ] 10칸 정도 아래로 굴을 파고 들어가면 첫 파도에 피해를 받지 않는다.
- [ ] 2분 후 습격 페이즈로 넘어가며 나팔 소리가 나고, 주변에 약탈자·변명자·마녀·소환사·파괴수가 1분간 나타나 플레이어를 공격한다.
- [ ] 그 후 1분 정비, 다시 10분 타이머.
- [ ] 저장 후 다시 들어와도 페이즈와 남은 시간이 이어진다.
- [ ] 네더에 가 있으면 폭격·습격이 오지 않는다.
- [ ] D-day가 실제 플레이 20분마다 1 감소한다.

**서버**
- [ ] `./gradlew runServer`가 크래시 없이 뜬다 (클라이언트 클래스 로드 오류 없음).

---

## 13. 확장 가이드 (요약 — 상세는 `docs/EXTENDING.md`로 작성)
새 세계(시나리오) 추가 절차:
1. `scenario/<이름>/<이름>Scenario.java` — `WorldScenario` 구현.
2. `Scenarios`에 한 줄 등록 → 생성 화면 "세계" 버튼에 자동 표시.
3. `ko_kr.json`/`en_us.json`에 `scenario.survival365.<이름>`, `.desc` 추가.
4. 지형이 다르면 `data/survival365/worldgen/world_preset/<이름>.json` (+ noise_settings) 추가하고 `worldPreset()`에서 반환.
5. 수치는 `config/survival365.json`의 `<이름>` 섹션으로.
6. 공통 규칙(D-day, 난이도 고정)은 코어가 처리하므로 시나리오는 자기 고유 기능만 구현.

이후 코어 기능 확장(예: 일수별 난이도 상승, 업적, 상점)은 `core/` 또는 새 `feature/` 패키지에 두고, 시나리오가 필요 시 훅으로 사용하게 한다.

---

## 14. 미정 사항 (기본값으로 구현, 나중에 바꿀 것)
| 항목 | 기본 구현 |
|---|---|
| D-DAY 도달 후 | 모든 플레이어에게 타이틀 "생존 성공!" + 도전과제 느낌의 효과음 1회. 게임은 계속되고 HUD는 `D-DAY` 유지. `completed=true`. 바그다드 사이클은 계속 돈다 |
| 창조 모드로 챌린지 월드 생성 | 허용 (테스트용). 창조/관전 플레이어는 폭격·습격 대상 제외 |
| 플레이어가 모두 죽어 있는 동안 | 타이머는 계속 흐름 |
| 멀티플레이에서 D-day 기준 | 서버 전체 공용 1개 |
| 팬텀 | 바닐라 그대로 (항상 밤이라 수면 불가 시 팬텀이 생길 수 있음). 문제가 되면 나중에 조정 |
