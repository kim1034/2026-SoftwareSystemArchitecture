# TimeFit

Software System Design Initial Project · Checkpoint 4 · Beta 0.4.0  
김대원 · 224569 · Personal Schedule Management Program

날짜별 활동량과 시간대 충돌을 확인하고, 덜 바쁜 날짜를 추천하는 Java Swing 개인 일정 관리 프로그램입니다. 유스케이스는 5개입니다. 외부 Java 라이브러리와 인터넷 연결 없이 실행합니다.

## Windows와 VS Code에서 시작

1. JDK 17 이상을 설치합니다. 터미널에서 `java -version`, `javac -version`, `jar --version`, `javadoc --version`이 실행되어야 합니다. JRE만 설치하면 소스 빌드가 안 됩니다.
2. 소스 ZIP의 `timefit` 폴더를 원하는 작업 폴더에 풉니다. 예: `C:\Users\battl\Desktop\전남대학교\3학년\2학기\소시설\개인 프로젝트\timefit`.
3. VS Code에서 그 `timefit` 폴더를 엽니다. Java 실행/디버깅 기능을 사용하려면 Java 확장을 설치합니다. 확장이 없어도 아래 PowerShell 명령으로 빌드하고 실행할 수 있습니다.
4. VS Code의 PowerShell 터미널에서 빌드합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
java -jar .\dist\ssd_ip_cp4_2026f_224569_app.jar
```

VS Code Java 확장이 있으면 `Run TimeFit` 실행 구성을 선택해도 됩니다. 소스는 UTF-8이며 경로에 한글과 공백이 있어도 스크립트가 개별 인수로 처리합니다.

## 검사와 API 문서

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\javadoc.ps1
Start-Process .\api-manual\index.html
```

Linux/macOS: `bash scripts/build.sh`, `bash scripts/test.sh`, `bash scripts/javadoc.sh`.

## 기본 사용 순서

1. 중앙 달력에서 날짜를 클릭하면 오른쪽에 일정, 총시간, 남는 시간을 표시합니다.
2. `+ 일정 추가`를 누르고 제목과 날짜를 입력합니다. 시간은 분 단위, 날짜는 `yyyy-MM-dd`, 시작/종료는 `HH:mm`입니다.
3. 시작과 종료를 모두 입력하면 그 차이를 사용합니다. 모두 비우면 예상 시간, 예상 시간도 비우면 기본 시간을 사용합니다. 시작/종료 중 하나만 입력하면 저장하지 않습니다.
4. 마감일을 넣으면 같은 입력 창에서 추천이 자동 갱신됩니다. 후보 선택은 날짜만 바꾸며 `저장`으로 확정합니다. 추천이 없어도 다른 날짜를 직접 지정할 수 있습니다.
5. 일정 카드의 수정·삭제·상태 버튼으로 관리합니다. 지난 미완료 일정에는 재계획 버튼이 생깁니다.
6. 전체 목록 탭에서 검색과 날짜·종류·중요도·상태 필터를 사용합니다. 달력 점/개수와 일별 합계는 필터와 관계없이 전체 일정 기준입니다.

## 시간과 추천 규칙

