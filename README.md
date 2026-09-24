# Smart Parcel

AI·IoT 소형 분류 시스템의 팀 프로젝트와 Java/Spring 백엔드 확장 작업을 함께 관리합니다.

- [팀 프로젝트 기준점](docs/portfolio/00-team-project-baseline.md)
- [확장 요구사항·규모](docs/portfolio/01-expansion-requirements.md)
- [다중 벨트 API, 마이그레이션, 실행·검증 방법](docs/multi-belt.md)
- 백엔드: `smart-parcel-backend` (Java 17 / Spring Boot / PostgreSQL)
- 실제 장비 없는 검증: `tools/provision_demo.py`, `tools/device_simulator.py` (Python 표준 라이브러리)

개인 고도화 브랜치는 `portfolio/backend-scalability`입니다. 신규 운영 API는 `/api/v2`입니다.
기존 React·Flutter·AI 장비 클라이언트의 v2 전환은 별도 작업입니다.
성능 개선율은 아직 측정하지 않았습니다.
