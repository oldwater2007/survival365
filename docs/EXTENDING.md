# 새 세계(시나리오) 추가하기

코어(D-day, 난이도 고정, 시간 고정, 저장, HUD, 생성 화면)는 건드리지 않는다. 시나리오는 자기 고유 기능만 구현한다.
아래는 예시로 `frozen`(얼어붙은 세계) 시나리오를 추가하는 과정이다.

## 1. 시나리오 클래스
`src/main/java/com/junseok/survival365/scenario/frozen/FrozenScenario.java`

```java
public final class FrozenScenario implements WorldScenario {
	public static final Identifier ID = Survival365.id("frozen");

	@Override public Identifier id() { return ID; }

	// 선택: 지형을 바꾸면 프리셋 키를 반환한다. null이면 플레이어가 고른 월드 유형을 그대로 쓴다.
	@Override public ResourceKey<WorldPreset> worldPreset() {
		return ResourceKey.create(Registries.WORLD_PRESET, ID);
	}

	// 선택: 시간을 고정하면 D-day가 "실제 플레이 20분 = 하루"로 계산된다.
	// @Override public OptionalLong fixedDayTime() { return OptionalLong.of(6000); }

	@Override public void tick(MinecraftServer server, ChallengeState state) { /* 매 서버 틱 */ }

	@Override public CompoundTag save() { return new CompoundTag(); }
	@Override public void load(CompoundTag tag) { }
}
```

- 생성자는 가볍게 유지한다. 생성 화면이 표시 이름을 얻으려고 클라이언트에서도 인스턴스를 만든다.
- 보스바처럼 서버에 붙는 자원은 처음 쓸 때 만든다(`BaghdadScenario.bossBar()` 참고). `onServerStopped`에서 정리한다.
- 대상 플레이어를 고를 때는 바그다드처럼 차원과 게임 모드를 직접 거른다.
- 쓸 수 있는 훅: `onChallengeStart`, `tick`, `onPlayerJoin`/`onPlayerLeave`, `onServerStopped`, `appendStatus`(`/s365 status`에 줄 추가), `registerCommands`(`/s365` 하위 명령).

## 2. 레지스트리에 한 줄 등록
`scenario/Scenarios.java`

```java
public static final Identifier FROZEN = register(FrozenScenario.ID, FrozenScenario::new);
```

등록 순서가 생성 화면 "세계" 버튼의 순환 순서다. 따로 UI 코드를 고칠 필요는 없다.

## 3. 번역 키 (ko_kr.json, en_us.json 둘 다)
```json
"scenario.survival365.frozen": "얼어붙은 세계",
"scenario.survival365.frozen.desc": "툴팁 설명",
"generator.survival365.frozen": "얼어붙은 세계"
```
프리셋이 있으면 `generator.survival365.<이름>` 키도 넣는다. 월드 탭의 월드 유형 버튼에 이 이름이 표시된다.

## 4. 지형 (선택)
- `src/main/resources/data/survival365/worldgen/world_preset/frozen.json`
- 필요하면 `.../worldgen/noise_settings/frozen.json`도 추가한다.
- noise_settings는 손으로 쓰지 않는다. `./gradlew genSources` 후 Minecraft jar의 `data/minecraft/worldgen/noise_settings/overworld.json`을 복사해서 고친다. 26.3 형식에서는 `aquifers` 객체를 빼면 대수층이 꺼진다.
- 일반 월드 유형 목록에 보이지 않게 하려면 `minecraft:normal` world_preset 태그에 넣지 않는다.

## 5. 설정값
`config/ModConfig.java`에 `public FrozenConfig frozen = new FrozenConfig();` 필드를 추가하면 `config/survival365.json`에 `"frozen"` 섹션이 생긴다.
`BaghdadConfig`처럼 `validate()`를 만들고 `ModConfig.load()`에서 호출한다. 수치는 하드코딩하지 않는다.

## 6. 명령어 (선택)
`registerCommands(root)`에서 `root.then(...)`으로 하위 명령을 붙인다.
실행할 때는 `ChallengeManager.activeScenario() instanceof FrozenScenario`인지 확인한다. 이 메서드는 등록된 모든 시나리오에 대해 서버 시작 시 한 번씩 호출된다.
두 시나리오가 같은 이름의 하위 명령을 등록하면 Brigadier가 노드를 합치고, 나중에 등록한 쪽의 실행부가 쓰인다. 그러니 이름이 겹치지 않게 한다.

## 7. 확인
- `./gradlew build`
- `./gradlew runClientGameTest` — 생성 화면과 바그다드 흐름의 자동 검사. 새 시나리오의 검사는 `src/gametest/.../Survival365ClientGameTest.java`에 추가한다.
- `./gradlew runServer` — 전용 서버. `config/survival365-server.json`의 `scenario`를 새 id로 바꾸고, `server.properties`의 `level-type`을 새 프리셋으로 설정해서 확인한다.
