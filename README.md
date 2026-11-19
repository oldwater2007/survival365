# Survival 365

마인크래프트 Java Edition용 Fabric 모드입니다. 월드를 만들 때 **365일 생존 난이도**를 켜면 D-365 카운트다운이 시작되고, 플레이할 "세계"(시나리오)를 고를 수 있습니다.

첫 시나리오는 **바그다드 마크**입니다. 항상 밤이고, 바다 없는 사막이 끝없이 이어지며, 주기적으로 TNT 폭격과 습격이 옵니다.

## 설치
1. Minecraft Java **26.3**에 [Fabric Loader](https://fabricmc.net/use/) 0.20.1 이상을 설치합니다.
2. [Fabric API](https://modrinth.com/mod/fabric-api)(26.3용)와 [Releases](../../releases/latest)에서 받은 `survival365-x.y.z.jar`를 `.minecraft/mods` 폴더에 넣습니다.
3. 멀티플레이 서버라면 서버와 모든 클라이언트에 같은 버전을 넣습니다. 서버는 `server.properties`에 `level-type=survival365\:baghdad`를 넣고 새 월드로 시작합니다.

새 버전이 나오면 월드에 들어갈 때 채팅창에 다운로드 링크가 표시됩니다. 이 알림은 `config/survival365.json`의 `updates.checkForUpdates`로 끌 수 있습니다.

## 개발
```
./gradlew build              # build/libs/survival365-<버전>.jar
./gradlew runClient          # 개발용 클라이언트
./gradlew runServer          # 개발용 전용 서버
./gradlew runClientGameTest  # 생성 화면과 바그다드 흐름 자동 테스트
```
- 명세: [docs/SPEC.md](docs/SPEC.md)
- 구현 판단 기록: [docs/DECISIONS.md](docs/DECISIONS.md)
- 새 시나리오 추가 방법: [docs/EXTENDING.md](docs/EXTENDING.md)

## 새 버전 배포
1. `gradle.properties`의 `version`을 올리고 커밋합니다(예: `1.0.1`).
2. 같은 번호의 태그를 붙여 올립니다.
   ```
   git tag v1.0.1
   git push origin main v1.0.1
   ```
3. GitHub Actions가 jar를 빌드해서 Release에 자동으로 첨부합니다. 그러면 플레이어의 게임에 업데이트 알림이 뜹니다.
