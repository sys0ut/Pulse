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
- `CUBRID_BIN` (옵션): 기본 `cubrid` (PATH에 없으면 절대경로)

### 실행
현재 빌드 산출물은 **Java 21 런타임**으로 실행하는 것을 기준으로 합니다.

```bash
export CUBRID_DB_LIST="demodb"
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

### 실행
`ops/monitoring/prometheus/prometheus.yml`의 `<EXPORTER_HOST>`만 실제 주소로 변경한 뒤 실행합니다.

```bash
cd ops/monitoring
docker compose up -d
```

### 접속/확인
- Prometheus: `http://localhost:9090` → Status → Targets → `pulse-cubrid`가 UP
- Grafana: `http://localhost:3000` (admin/admin)

