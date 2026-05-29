# CI/CD: Add Kafka + MailHog to GitHub Actions

## Problem

GitHub Actions workflow runs `mvn test` but has no Kafka or email service
containers. While current tests are Mockito-based unit tests (no Spring context),
the CI environment differs significantly from production and local dev, limiting
future integration test capability.

## Solution

Add two service containers to `.github/workflows/build.yml`:

- **Kafka in KRaft mode** (no ZooKeeper dependency) — matches `confluentinc/cp-kafka:7.5.0`
  from docker-compose.yml. KRaft mode eliminates inter-service container DNS
  issues in GitHub Actions networking.
- **MailHog** — matches local dev SMTP capture. Required for notification-service
  email integration tests.

## Container Configuration

### Kafka (KRaft mode)
- Image: `confluentinc/cp-kafka:7.5.0`
- Port: `9092:9092`
- Advertised listener: `PLAINTEXT://localhost:9092` (GitHub Actions service
  container convention)
- Health check: TCP port probe with 60s start period (Kafka startup is slow)

### MailHog
- Image: `mailhog/mailhog:latest`
- Port: `1025:1025`
- Health check: `nc -z localhost 1025`

## Testing Approach

No code changes needed. All existing tests (60 total, all unit tests) continue
to pass. The containers serve as infrastructure for future integration tests.

## Files Changed

- `.github/workflows/build.yml` — add `kafka` and `mailhog` service definitions
