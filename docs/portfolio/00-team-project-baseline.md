# 팀 프로젝트 기준선

이 문서는 캡스톤 팀 프로젝트의 마지막 상태와 이후 박진호 개인 고도화 작업의 경계를 고정한다. 기준 태그 이후의 커밋만 개인 고도화 범위로 설명한다.

## Git 기준점

| 저장소 | 당시 브랜치 | 기준 커밋 | 기준 태그 | 개인 고도화 브랜치 |
| --- | --- | --- | --- | --- |
| `smart-parcel` | `main` | `0645cf5ac6a7990001179cdac6b5cbb66d65c3a6` | `baseline/team-project-final` | `portfolio/backend-modernization` |
| `smart-sort-ai` | `skdud` | `7846bca7f1e947a56794e98b0364cc969ade60a2` | `baseline/team-project-final` | `portfolio/backend-modernization` |

기준 태그는 기존 팀 프로젝트의 마지막 커밋을 가리킨다. 고도화 작업은 두 저장소의 `portfolio/backend-modernization` 브랜치에서만 진행한다.

현재 로컬 구조에서 `smart-sort-ai`는 `smart-parcel` 폴더 안에 있지만 별도 `.git`을 가진 독립 저장소다. `smart-sort-arduino`도 별도 저장소이며 이번 백엔드 고도화 기준선의 변경 대상에서는 제외한다. 상위 저장소에서 두 중첩 저장소를 통째로 `git add`하지 않는다.

### 원격 저장소 기록

- `smart-parcel`: `https://github.com/jinho-jinho/smart-parcel.git`
- 로컬 `smart-sort-ai`의 현재 `origin`: `https://github.com/yoniyon03/smart-sort-ai.git`
- 포트폴리오에서 제시한 AI 저장소: `https://github.com/jinho-jinho/smart-sort-ai`

AI 저장소의 로컬 `origin`과 포트폴리오에서 제시한 URL이 다르다. 기준선 보존 단계에서는 원격 주소를 임의로 변경하지 않는다. 이후 푸시 전에 개인 저장소를 `origin` 또는 별도 remote로 사용할지 결정한다.

## 당시 구현 실행 방법

아래 절차는 기준 커밋에 존재하는 파일과 진입점을 바탕으로 정리한 것이다. 비밀번호, JWT 키, 메일 계정 등 설정값은 이 문서에 복사하지 않는다.

### Spring Boot 백엔드

필요 구성:

- Java 17
- PostgreSQL
- Redis
- `smart-parcel-backend/src/main/resources/application.properties`의 DB, Redis, 메일, JWT, 쿠키, 파일 저장소 설정

Windows PowerShell에서 실행:

```powershell
cd smart-parcel-backend
.\gradlew.bat bootRun
```

기준 코드의 기본 서버 포트는 `8080`이다. 설정 파일의 실제 인증 정보는 Git 기록이나 포트폴리오 문서에 노출하지 않는다.

### React 웹

필요 구성:

- Node.js와 npm. 기준 저장소에는 Node 버전 고정 파일이 없다.
- 로컬 API 사용 시 `VITE_API_BASE_URL` 환경 변수. 미지정 시 기준 코드는 Azure 배포 API를 사용한다.

```powershell
cd smart-parcel-web
npm ci
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

### Flutter 모바일

기준 `pubspec.yaml`은 Dart SDK `^3.7.2`를 요구한다.

```powershell
cd smart_parcel_mobile
flutter pub get
flutter run
```

### AI·장치 프로그램

기준 저장소에는 Python 버전 파일과 의존성 잠금 파일이 없다. 코드에서 확인되는 외부 패키지는 `ultralytics`, `opencv-python`, `pyserial`, `torch`, `paddleocr`, `numpy`, `requests`다. 정확한 버전 재현은 후속 환경 표준화 단계에서 해결한다.

실행 전에 다음 상수를 장치 환경에 맞게 확인한다.

- `main/device_api.py`: `BASE_URL`, `MANAGER_ID`
- `main/total_run_project.py`: `SERIAL_PORT`, `BAUD_RATE`, `CAMERA_INDEX`, `TEST_MODE`
- 모델 파일: `training/runs/detect/train12/weights/best.pt`

```powershell
cd smart-sort-ai
python .\main\total_run_project.py
```

카메라, Arduino 시리얼 장치, 학습된 모델 파일이 없으면 전체 장치 연동 실행은 완료되지 않는다.

## 당시 보고된 성능 수치

다음 값은 최종보고서에 기록된 당시 결과이며, 이번 고도화 과정에서 재현하거나 검증한 값이 아니다.

- 도구: k6
- 부하: 가상 사용자 50명
- 시간: 1분
- 한 반복의 요청: 장치 설정 조회, 이미지 없는 오류 등록, 분류 이력 10건 조회, 일별 통계 조회
- 반복 사이 대기: 0.5초
- 총 HTTP 요청: 1,685건
- 처리량: 약 26.6 req/s
- 평균 응답 시간: 약 1.69초
- 중앙값: 약 1.45초
- p90: 약 3.02초
- p95: 약 3.87초
- 최대: 약 8.07초
- 보고된 HTTP/check 오류율: 0%

`p95 3.87초`는 위 네 종류 API가 섞인 전체 HTTP 요청의 p95다. 특정 API의 지연 시간도 아니고, 누락 없는 전달을 증명하는 수치도 아니다.

## 이후 측정값과 비교하는 규칙

포트폴리오에서는 기존 수치를 `[당시 보고]`, 새 실험을 `[개인 고도화 재측정]`으로 표시한다.

직접 전후 비교는 다음 조건이 모두 같은 경우에만 한다.

- 같은 API와 HTTP 메서드
- 같은 요청 파라미터와 응답 크기
- 같은 데이터 건수와 분포
- 같은 동시 사용자 수 또는 요청 도착률
- 같은 시험 시간과 워밍업 방식
