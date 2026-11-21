# Cloud & Deployment Mastery Roadmap for System Design

This is a generalized knowledge map — not tied to any specific job level or
provider. The goal is to be able to walk into any system design conversation
(interview or real work) and reason correctly about infrastructure, not just
recite service names.

## Depth legend

- **[AWARE]** — Know it exists, what problem it solves, when it becomes
  relevant. Enough to not be blindsided by it in a design.
- **[WORKING]** — Can use/configure it correctly, reason about tradeoffs,
  explain common failure modes.
- **[DEEP]** — Can design with it from scratch, debug it under pressure,
  defend tradeoffs against pushback, explain the underlying mechanics, not
  just the API surface.

Language is provider-agnostic. AWS/GCP/Azure names are noted in parentheses
where a concrete anchor helps — theory sticks better when tied to a real
implementation, even if the theory itself is portable.

---

# PART 1 — CLOUD (infrastructure & platform theory)

## 1. Compute Models — [DEEP]
**Subtopics:** VM vs container vs serverless (FaaS) vs bare metal;
virtualization/isolation basics (hypervisor, namespace/cgroup-level
isolation); horizontal vs vertical scaling; reactive vs predictive
autoscaling; cold starts and warm pools; pricing models (on-demand,
reserved/committed, spot/preemptible).

**Why deep:** Every capacity, cost, and latency tradeoff in a system design
traces back to a compute-model decision.

**How to acquire:** Build the *same* small service three ways (raw VM,
container, serverless) and compare cost, cold-start latency, and ops burden
directly, not from memory. Read one cloud provider's architecture
whitepaper end to end (e.g. AWS Well-Architected Framework or GCP's
architecture framework) — they're provider-flavored but the reasoning
transfers.

## 2. Networking — [DEEP]
**Subtopics:** TCP/IP basics as applied to cloud; virtual networks and
subnetting (public/private); route tables, NAT, internet gateways; load
balancing (L4 vs L7, health checks); DNS and service discovery; CDN and
edge caching; egress cost; private connectivity (VPC peering, private
endpoints, transit gateways); security groups (stateful) vs network ACLs
(stateless); service mesh basics (sidecar proxy, mTLS, traffic shifting).

**Why deep:** Networking is the most common source of *silent* failure in
distributed systems — nothing errors, it just times out.

**How to acquire:** Build a multi-tier network by hand at least once
(no infra-as-code the first time — the friction is the lesson). Deliberately
break each layer one at a time (bad security group rule, missing route,
killed NAT) and diagnose by elimination with no hints. Skim one service
mesh's concept docs (Istio or Linkerd) even if you never deploy one.

## 3. Storage & Data Systems — [DEEP relational + NoSQL + object; WORKING warehouse/lake]
**Subtopics:** Object storage (durability model, storage classes, lifecycle
rules); block vs file storage; managed relational DBs (replication,
failover, read replicas, multi-AZ); managed NoSQL (partition keys,
secondary index patterns, consistency models); caching-as-a-service;
data warehouse/lake concepts at a high level; backup/snapshot strategy;
encryption at rest and in transit; storage cost tiers.

**How to acquire:** Design a single-table NoSQL schema for a real
multi-entity domain, access-patterns-first. Stand up cross-region
replication once and watch failover happen. Read the CAP theorem and
PACELC directly from primary/explainer sources rather than secondhand
summaries.

## 4. Identity, Access & Security — [DEEP]
**Subtopics:** RBAC vs ABAC; least-privilege principle; users vs roles vs
service identities; federated identity (OIDC/SAML); secrets management;
cross-account/cross-project trust; key management (envelope encryption
conceptually); network security (WAF, DDoS mitigation); shared
responsibility model; audit logging.

**Why deep:** Permission and secrets failures are both extremely common and
extremely costly — this is one of the few areas where "mostly right" isn't
good enough.

**How to acquire:** Implement least-privilege for a real app from scratch —
no wildcard actions or resources anywhere. Do a cross-account/cross-project
trust exercise end to end. Read the shared-responsibility-model doc of any
major provider once, carefully.

## 5. Distributed Systems Theory — [DEEP]
**Subtopics:** CAP theorem and PACELC; consistency models (strong,
eventual, causal); consensus basics (Paxos/Raft, conceptually); replication
strategies (leader-follower, multi-leader, leaderless); partitioning/
sharding strategies; idempotency; distributed transactions and the saga
pattern; ordering and clock skew (logical/vector clocks); quorum reads and
writes.

