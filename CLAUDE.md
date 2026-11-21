# SocialFeed: learning project

Rahul is growing SocialFeed from one Spring Boot service into a 6-service social platform on AWS, to learn microservice, backend, cloud and CI/CD concepts one phase at a time.

- Blueprint (final architecture, request flows, split decision): https://claude.ai/artifact/GP8DCr1xKpdcazg8uwu7gi
- Source roadmaps: `cloud-deployment-mastery-roadmap.md`, `kubernetes_roadmap.md`
- Jira: project ZNSM at https://rahulrx2208.atlassian.net (Java tasks live here; key renamed from KAN, so old KAN-n links redirect to ZNSM-n)

## Working agreement

- **Rahul writes all Java code.** Claude does not write or edit Java under `src/` unless Rahul asks for help with a specific piece.
- **Claude writes and explains config**: YAML, `.env`, docker-compose, Gradle, Dockerfile, Flyway setup, Kubernetes, CI, Terraform. Every config change comes with an explanation of who reads it and how.
- **For Java, Claude gives a task, not the code**: the goal, acceptance criteria and the concept names to study. Hints only when Rahul asks.
- **Teach in depth before building**: the mechanics, which component does what, and why. No bullet summaries in place of explanation.
- **End of every session**: add an entry to the Progress log below and update the Open tasks list.

## Local setup

```powershell
copy .env.example .env          # then fill in real values (first time only)
docker compose up -d            # MySQL on localhost:3307
./gradlew bootRun               # or run SocialFeedApplication from IntelliJ (working dir = project root)
docker compose --profile app up --build   # app + MySQL, both in containers
docker compose down             # stop; add -v to also wipe the database volume
./gradlew test                  # needs only Docker running; starts its own MySQL, no .env needed
```

