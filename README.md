# Smart Parcel

Java 17 / Spring Boot / Spring Data JPA / PostgreSQL 기반 다중 벨트 백엔드.
개인 개선 브랜치는 `portfolio/backend-scalability`이며 [팀 프로젝트 기준점](docs/portfolio/00-team-project-baseline.md)은 보존한다.

## 구조와 범위

- 기존 `domain → repository → service → controller` 구조를 확장한다. 조직·벨트·장비, 설정 버전, 처리 시도(`attemptId`), 이벤트(`eventId`)를 JPA로 관리한다.
- 조직은 로그인 사용자 또는 인증된 장비에서 결정한다. 관리 요청은 MANAGER만 허용하며 다른 조직·벨트 연결은 API와 복합 FK로 거절한다.
- 배포된 규칙·슈트 이름·각도는 변경할 수 없다. 새 버전을 만들고 배포·활성화한다. 활성화 요청의 `expectedVersionId`가 현재 값과 다르면 409다.
- 이벤트·이미지·알림은 같은 트랜잭션에 저장한다. 동일 ID·내용은 중복 ACK, 같은 ID의 다른 내용은 409다. 존재하지 않는 ID의 동시 수신 잠금에만 PostgreSQL 네이티브 쿼리를 사용한다.
- 이력은 JPA `Page`의 OFFSET 및 필요한 COUNT 조회를 기준으로 둔다. 부하 실험·쿼리 최적화·성능 개선율은 아직 수행하지 않았다.
- Redis는 기존 로그인 토큰 관리에 사용한다. 이벤트 복구는 장비의 SQLite 대기열과 재전송으로 검증하며 Kafka/Redis 큐는 추가하지 않았다.

## 실행

저장소 루트에서 실행한다. Compose는 로컬 PostgreSQL·Redis용이며, 이미 설치한 서비스를 사용해도 된다.

```powershell
Copy-Item .env.example .env
# .env의 DB_PASSWORD를 원하는 로컬 비밀번호로 변경
docker compose up -d
$env:DB_PASSWORD = "동일한 로컬 비밀번호"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
cd smart-parcel-backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

DB 기본값은 `localhost:5432/smartparcel_db`, 사용자 `smartparcel`이다. 다른 DB는 `DB_URL`·`DB_USERNAME`으로 지정한다.
`application-local.properties`는 로컬 DB·Redis 연결을 설정한다. `.env`는 Compose가 읽으며 Java 실행에는 위 환경변수도 필요하다.

## 장비 없이 검증

새 터미널의 저장소 루트에서:

```powershell
$env:PARCEL_EMAIL = "demo@example.test"
$env:PARCEL_PASSWORD = "로컬에서 정한 8자 이상의 비밀번호"
python tools/provision_demo.py --signup
$devices = Get-Content .local/demo-devices.json -Raw | ConvertFrom-Json
$env:PARCEL_DEVICE_ID = $devices[0].deviceId
$env:PARCEL_DEVICE_KEY = $devices[0].deviceKey
python tools/device_simulator.py --spool .local/belt-a.sqlite3 demo
python tools/device_simulator.py --spool .local/belt-a.sqlite3 status
```

기존 관리자 계정은 `--signup`을 생략한다. 생성기는 벨트 A/B, 슈트, 배포 설정, 장비를 만들고 키를 Git에서 제외된 로컬 파일에 저장한다.
`demo`는 설정 조회·적용 보고, PNG를 포함한 분류 결과, 장비 오류를 실제 HTTP로 전송한다.
두 번째 장비는 `$devices[1]`과 별도의 spool 파일을 사용한다.

설정 조회 후 `enqueue-decision --image 경로`, `enqueue-error --code CAMERA_ERROR`로 오프라인 저장하고
`flush --wait`로 재전송한다. `requeue "event UUID"`는 ID·본문·이미지를 그대로 다시 보낸다.
ACK 이전에는 로컬 기록을 보존하며 400/401/403/409 등의 영구 오류는 BLOCKED로 남긴다.

## API

관리 API는 사용자 Bearer JWT, 장비 API는 `X-Device-Id`·`X-Device-Key`를 사용한다.

| 경로 | 용도 |
| --- | --- |
| GET/POST `/api/belts` | 내 조직 벨트 조회·생성 |
| GET/POST `/api/belts/{belt}/chutes`, `…/devices` | 슈트·장비 조회·등록 |
| GET/POST `/api/belts/{belt}/versions` | 설정 버전 조회·생성 |
| GET `/api/belts/{belt}/versions/{version}` | 당시 규칙·슈트 스냅샷 |
| POST `/api/belts/{belt}/versions/{version}/publish` | 배포 후 변경 금지 |
| PUT `/api/belts/{belt}/active-version` | 버전 활성화 |
| GET `/api/attempts?beltId=…&from=…&to=…&page=0&size=50` | 처리 시도 페이지 |
| GET `/api/attempts/{attemptId}/events`, `/api/events/{eventId}/image` | 사건·인증된 이미지 조회 |
| GET `/api/stats?beltId=…&from=…&to=…`, `/api/errors/history?…` | 통계·오류 페이지 |
| GET/POST/DELETE `/api/admin/staff` (`DELETE /{id}`) | 조직 내 직원 관리 |
| GET `/api/notifications`, PATCH `/api/notifications/{id}/read` | 내 알림 조회·읽음 처리 |
| GET `/api/devices/setup`, PUT `/api/devices/applied-version`, POST `/api/devices/events` | 장비 설정·이미지 포함 이벤트 |

설정 및 이벤트 요청 예시는 `tools/provision_demo.py`와 `tools/device_simulator.py`가 실행 가능한 기준이다.
조회 기간은 `[from, to)`, 페이지 크기는 최대 100이다. 이미지는 PNG/JPEG 5 MiB, 최대 1,600만 픽셀이다.
기존 `/api/v2` 병행 API와 managerId 기반 장비 쓰기는 제거했다.
팀 프로젝트의 React·Flutter·실장비 AI 클라이언트는 기존 API 계약을 사용하므로, 이번 백엔드 검증은 갱신된 시뮬레이터로 수행한다.

## 마이그레이션과 테스트

Flyway V1~V3 이력은 변경하지 않았다. V4는 기존 팀 프로젝트 테이블을 `legacy`에 보관하고 운영 테이블을 `public`으로 통합한다.
과거 이력에 존재하지 않았던 장비·이벤트 ID를 만들지 않는다. 모호한 이전 규칙은 `migration_issues`에 보존하며 새 검토 버전이 필요하다.
기존 프로젝트 DB는 백업/복제본에서 V1과 일치하는지 확인한 뒤 버전 1 baseline을 명시적으로 적용해야 한다.
수동 DDL을 적용한 DB에 자동 baseline을 사용하지 않는다. 작성 중인 `db/migration/ddl.sql`은 Flyway 실행 대상이 아니다.

전용 PostgreSQL 테스트 DB 이름은 `_test`로 끝나야 한다. 마이그레이션 테스트 계정에는 임시 DB 생성·삭제 권한이 필요하다.

```powershell
docker compose exec postgres createdb -U smartparcel smartparcel_test
$env:TEST_DB_URL = "jdbc:postgresql://127.0.0.1:5432/smartparcel_test"
$env:TEST_DB_USER = "smartparcel"
$env:TEST_DB_PASSWORD = "로컬 DB 비밀번호"
$env:PYTHON = "python"
cd smart-parcel-backend
.\gradlew.bat test
cd ..
python -m unittest discover -s tools -p "test_*.py"
```