**Why deep:** This is the actual theoretical backbone behind every
cloud-scale design question — services are just an implementation of these
ideas.

**How to acquire:** Read the Dynamo paper and the Raft paper (or a strong
explainer of each). Implement a toy leader-election or an idempotent
consumer by hand. For every managed service you use, explicitly ask "which
of these distributed-systems concepts is this service an implementation
of?" and answer it.

## 6. Messaging & Asynchronous Architecture — [DEEP]
**Subtopics:** Queue (point-to-point) vs topic (pub/sub); fan-out patterns;
delivery guarantees (at-least-once, at-most-once, exactly-once — and why
exactly-once is usually a myth in practice); dead-letter queues; visibility/
ack timeouts; backpressure; event-driven architecture; event sourcing and
CQRS at a conceptual level; log-based streaming (partitioning, consumer
groups, offsets).

**Why deep:** Nearly every design that needs to scale past a single request/
response pair leans on an async backbone.

**How to acquire:** Build a fan-out pipeline with a dead-letter queue and
genuinely idempotent consumers (test it by deliberately duplicating a
message). Read a streaming platform's design doc (Kafka's is excellent)
even if you never run one yourself.

## 7. Observability — [DEEP]
**Subtopics:** The three pillars — metrics, logs, traces; structured
(queryable) logging vs raw text; custom metrics; dashboards; alerting
design (SLIs, SLOs, error budgets); distributed tracing and context
propagation; log-query languages; basic anomaly detection.

**Why deep:** You cannot operate or debug a cloud-scale system without this
— it's the difference between guessing and knowing.

**How to acquire:** Instrument one real service with structured logs, a
custom metric, and a trace. Write real log-query-language queries against
your own logs, not tutorial data. Deliberately break something and force
yourself to diagnose it using *only* observability tooling — no reading the
source to shortcut it.

## 8. Cost & Capacity Planning — [WORKING, leaning DEEP]
**Subtopics:** Pricing models across compute/storage/network/data-transfer;
egress cost traps; reserved/spot/committed-use tradeoffs; back-of-envelope
capacity estimation (QPS, storage growth per year, bandwidth); cost
tagging/observability; how autoscaling policy affects your bill.

**How to acquire:** Do real back-of-envelope math for a system you design —
users, requests/sec, storage growth, bandwidth — and sanity-check it
against a provider's pricing calculator. This single habit is worth more
than reading pricing pages.

## 9. Reliability Engineering — [DEEP]
**Subtopics:** Availability math (what 99.9% vs 99.99% actually means in
downtime-per-year); eliminating single points of failure; failover
strategy (active-active vs active-passive); health checks; circuit
breakers; retries with exponential backoff and jitter; bulkheading;
graceful degradation; chaos engineering basics; disaster recovery
(RTO/RPO, backup strategy, multi-region DR).

**Why deep:** Reliability reasoning is often the single biggest
differentiator between a junior and senior answer in a system design
discussion.

**How to acquire:** Calculate the composite availability of a real
multi-component system on paper. Implement a circuit breaker and
retry-with-jitter yourself in code, don't just use a library blindly.
Read one chaos-engineering writeup (Netflix's Chaos Monkey material is a
good start).

## 10. Multi-Region & Global Architecture — [WORKING]
**Subtopics:** Latency-based routing; data residency/sovereignty
constraints; active-active vs active-passive across regions; conflict
resolution across regions; global load balancing; edge computing basics.

**How to acquire:** Read 2–3 real multi-region case studies from
engineering blogs. Work through a design exercise on paper — this topic
rewards reasoning more than hands-on building, unless your specific work
demands the hands-on depth.

---

# PART 2 — DEPLOYMENT (delivery & operations theory)

## 1. Version Control & Branching Strategy — [WORKING]
**Subtopics:** Trunk-based development vs GitFlow-style branching; feature
flags as a decoupling tool; code-review gating.

**How to acquire:** Actually practice both a trunk-based and a
longer-lived-branch workflow on a real repo, so the tradeoffs aren't
theoretical.

## 2. Continuous Integration (CI) — [DEEP]
**Subtopics:** Build pipelines; automated test stages (unit, integration,
e2e); artifact creation and versioning; pipeline-as-code.

**Why deep:** Everything downstream (CD, deployment strategy, rollback)
assumes CI is solid — a shaky foundation here breaks everything above it.

