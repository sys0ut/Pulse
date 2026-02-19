## Pulse Coding Conventions (Java 21)

### Formatting (Spotless)
- **Check**: `./gradlew spotlessCheck`
- **Apply**: `./gradlew spotlessApply`

Recommended: enforce `spotlessCheck` in CI on push / pull request.

### Static analysis (Error Prone)
- Runs during `compileJava` when enabled.
- Recommended CI command: `./gradlew clean spotlessCheck build`

### Project rules (v1)
- **Write-only services**: `log-ingest`, `metric-ingest`
- **Read-only service**: `query-api`
- **DDL/TTL ops**: `partition-manager` (disabled by default)
- **DB access** must go through `storage` (no SQL in controllers/services)
- Use **keyset pagination** for logs (avoid OFFSET paging)

