# 다중 벨트 백엔드

## 저장소와 호환성

코드 작업 위치: `C:\Users\Jinho\IdeaProjects\smartparcel`
브랜치: `portfolio/backend-scalability`

기존 public.users와 로그인/JWT를 유지하고 organization_id를 추가했다.
새 도메인은 parcel PostgreSQL 스키마에 둔다. 모든 조직은 같은 테이블을 공유하며 복합 FK와 API 권한 검사로 경계를 지킨다.
JDBC로 복합 FK, 부분 UNIQUE, 명시적 잠금과 트랜잭션을 표현했다.
기존 JPA와 같은 DataSource 및 Spring 트랜잭션 관리자를 사용한다.

이전 장비·설정 API인 /api/devices/**, /api/chutes/**, /api/sorting-groups/**, /api/sorting-rules/**는 차단했다.
기존 이력 조회는 기존 API에 남아 있다. 기존 프런트엔드와 AI 프로그램은 v2로 전환해야 새 벨트를 운영할 수 있다.
새 API 응답은 JSON 객체/배열이며 기존 ApiResponse wrapper를 사용하지 않는다.

## Flyway

| 버전 | 역할 |
| --- | --- |
| V1 | 빈 DB에 기존 프로젝트 테이블 생성 |
| V2 | 사용자 조직 소속 이전, parcel 도메인·제약·트리거·인덱스·이미지 테이블 |
| V3 | 관리자별 기본 벨트와 기존 설정을 초안으로 이전 |

빈 DB에서는 애플리케이션 시작 시 V1~V3을 적용한다. Hibernate DDL 자동 변경은 사용하지 않는다.
기존 팀 프로젝트 DB는 백업/복제본에서 V1과 일치하는지 확인한 뒤 버전 1 baseline을 명시적으로 설정한다.
예: `./gradlew bootRun --args='--spring.profiles.active=local --spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=1'`.
다음 실행부터 baseline 옵션을 제거한다. 기본 자동 baseline은 꺼져 있다.

사용자가 별도 DDL을 이미 수동 적용한 DB에는 이 baseline 절차를 사용하지 않는다.
빈 DB에서 시작하거나 해당 DB에 맞는 별도 전환이 필요하다.
작성 중인 db/migration/ddl.sql은 보존했다. Flyway가 실행하는 V번호__설명.sql 형식의 파일이 아니므로 실행되지 않는다.

기존 이력과 이미지 URL은 public에 보존한다. 알 수 없는 과거 deviceId/eventId/configVersion을 생성하지 않는다.
N:M 목적지, 누락 슈트, 중복 정규화 입력 등은 parcel.migration_issues에 기록한다.
문제가 있는 초안은 배포할 수 없다. 운영자가 새 버전에서 모호한 규칙을 명확히 지정해야 한다.
이전 설정은 모두 DRAFT이며 자동 활성화하지 않는다.

배포 잠금 순서는 벨트 → 버전 → 자식 설정이다.
버전 행을 잠근 뒤 구성 검사와 PUBLISHED 전환을 한 트랜잭션으로 처리한다.
배포된 설정에 대한 INSERT/UPDATE/DELETE는 트리거가 차단한다.

## 로컬 실행 (PowerShell, Java 17 / Docker / Python 3)

저장소 루트:
```powershell
Copy-Item .env.example .env
# .env의 DB_PASSWORD를 로컬에서 정한 값으로 수정
docker compose up -d
$env:DB_PASSWORD = "위에서 정한 로컬 비밀번호"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
cd smart-parcel-backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

local 프로필은 DB/Redis를 localhost로 지정한다.
Redis는 기존 로그인 refresh token 저장에 사용하며, 새 장비 이벤트 전달에 Redis/Kafka를 사용하지 않는다.
이미지는 PNG/JPEG 5 MiB, 최대 1,600만 픽셀까지 받는다.

새 PowerShell 창, 저장소 루트:
```powershell
$env:PARCEL_EMAIL = "demo@example.test"
$env:PARCEL_PASSWORD = "로컬에서 정한 8자 이상의 비밀번호"
python tools/provision_demo.py --signup
```

기존 관리자면 --signup을 빼거나 PARCEL_ADMIN_TOKEN에 로그인 access token을 설정한다.
서로 다른 규칙을 가진 벨트 A/B와 장비를 생성하고, 발급 키는 .local/demo-devices.json에 저장한다.
이 파일은 Git에서 제외된다. DB에는 키 원문 대신 BCrypt 해시를 저장한다.

```powershell
$devices = Get-Content .local/demo-devices.json -Raw | ConvertFrom-Json
$env:PARCEL_DEVICE_ID = $devices[0].deviceId
$env:PARCEL_DEVICE_KEY = $devices[0].deviceKey
python tools/device_simulator.py --spool .local/belt-a.sqlite3 setup
python tools/device_simulator.py --spool .local/belt-a.sqlite3 demo
python tools/device_simulator.py --spool .local/belt-a.sqlite3 status
```

demo는 설정 조회·적용 보고, 테스트 PNG를 포함한 판정, 장비 오류를 실제 HTTP로 전송한다.
두 번째 장비는 $devices[1]과 별도 spool 경로를 사용한다.

## 오프라인·재전송

```powershell
# 설정을 한 번 조회한 뒤에는 네트워크 없이도 이미지와 사건을 저장 가능
python tools/device_simulator.py --spool .local/belt-a.sqlite3 enqueue-decision --image C:/images/parcel.jpg
python tools/device_simulator.py --spool .local/belt-a.sqlite3 enqueue-error --code CAMERA_ERROR
python tools/device_simulator.py --spool .local/belt-a.sqlite3 enqueue-discharge --attempt-id "UUID" --failed --code SERVO_ERROR
python tools/device_simulator.py --spool .local/belt-a.sqlite3 flush --wait
# 이미 ACK된 사건도 ID·본문·이미지를 그대로 재전송
python tools/device_simulator.py --spool .local/belt-a.sqlite3 requeue "event UUID"
python tools/device_simulator.py --spool .local/belt-a.sqlite3 flush
```

SQLite WAL/FULL 동기화로 이미지 바이트와 eventId·본문을 먼저 저장한다.
대기열 선두의 ACK를 받기 전에는 뒤의 사건을 보내지 않는다.
네트워크/일시 서버 장애는 대기 상태를 유지하고, --wait는 backoff+jitter로 계속 재시도한다.
400/401/403/409 등 영구 오류는 BLOCKED로 보존하고 운영자가 확인해야 한다.
ACKED 기록도 재현을 위해 보존하며 자동 삭제하지 않는다. 용량 관리·보관 정책은 후속 과제다.
실제 장비의 디스크 손상이나 SQLite 저장 이전 사건까지 복구한다고 보장하지 않는다.

## 관리 API — 사용자 JWT Bearer 인증

조직은 인증된 사용자의 소속에서 결정하고 변경 작업에는 MANAGER 권한을 요구한다.
공개 STAFF 가입에서 관리자 이메일만으로 타 조직에 들어가는 경로는 차단했다.
직원은 관리자가 POST /api/v2/staff로 생성한다.

| 메서드 | 경로 | 용도/본문 |
| --- | --- | --- |
| GET | /api/v2/organization | 내 조직 |
| GET/POST | /api/v2/belts | 목록 / {code,name} |
| GET/POST | /api/v2/belts/{belt}/chutes | 목록 / {code,name} |
| GET/POST | /api/v2/belts/{belt}/devices | 목록(키 제외) / {code}, 발급 시 키 1회 반환 |
| POST | /api/v2/devices/{id}/deactivate?revoke=false | 제어권 해제, true면 키도 폐기 |
| GET/POST | /api/v2/belts/{belt}/versions | 목록 / 아래 설정 JSON |
| GET | /api/v2/belts/{belt}/versions/{version} | 규칙·슈트 스냅샷 |
| POST | /api/v2/belts/{belt}/versions/{version}/publish | 불변 버전 배포 |
| PUT | /api/v2/belts/{belt}/active-version | {versionId,expectedVersionId} |
| POST | /api/v2/staff | {email,name,password} |
| GET | /api/v2/attempts?beltId=…&from=…&to=…&size=50&page=0 | 조직·벨트 이력 |
| GET | /api/v2/attempts/{attemptId}/events | 관련 사건 |
| GET | /api/v2/events/{eventId}/image | 조직 권한 검사 후 이미지 |
| GET | /api/v2/stats?beltId=…&from=…&to=… | 시도 기반 통계 |
| GET | /api/v2/notifications | 사용자별 최근 미확인 알림 |

expectedVersionId는 현재 지정 버전이며 최초에는 null이다. 동시 활성화 충돌은 409로 반환한다.

설정 생성 예:
```json
{
  "groupName": "구호 물자",
  "chutes": [{"chuteId": 1, "servoDeg": 45}],
  "rules": [{
    "name": "의약품", "priority": 1,
    "inputType": "TEXT", "inputValue": "K1S",
    "itemName": "의약품 상자", "chuteId": 1
  }]
}
```

priority가 작을수록 먼저 평가하며 입력값은 trim 후 대문자로 정규화한다.
이름·각도는 배포 버전의 스냅샷을 사용한다.
조회 기간은 [from,to), 정렬은 capturedAt DESC, attemptId DESC다.
목록은 OFFSET 방식이며 총건수는 계산하지 않는다. 페이지 방식 성능 비교는 후속 실험이다.

## 장비 API

X-Device-Id: UUID와 X-Device-Key: 발급 키 헤더를 사용한다.
사용자 JWT나 managerId로 인증하지 않는다.

- GET /api/v2/device/setup: 장비 소속 벨트의 지정 설정.
- PUT /api/v2/device/applied-version: {versionId}. 지정 버전이 바뀌었으면 409.
- POST /api/v2/device/events: multipart payload(JSON), image(PNG/JPEG).
- DECISION: 이미지·attemptId·ruleVersionId·capturedAt·decidedAt·decisionStatus 필수.
- MATCHED: ruleId·chuteId·recognizedInputType·recognizedValue 필수.
- UNMATCHED/ERROR: 목적 규칙·슈트 없음. ERROR는 errorCode 필수.
- DISCHARGE_FAILED: attemptId·errorCode 필수. DISCHARGE_CONFIRMED: attemptId 필수.
- DEVICE_ERROR: errorCode 필수, 시도를 모르면 attemptId 생략.

active=false인 교체 장비도 키가 폐기되지 않았다면 이전 결과를 보낼 수 있지만 새 설정은 받을 수 없다.
과거 버전의 지연 결과도 해당 벨트의 배포 버전이면 허용한다.

## 원자성과 충돌

eventId → attemptId 순서로 PostgreSQL 트랜잭션 advisory lock을 잡는다.
파싱한 DTO의 일관된 JSON 직렬화와 이미지 바이트에 대해 서버가 SHA-256을 계산한다.
동일 이벤트·내용은 기존 ACK, 동일 ID의 다른 내용·소속은 409다.
다른 eventId로 같은 attemptId의 판정을 덮어쓰는 것도 409다.

시도·이벤트·이미지 BYTEA·오류 알림은 같은 트랜잭션에 저장하고 커밋 뒤 응답한다.
응답만 유실돼도 같은 ID로 재전송할 수 있다.
이미지는 별도 테이블에 두어 일반 목록에서 바이트를 읽지 않는다.
대량 이미지 저장 비용·보관·객체 저장소 전환은 별도 검증 과제다.

배출 성공·실패가 충돌하면 둘 다 보존하고 FAILED + discharge_conflict=true로 결정한다.
최종 상태의 사건 중 가장 이른 occurredAt을 dischargeAt으로 사용한다.
동일 시각이면 관측 슈트 중 가장 작은 ID로 일관되게 선택한다.
충돌 건수는 실패 건수의 부분집합으로 별도 표시한다.
v2 알림은 DB에 저장하며 SSE/메시지 브로커 전달은 이번 범위에 포함하지 않았다.

## 검증

전용 DB 이름은 _test로 끝나야 하며 테스트 계정에 임시 DB 생성·삭제 권한이 필요하다.
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

실제 PostgreSQL에서 마이그레이션, 조직/벨트 FK, 배포 불변성, 동시 재전송, 배포·편집 경쟁,
롤백, 사건 순서 역전, 장비 인증, 타 조직 이미지 차단을 검증한다.
Python 데모 생성기와 시뮬레이터를 실제 임시 HTTP 서버에 연결하는 테스트도 포함한다.
별도 테스트는 기존 설정·이력이 있는 DB를 baseline 후 이전하고 모호한 N:M 규칙이 기록되는지 확인한다.
기능 검증을 성능 개선율이나 100벨트 부하·장시간 장애 복구 목표 달성으로 표현하지 않는다.