How to run and test (Windows PowerShell; use `curl.exe`, because plain `curl` is an alias for Invoke-WebRequest):
```powershell
docker compose up -d; docker compose ps          # MySQL must show (healthy)
.\gradlew.bat bootRun                            # ready at "Started SocialFeedApplication"
Invoke-RestMethod http://localhost:8080/actuator/health
# Race test: 10 identical registers at the same instant
'{"username":"race1","email":"race1@example.com","password":"secret123"}' | Out-File -Encoding ascii body.json
curl.exe -s --parallel --parallel-immediate -H "Content-Type: application/json" -d "@body.json" -w " -> HTTP %{http_code}`n" "http://localhost:8080/api/auth/register?try=[1-10]"
```

Config rules:
- Secrets live only in `.env` (git-ignored) locally, and in environment variables / a secret store everywhere else. Never in `application.yaml`.
- `application.yaml` uses `${NAME}` (required, fails startup if missing) for secrets and `${NAME:default}` for safe defaults.
- Local MySQL on the host already uses port 3306, so the Docker MySQL is on 3307.
- Schema changes only through a new Flyway file `src/main/resources/db/migration/V<n>__<description>.sql`. Never edit an applied file (checksum check fails startup). Hibernate runs with `ddl-auto: validate`.

## Phases

0. Clean up the current service: compose, secrets, Flyway, paging, N+1, register race, tests, metrics  ← **current**
1. Split into user-service + post-service + gateway (Gradle multi-project), RS256 JWT + JWKS, follows
2. Kafka events, transactional outbox, feed-service with Redis timelines
3. Idempotency keys, idempotent consumers, interaction-service (likes, concurrency, DynamoDB)
4. Reliability: notification-service, timeouts, retries, circuit breakers, DLQ, rate limiting, chaos
5. Observability: JSON logs, OpenTelemetry traces, metrics, SLOs
6. CI/CD and Kubernetes: GitHub Actions, Helm, Argo CD, canary, rollback, expand/contract migrations
7. AWS foundations with Terraform: VPC, EKS, RDS, ElastiCache, S3, Secrets Manager
8. Traffic and scaling: API Gateway vs ALB, WAF, Route 53, autoscaling, load tests
9. Multi-region and disaster recovery

## Open tasks

Java (Rahul), epic ZNSM-6:
- [x] P0-1 (ZNSM-7) Fix the register race condition (409, never 500)
- [ ] P0-2 (ZNSM-8) Fix N+1 queries when listing posts
- [ ] P0-3 (ZNSM-9) Stop swallowing JWT errors; return 401 with a JSON body
- [ ] P0-4 (ZNSM-10) Cursor paging for `GET /api/posts` + your own `V2__` index migration (unblocked)
- [ ] P0-5 (ZNSM-11) Permit `/actuator/prometheus` in `SecurityConfig` (unblocked)
- [ ] P0-6 (ZNSM-12) Integration tests with Testcontainers, including a concurrent-register test (unblocked)

Config (Claude):
- [x] C-1 docker-compose MySQL + secrets moved to `.env`
- [x] C-2 Flyway: dependency, V1 migration, `ddl-auto: validate`
- [x] C-3 Micrometer Prometheus dependency
- [x] C-4 Testcontainers setup (`TestcontainersConfiguration`, `application-test.yaml`, test profile in Gradle)
- [x] Jira: epic ZNSM-6 + tasks ZNSM-7..12 created

## Progress log

### 2026-11-21: Session 1
- Picked SocialFeed as the learning system and agreed the 10-phase plan and target architecture (blueprint link above).
- Decided to split into user-service + post-service in Phase 1, after the Phase 0 clean-up.
- Reviewed the current code. Found: register check-then-insert race (`AuthService`), N+1 in `PostService.toResponse`, swallowed exceptions in `JwtAuthFilter`, a DB lookup per request in the JWT filter, Prometheus endpoint exposed but the library missing, hardcoded secrets.
- C-1 done: `docker-compose.yml` (MySQL 8.4 on 3307, healthcheck, named volume, optional `app` profile), `.env` / `.env.example`, `application.yaml` now reads secrets from placeholders, `.env` added to `.gitignore` and `.dockerignore`.
- Taught: Spring Boot property source precedence, `${}` placeholders, `spring.config.import`, how compose reads `.env`, container DNS names.
- Connected the Atlassian connector. Created epic ZNSM-6 (Phase 0) with tasks ZNSM-7..12 (P0-1..P0-6), assigned to Rahul, labelled by concept. Rahul is renaming the project key KAN -> ZNSM in Jira settings.
- Rahul's first attempt at P0-1 (ZNSM-7): parallel curl test reproduced the race (1 x 200, 2 x 403). The catch checks the wrong exception type, and the resulting 500 shows up as 403 because the `/error` dispatch is blocked by Spring Security. Added a "How to run and test" section to this file.
- P0-1 finished: Rahul's approach (trust the DB unique constraint, map the failure to 409) was right. At Rahul's request Claude fixed the code: catch `DataIntegrityViolationException` around `saveAndFlush`, re-check which field collided, use `UserNameTaken` (409) for both pre-checks. Also permitted `/error` in `SecurityConfig` so server errors show as 500 instead of 403. Verified: 10 parallel registers -> 1 x 200, 9 x 409.
- Taught: unique-index locking on INSERT, persist vs flush vs commit, save vs saveAndFlush under @Transactional, the `/error` ERROR dispatch, Flyway vs ddl-auto.
- C-2 done: `spring-boot-starter-flyway` + `flyway-mysql`, `V1__create_users_and_posts.sql` with readable constraint names (`uk_users_username`, `uk_users_email`, `fk_posts_user`), `ddl-auto: validate`. Showed Flyway refusing a non-empty schema with no history, then rebuilt the Docker DB (`down -v`, test data only).
- C-3 done: `micrometer-registry-prometheus`; `/actuator/prometheus` serves metrics (403 without a token until ZNSM-11).
- C-4 done: Testcontainers 2.0.5 + `spring-boot-testcontainers`. `TestcontainersConfiguration` (MySQL 8.4 with `@ServiceConnection`), `application-test.yaml` with fake secrets, `spring.profiles.active=test` set in the Gradle test task. `contextLoads` now passes with no `.env` (verified), Flyway V1 runs inside the test container.
- Next: Rahul does ZNSM-8 (N+1), ZNSM-9 (JWT errors), ZNSM-10 (paging + V2 index), ZNSM-11 (Prometheus permit), ZNSM-12 (tests). Phase 0 config work is complete.
