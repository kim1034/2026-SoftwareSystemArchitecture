# Beta 0.4.0 검증 기록

검증 날짜: 2026-11-02. Java 17.0.20, Linux, Swing/X11 가상 디스플레이.

## 실행 결과

- CoreTests: 89 assertions PASS. 기본값/시간 우선순위/하루종일/충돌/추천/상태/재계획/저장 실패/한글 복원.
- GuiSmokeTests: 17 assertions PASS. Robot으로 실제 버튼 클릭, 후보 선택 후 미저장, 저장 확정, 충돌 저장 비활성화, 취소, 설정 화면, 재계획과 재시작 데이터 확인.
- JarSmokeTests: 임시 새 디렉터리에 제출 JAR 복사 후 2회 시작/종료 PASS. 원래 저장 데이터 유지.
- Javadoc: HTML 생성 성공, 문법 검사 오류 없음. API ZIP은 index.html과 관련 HTML/CSS/JS를 함께 포함.
- Windows: PowerShell 빌드/검사/Javadoc 스크립트 제공. Windows에서의 실행은 아직 확인하지 않았음.

## 요구사항별 검증 연결

| 요구사항 | 구현 | 확인 방법 |
|---|---|---|
| R1~R4, R24, R26 | ScheduleService.prepare | CoreTests 입력/시간 검증 및 GUI 등록 |
| R5, R22, R32~R34, R42 | MainFrame, ScheduleService | 검색/필터/삭제/달력 count 자동 검사와 화면 점검 |
| R6~R9, R36 | SettingsService, Schedule.effectiveMinutes | 기본값 변경/최대 시간 변경/기존 일반 보존 검사 |
| R10~R14, R25, R30 | PlanningService.checkConflicts | 최대와 같음/초과/겹침/경계/자신 제외/완료 포함 검사 |
| R15~R19, R28~R29, R31 | recommendDates, ScheduleDialog | 기간/정렬/오늘 예외/후보 선택 미저장/충돌 저장 차단 검사, 더 보기 화면 점검 |
| R20~R21, R27, R40 | updateScheduleProgress, isOverdue | 미완료/상태/동일 ID 이동/시간 보존/실패 보존 검사 |
| R23, R41 | FileRepository, StateStore | XML 한글 roundtrip, 가짜 저장 실패, 실제 교체 실패/임시 정리, JAR 재시작 검사 |
| R35, R37~R39 | prepare, checkConflicts, ScheduleDialog | 수동/자동 하루종일과 배타성/추천 검사, 입력 비활성화 코드 및 화면 확인 |

## 수동 최종 확인

1. Windows에서 JDK 명령 4개가 실행되는지 확인.
2. VS Code에서 scripts/build.ps1 실행 후 제출 JAR 실행.
3. 일정 하나 등록, 종료 후 재실행, 일정 유지 확인.
4. api-manual/index.html을 웹 브라우저에서 열기.
5. 최종 ZIP을 새 폴더에 풀고 JAR/PDF/API/소스가 모두 열리는지 확인.

자동 검사 개수는 전체 요구사항 개수와 같다는 뜻이 아니며, 화면 설명과 자동 검사를 함께 사용했다.
