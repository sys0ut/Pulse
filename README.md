# Pulse (v1) - 실행 방법

이 레포의 데모 목표는:
- CUBRID 서버(호스트 또는 컨테이너 내부)에서 **`db-exporter`**를 실행해서 `/actuator/prometheus`로 메트릭을 노출
- WSL에서 **Prometheus + Grafana**를 Docker로 실행해서 exporter를 scrape 하고 웹 UI로 확인

---

## 1) CUBRID 서버에서 `db-exporter` 실행

### 전제(중요)
`db-exporter`는 내부에서 아래 커맨드를 실행합니다. **Exporter를 실행하는 위치에서 커맨드가 돌아야 합니다.**
- `cubrid broker status -b`
- `cubrid heartbeat status`
- `cubrid tranlist <db>`
- `cubrid spacedb -sp <db>`

### 환경변수
- `CUBRID_DB_LIST` (필수): 예) `demodb,tt`
  - `databases.txt`가 **multihost** 설정이면 DB명이 `demodb@localhost`처럼 **`@호스트`를 포함**해야 합니다. (미포함 시 `tranlist/spacedb`가 실패)
- `CUBRID_BIN` (옵션): 기본 `cubrid` (PATH에 없으면 절대경로)

### 실행
현재 빌드 산출물은 **Java 21 런타임**으로 실행하는 것을 기준으로 합니다.

```bash
export CUBRID_DB_LIST="demodb@localhost"
export CUBRID_BIN="cubrid"

/opt/jdk-21*/bin/java -jar db-exporter-0.1.0-SNAPSHOT.jar
```

### 확인
```bash
curl -s http://localhost:8085/actuator/prometheus | grep pulse_cubrid_scrape_success
curl -s http://localhost:8085/actuator/prometheus | grep pulse_cubrid_db_scrape_success
```

---

## 2) WSL에서 Prometheus + Grafana 실행(Docker)

### 사전 확인
WSL에서 exporter 접근이 되어야 합니다.

```bash
curl -s http://<EXPORTER_HOST>:8085/actuator/prometheus | head
```

### 케이스 A) 이미 Prometheus + Grafana가 떠 있는 경우(추천)
- **Prometheus**: scrape target에 exporter를 추가합니다.
  - 예) `/etc/prometheus/prometheus.yml`
    - `targets: ["172.31.201.42:8085"]`
- **Grafana**:
  - Prometheus 데이터소스를 추가합니다. (URL 예: `http://prometheus:9090`)
  - 대시보드 Import(JSON): `ops/monitoring/grafana/dashboards/pulse-cubrid-7panels.json`
  - Import 후 패널이 `No data`면, Grafana 데이터소스 **이름이 `Prometheus`인지** 확인하세요.
    - (대시보드/변수가 기본적으로 `Prometheus` 이름을 사용합니다)

### 케이스 B) `ops/monitoring` 스택으로 새로 띄우는 경우
`ops/monitoring/prometheus/prometheus.yml`의 `<EXPORTER_HOST>`만 실제 주소로 변경한 뒤 실행합니다.

```bash
cd ops/monitoring
docker compose up -d
```

### 접속/확인
- Prometheus: `http://localhost:9090` → Status → Targets → `pulse-cubrid`가 UP
- Grafana: `http://localhost:3000` (admin/admin)
  - `Dashboards`에서 **`Pulse - CUBRID (7 panels)`** 대시보드가 자동으로 보입니다.
  - 대시보드 상단 변수에서 `db/broker/node`를 선택한 뒤 Refresh 하세요.

