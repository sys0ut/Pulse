# Pulse Observability Mini Platform (v1)

Spring Boot + CUBRID 11.4 기반의 **로그/메트릭 수집·조회** 미니 플랫폼(v1) 프로젝트 골격입니다.

## Modules

- `common`: 공통 DTO/Validation/ID/태그 정규화 유틸
- `storage`: CUBRID JDBC 기반 DAO/SQL(배치 insert, 조회, 롤업 upsert)
- `log-ingest`: 로그 수집(write-only) 서비스
- `metric-ingest`: 메트릭 수집(write-only) + 1m 롤업 업데이트 서비스
- `query-api`: 로그 조회(read-only), 메트릭 조회(read-only, 기본 rollup_1m) 서비스
- `partition-manager`: 파티션 생성/삭제(DDL) 전용 서비스

## Quick Start (dev)

1) (권장) Docker로 CUBRID/Prometheus/Grafana를 올립니다.

- `docker/docker-compose.yml` 참고

2) 서비스 실행(예: query-api)

```bash
./gradlew :query-api:bootRun
```

## Notes

- CUBRID JDBC 드라이버는 환경에 따라 Maven 좌표가 다를 수 있어, 기본은 “드라이버는 런타임에 제공”하는 형태로 구성했습니다.
- 운영(v1) 기준:
  - 로그 `message`는 **8KB까지만 메인 테이블 저장**, 초과분은 `log_payload`로 분리 저장
  - 메트릭은 **`metric_rollup_1m`(1분 롤업)**을 Grafana 기본 조회 경로로 사용