- 최초 하루 최대 480분, 기본 120분이며 설정에서 변경합니다.
- 완료 일정도 총시간에 포함합니다. 총시간이 최대와 같으면 허용하고 넘으면 거부합니다.
- 시간이 있는 일반 일정끼리 `candidate.start < existing.end && existing.start < candidate.end`이면 충돌합니다. 서로 맞닿는 시간은 허용합니다.
- 일반과 하루종일은 시간 구분이며 학습·과제·연구 등의 일정 종류와 별개입니다.
- 하루종일은 현재 하루 최대 시간 전체를 사용하고 다른 일정과 공존할 수 없습니다. 최대 시간 변경 시 기존 하루종일도 다시 계산합니다.
- 기본 시간이 최대 시간을 넘고 새 일정의 시간/예상 시간을 모두 생략하면 하루종일로 적용하고 안내합니다. 명시적으로 입력한 일반 시간이 최대를 넘으면 충돌입니다.
- 기본값 변경은 기존 일반 일정에 영향을 주지 않습니다.
- 추천은 오늘부터 미래 마감일 전날까지입니다. 마감일이 오늘이면 오늘만 검사합니다.
- 등록 후 총시간 오름차순 → 동률이면 토·일 → 날짜 오름차순입니다.
- 상위 3개와 더 보기를 제공합니다. 시간이 있는 일정은 오늘 시작 시각이 지났으면 오늘을 제외합니다. 시간이 없거나 하루종일이면 오늘의 총량 기준으로 판단합니다.
- 과거 날짜 직접 기록은 가능하며 추천은 과거를 제외합니다. 재계획은 오늘 이후로 이동하며 같은 ID와 일반 소요 시간/시간 구분을 유지하고, 시작·종료를 비우며 예정 상태로 저장합니다.

## 저장 위치와 실패 처리

기본 데이터는 사용자 홈 아래 `.timefit/data.xml`에 저장합니다. Windows 예시는 `C:\Users\battl\.timefit\data.xml`입니다. 설치/소스 폴더와 분리되어 재빌드해도 유지됩니다.

```powershell
java '-Dtimefit.data=C:\Users\battl\Desktop\timefit-demo\data.xml' -jar .\dist\ssd_ip_cp4_2026f_224569_app.jar
```

임시 파일에 기록하고 flush 후 원자적 교체에 성공해야 메모리와 화면에 확정합니다. 원자적 교체를 지원하지 않는 파일 시스템에서는 저장 오류를 안내하며 기존 파일을 보존합니다. 손상 파일을 읽으면 원본을 덮어쓰지 않고 시작을 중단합니다. 같은 데이터 파일은 한 프로세스만 열 수 있도록 잠급니다.

## 구조

- `timefit.model`: 일정·설정·입력·계산 결과
- `timefit.service`: ScheduleService, PlanningService, SettingsService
- `timefit.storage`: StateStore, FileRepository
- `timefit.ui`: MainFrame, ScheduleDialog, SettingsDialog, Theme
- `src/test/java`: CoreTests, GuiSmokeTests, JarSmokeTests
- `scripts`: Windows PowerShell / POSIX 빌드·검사·Javadoc 스크립트
- `docs`: 요구사항, 클래스 구조, 검증 기록

## 베타 범위와 확인 환경

Java 17, Linux에서 컴파일·자동 검사·실제 Swing 버튼 검사·독립 JAR 시작/재시작을 확인했습니다. Windows PowerShell 스크립트는 제공하지만 Windows에서의 최종 실행은 사용자 PC에서 확인해야 합니다. 화면 캡처는 실제 Swing GUI이며 재현 검사 시계를 2026-11-02 12:00 Asia/Seoul로 고정했습니다. 평소 실행은 사용자 컴퓨터의 시스템 날짜·시간대를 사용합니다.

한 날짜 안에서 끝나는 일정만 지원합니다. 반복 일정·자정 넘김·자동 시간대 배치는 지원하지 않습니다. 입력은 제목 200자 이내, 분 단위이며, 베타의 추천 검색 기간은 오늘부터 3660일 이내입니다. 더 먼 마감일은 오류로 안내하며 직접 날짜 등록은 가능합니다. 여러 컴퓨터의 동시 사용이나 클라우드 동기화는 지원하지 않습니다.

## 제출 파일

- `ssd_ip_cp4_2026f_224569_src.zip`: 소스와 빌드 설정, 문서
- `ssd_ip_cp4_2026f_224569_app.jar`: 실행 파일
- `ssd_ip_cp4_2026f_224569_api_manual.zip`: Javadoc 전체 HTML
- `ssd_ip_cp4_2026f_224569_report.pdf`: 보고서
- 최종 제출: 위 파일과 실행 안내를 포함한 `ssd_ip_cp4_2026f_224569.zip`

GitHub: https://github.com/kim1034/2026-SoftwareSystemArchitecture
