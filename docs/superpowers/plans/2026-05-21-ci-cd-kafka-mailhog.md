# Add Kafka + MailHog to CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Kafka (KRaft mode) and MailHog service containers to GitHub Actions CI workflow.

**Architecture:** Two new service containers in `.github/workflows/build.yml` — Kafka using KRaft mode (single container, no ZooKeeper) and MailHog for SMTP capture. Both use health checks for readiness.

**Tech Stack:** GitHub Actions, Docker, Confluent Kafka 7.5.0, MailHog, Maven

---

### Task 1: Add Kafka + MailHog to GitHub Actions workflow

**Files:**
- Modify: `.github/workflows/build.yml` — add service definitions between `redis` and `steps`

- [ ] **Step 1: Add Kafka and MailHog service containers**

Insert the following service definitions after the `redis` block (after line 35) and before the `steps` block (before line 37):

```yaml
      kafka:
        image: confluentinc/cp-kafka:7.5.0
        env:
          KAFKA_NODE_ID: 1
          KAFKA_PROCESS_ROLES: broker,controller
          KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
          KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
          KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
        ports:
          - 9092:9092
        options: >-
          --health-cmd "bash -c 'echo > /dev/tcp/localhost/9092' || exit 1"
          --health-interval 10s
          --health-timeout 10s
          --health-retries 10
          --health-start-period 60s

      mailhog:
        image: mailhog/mailhog:latest
        ports:
          - 1025:1025
        options: >-
          --health-cmd "nc -z localhost 1025 || exit 1"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

The exact insertion point is after the `redis` service block (ending at line 35 with `--health-retries 5`).

- [ ] **Step 2: Verify YAML is valid**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))"`

Expected: no output (no errors)

- [ ] **Step 3: Commit the CI change**

```bash
git checkout -b ci/add-kafka-mailhog
git add .github/workflows/build.yml
git commit -m "ci: add Kafka and MailHog service containers to GitHub Actions

Add Kafka in KRaft mode (single container, no ZooKeeper) and MailHog
to the CI workflow. This enables future integration tests that depend
on Kafka messaging and email capture.

Kafka uses confluentinc/cp-kafka:7.5.0 with KRaft mode to avoid
inter-service container DNS issues in GitHub Actions networking.
MailHog matches the local dev environment for SMTP capture."
```

- [ ] **Step 4: Push branch**

```bash
git push origin ci/add-kafka-mailhog
```

---

### Related Tasks (Future)

- Add integration tests that use these new CI containers (e.g., notification-service @SpringBootTest with real Kafka)
- Add Kafka health indicator assertion to context-load tests