**How to acquire:** Build one complete CI pipeline from scratch for a real
project, including at least two test stages and artifact publishing.

## 3. Containerization — [DEEP]
**Subtopics:** What a container actually is, conceptually (namespaces/
cgroups, not "a lightweight VM"); Dockerfile best practices (layer caching,
multi-stage builds, image size); image registries; image scanning basics;
container runtime fundamentals.

**How to acquire:** Containerize a real service, deliberately optimize the
image size (measure before/after), push to a registry, run it. Read why
multi-stage builds exist by hitting the problem they solve firsthand.

## 4. Container Orchestration — [DEEP concepts, WORKING hands-on]
**Subtopics:** What an orchestrator actually solves — scheduling,
self-healing, service discovery, scaling; core Kubernetes concepts (pods,
deployments, services, ingress, config/secrets, namespaces, resource
requests/limits, readiness/liveness probes, horizontal pod autoscaling);
managed alternatives (Fargate-style, Cloud Run-style) and when they beat
raw Kubernetes.

**How to acquire:** Deploy a real service on a managed container platform
(managed Kubernetes or a simpler managed container service — either is
fine for the concepts). Break a pod on purpose (bad probe, tight resource
limit) and diagnose it without hints.

## 5. Continuous Delivery/Deployment (CD) — [DEEP]
**Subtopics:** Deployment strategies (rolling, blue-green, canary,
feature-flag-gated); rollback strategy per deployment type; automated
promotion across environments; GitOps concepts.

**Why deep:** This is the actual mechanism by which "code to production
safely" happens — the whole point of the deployment lifecycle.

**How to acquire:** Wire a pipeline that auto-deploys on push. Then
deliberately push a broken build and practice rolling it back — doing this
once for real teaches more than reading ten articles about it.

## 6. Infrastructure as Code (IaC) — [DEEP]
**Subtopics:** Declarative vs imperative IaC; state management and drift;
modularity/reusability of infra code; idempotency of infra changes;
working fluency with at least one tool (Terraform, CDK, CloudFormation,
Pulumi — the concepts transfer across all of them).

**How to acquire:** Rebuild something you previously created by hand
(console/click-ops) as code instead. Deliberately introduce drift (change
something out-of-band) and practice reconciling it.

## 7. Configuration & Secrets Management — [WORKING]
**Subtopics:** Environment-specific configuration; secrets injection
(never hardcoded); config-as-code; feature flags/toggles as a deployment
mechanism, not just a testing tool.

**How to acquire:** Wire a real secrets manager into an actual deployment
pipeline rather than reading about the pattern.

## 8. Testing in the Deployment Pipeline — [WORKING]
**Subtopics:** Test pyramid (unit/integration/e2e/contract); post-deploy
smoke tests; load/performance testing; fault-injection testing inside
pipelines.

**How to acquire:** Add one smoke test and one basic load test to a real
CI/CD pipeline and watch them actually catch (or fail to catch) a bad
deploy.

## 9. Release Management & Rollback Strategy — [DEEP]
**Subtopics:** Versioning strategy (semver, immutable artifacts); rollback
mechanics specific to each deployment strategy; coordinating database
migrations with code deploys (backward-compatible schema changes, the
expand/contract pattern).

**Why deep:** Deploy/migration coordination is a classic real-world failure
point that most tutorials skip entirely.

**How to acquire:** Practice one expand/contract schema migration alongside
a rolling deploy — add a column, deploy code that writes both old and new,
backfill, then deploy code that reads only new, then drop the old column.

---

# PART 3 — COMBINED (where cloud and deployment meet)

These are the topics that only make sense once you have both halves —
they're often the actual substance of a senior-level design discussion.

## 1. Zero-Downtime Deployment Mechanics — [DEEP]
How load balancer health checks, orchestrator/ASG readiness, and
deployment strategy (rolling/canary/blue-green) all interlock to make a
deploy invisible to users. **Acquire:** trace one real deploy end to end
and identify exactly which component would notice if any single piece
were missing.

## 2. Immutable Infrastructure — [WORKING]
Baking images/artifacts vs mutating live servers; why immutability
simplifies rollback and debugging. **Acquire:** build one image-baking
pipeline (AMI or container image) and compare it to a mutate-in-place
approach you've done before.

## 3. Environment Strategy — [WORKING]
Dev/staging/prod parity; isolated accounts/projects per environment;
promotion flow between them. **Acquire:** set up a real second environment
and promote a change through it deliberately.

