# Quick Test Guide

## Prerequisites
✅ Java 21 installed
✅ Maven installed (or use ./mvnw)
✅ **Docker Desktop running** (required for Testcontainers)

## Running Tests

### Run All Tests
```bash
cd notification
./mvnw clean test
```

### Run Specific Test Class
```bash
# Application integration tests
./mvnw test -Dtest=NotificationApplicationTests

# Notification service tests
./mvnw test -Dtest=NotificationServiceIntegrationTest

# Order service tests
./mvnw test -Dtest=OrderServiceIntegrationTest
```

### Run Single Test Method
```bash
./mvnw test -Dtest=NotificationApplicationTests#shouldCreateOrderAndPublishEventToRabbitMQ
```

### Run with Detailed Output
```bash
./mvnw clean test -X
```

## Test Execution Time
⏱️ Expected: ~30-60 seconds (includes Docker container startup)

## Troubleshooting

### "Cannot connect to Docker daemon"
**Solution**: Start Docker Desktop

### "Tests timeout"
**Cause**: RabbitMQ container taking too long to start
**Solution**: 
```bash
docker pull rabbitmq:3.13-management-alpine
```

### "Port already in use"
**Cause**: Previous test didn't clean up
**Solution**:
```bash
docker ps
docker stop <container-id>
```

### H2 Database Issues
**Solution**: Tests use in-memory H2, automatically cleaned up

## Test Output Example
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running NotificationApplicationTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running NotificationServiceIntegrationTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running OrderServiceIntegrationTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
```

## What Each Test Class Validates

### NotificationApplicationTests
✅ Full application context
✅ RabbitMQ integration
✅ REST endpoints
✅ End-to-end flows

### NotificationServiceIntegrationTest
✅ Email/SMS sending
✅ Async behavior
✅ Metrics recording
✅ Error handling

### OrderServiceIntegrationTest
✅ Order creation
✅ Event publishing
✅ Idempotency
✅ Data consistency

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Integration Tests
  run: ./mvnw clean test
  env:
    TESTCONTAINERS_RYUK_DISABLED: false
```

### Docker Compose for CI
```yaml
version: '3.8'
services:
  maven-tests:
    image: maven:3.9-eclipse-temurin-21
    volumes:
      - .:/app
      - /var/run/docker.sock:/var/run/docker.sock
    working_dir: /app/notification
    command: mvn clean test
```

## Viewing Test Reports

### Surefire Reports
```bash
open notification/target/surefire-reports/index.html
```

### Console Output
All test output is logged to console with timestamps and details.

## Test Coverage (Future Enhancement)
```bash
# After adding JaCoCo plugin
./mvnw clean verify
open target/site/jacoco/index.html
```

## Quick Verification
After making code changes:
```bash
./mvnw clean test
```

Look for: `BUILD SUCCESS` ✅