## 4. Service Mesh & Traffic Management — [AWARE, WORKING if platform team role]
Canary traffic-splitting, mTLS between services, traffic shifting as a
deployment primitive rather than just a networking one.

## 5. Multi-Account/Multi-Project Cloud Organization — [AWARE]
Landing zones, account-per-environment patterns, org-level guardrails.
Mostly a "know the pattern exists" topic unless you're doing platform/infra
work directly.

## 6. Disaster Recovery + Pipeline Coordination — [WORKING]
Can your entire stack be rebuilt from IaC, in a new region, inside your
RTO? **Acquire:** actually attempt a from-scratch redeploy of something
you've built, timing it, treating the timer as your RTO test.

## 7. Cost-Aware Deployment — [WORKING]
Right-sizing pipeline resources; autoscaling policy tied to real traffic
patterns rather than guesses. **Acquire:** tune one autoscaling policy
against real load-test data instead of default settings.

## 8. Security Throughout the Pipeline ("shift-left") — [WORKING]
Image scanning, least-privilege IAM roles for CI/CD runners themselves
(not just the app), supply-chain/SBOM basics. **Acquire:** audit one real
CI/CD pipeline's permissions and remove anything broader than necessary.

---

# PART 4 — Practice Problems

Classic system design problems, chosen to each stress a different cluster
of the topics above:

1. **URL shortener** — compute model choice, IAM scoping, simple storage
2. **Rate limiter** — algorithms + distributed coordination across nodes
3. **Distributed file storage (S3-like)** — durability, partitioning,
   consistency
4. **News feed / fan-out system** — messaging, fan-out-on-write vs
   fan-out-on-read
5. **Chat / real-time messaging system** — connection state, ordering,
   presence
6. **Distributed cache** — consistency, eviction, cache stampede
7. **Payment processing system** — idempotency, distributed transactions,
   exactly-once illusions
8. **Ride-sharing dispatch** — geo-partitioning, real-time matching at scale
9. **Video streaming platform** — CDN strategy, adaptive delivery, storage
   tiers
10. **Notification system** — multi-channel fan-out, retries, delivery
    guarantees
11. **E-commerce inventory system** — concurrency control, consistency
    under contention
12. **Global multi-region deployment for an existing single-region app** —
    reliability + combined-topic focus
13. **CI/CD pipeline design for a microservices org** — pure deployment
    focus
14. **Monitoring/alerting platform design** — observability focus
15. **Debugging drills** — given a deliberately broken deployment or
    network scenario, diagnose with no source-reading shortcuts (do this
    repeatedly; it's the single highest-yield practice format)

For each, practice explaining not just the *design* but the *failure
modes* — what breaks first under 10x load, what happens if one AZ/region
goes down, what happens if a deploy goes out mid-traffic-spike.

---

# PART 5 — Roadmap (sequencing, not calendar days)

Knowledge compounds in a specific order — each phase leans on the one
before it, so this is deliberately sequential rather than parallel.

### Phase 1 — Foundations
Compute models → Networking → Storage → IAM/Security.
*Exit criteria: can explain, from memory, why a request from a browser to
a database actually works — every hop, every layer.*

### Phase 2 — Distributed Systems Core
Distributed systems theory → Messaging/async → Observability.
*Exit criteria: can map any managed service you use back to the
distributed-systems concept it implements.*

### Phase 3 — Deployment Fundamentals
Version control strategy → CI → Containerization → CD.
*Exit criteria: have taken one real change from a git commit to a running
production-like environment, fully automated, at least once.*

### Phase 4 — Orchestration & Infrastructure as Code
Container orchestration → IaC → Config/secrets management.
*Exit criteria: can rebuild a system you previously built by hand,
entirely from code, and explain every resource in it.*

### Phase 5 — Reliability, Scale & Combined Topics
Reliability engineering → Multi-region → Cost/capacity planning → all of
Part 3 (combined topics).
*Exit criteria: given any design, can immediately name its single points
of failure and its cost drivers without being prompted.*

### Phase 6 — Integration Practice
Work through the Part 4 practice problems, alternating between "design
from scratch" and "diagnose a broken system." Weight debugging drills
heavily — they expose gaps that pure design exercises hide.

Two habits matter more than any single topic on this list: (1) always do
the back-of-envelope math instead of skipping to a service name, and
(2) always ask "what fails first, and how would I know?" before calling
any design finished.
