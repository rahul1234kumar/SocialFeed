# Kubernetes Roadmap: Beginner to Interview-Ready Expert (12 Weeks)

**Learner:** Rahul
**Goal:** Understand Kubernetes deeply enough to answer most interview questions and to work confidently with the company pipeline (GitHub Actions → registry → Harness → OpenShift).
**Style:** Every topic follows the same 4 steps:

1. **Theory** – what it is and why it exists (academic, plain language)
2. **Simple example** – the smallest thing that shows the idea
3. **Enterprise view** – how big companies and large apps use it
4. **Your turn** – questions I ask you, answered in chat before we move on

> Plain-language rule: every short form is spelled out the first time it appears. A glossary is at the end.

---

# Plan Review and Project Adaptation (read this first)

## Verdict on the original plan

**The plan is good.** The 4-step rhythm (theory, small example, enterprise view, your turn), the week order (containers, Pods, Deployments, networking, config, storage, scaling, security, Helm/CI-CD, observability, advanced), the interview Q/A and the mapping to your company pipeline (GitHub Actions, registry, Harness, OpenShift) are all sound. The order is the right one for interviews and for daily work.

**What was wrong for *our* situation (all fixed in this version):**

| # | Problem in the original | Fix applied |
| - | ----------------------- | ----------- |
| 1 | Demo app was a made-up `socialfeed-service` (Maven, Redis, `/api/hello`) | Everything now uses the real **SocialFeed** project (Gradle, MySQL, JWT) |
| 2 | Redis StatefulSet in Week 6 | **MySQL** StatefulSet, because SocialFeed really needs MySQL |
| 3 | Health probes on `/actuator/health/...` | SocialFeed has **no Actuator** and Spring Security blocks everything except `/api/auth/**`. Pods would never become Ready (HTTP 403). Week 0 patches fix this, and Week 10 turns the mistake into a drill |
| 4 | `pom.xml`, `mvn`, JDK 21 | `build.gradle`, `gradlew`, and the JDK in `build.gradle` (currently 25) |
| 5 | Secrets example was a toy `DB_PASSWORD` | The real hardcoded DB password and JWT signing secret in `application.yaml` become the ConfigMap/Secret lesson |
| 6 | Commands written for Windows CMD; `curl` | You use **PowerShell**, where `curl` is an alias of `Invoke-WebRequest`. Examples now use `Invoke-RestMethod` / `curl.exe` |
| 7 | The app depends on a database from day one | Added a throwaway MySQL in Weeks 1 to 3 so every week has a working app |
| 8 | Typos: broken Java generics (`Mapstring,`), a split table row in Appendix B, stray HTML at the end, `oc rollout status dc/...` (DeploymentConfig is deprecated) | Corrected |
| 9 | OpenShift appears in only one week | Kept the scope, but Week 9 now says to rehearse on the free Red Hat Developer Sandbox |

**Honest caveats:** 6 to 8 hours a week for 12 weeks is demanding. If time is short, Weeks 1 to 5 plus 9 and 10 give the most value for interviews and for reading your company `values.yml`. Weeks 7, 8 and 11 can be shortened.

## What SocialFeed gives us

| Fact about the project | Kubernetes lesson it creates |
| ---------------------- | ---------------------------- |
| Stateless REST API on port 8080, JWT in the `Authorization` header | Perfect for a Deployment with several replicas. Any Pod can serve any user |
| Needs MySQL (`localhost:3306/socialfeed`, user `root`) | Service DNS name instead of `localhost`, StatefulSet + PVC, startup ordering |
| `spring.datasource.*` and `app.jwt.secret` hardcoded in `application.yaml` | ConfigMap and Secret (Spring maps `SPRING_DATASOURCE_URL`, `APP_JWT_SECRET` environment variables automatically, no code change) |
| `ddl-auto: update` creates tables on start | Several Pods starting on an empty DB can race. Real teams use Flyway or Liquibase (Week 6 note) |
| Login uses BCrypt (CPU heavy) | A natural load generator for the HPA in Week 7 |
| `spring-boot-starter-kafka` present but unused | Optional Kafka topics in Week 11 |
| `contextLoads()` test needs a live MySQL | The Docker build and CI skip tests (`-x test`) or start a MySQL service |

## Week 0: small patches to the project (infrastructure only, no new features)

Do these once, from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`, and confirm `.\gradlew build` still passes (with MySQL running).

1. **`build.gradle`**: add `implementation 'org.springframework.boot:spring-boot-starter-actuator'` (gives `/actuator/health/liveness` and `/readiness`). In Week 10 also add `runtimeOnly 'io.micrometer:micrometer-registry-prometheus'`.
2. **`application.yaml`**: add

   ```yaml
   management:
     endpoint:
       health:
         probes:
           enabled: true
     endpoints:
       web:
         exposure:
           include: health,info,prometheus
   ```

3. **`SecurityConfig.java`**: kubelet probes are anonymous, so open the health endpoints (and the optional host endpoint below):

   ```java
   .requestMatchers("/api/auth/**", "/api/public/**", "/actuator/health/**").permitAll()
   ```

4. **Optional demo endpoint** (so you can *see* which Pod answered during load balancing and rolling updates). New file `controller/HostController.java`:

   ```java
   @RestController
   public class HostController {
       @GetMapping("/api/public/host")
       public Map<String, String> host() throws UnknownHostException {
           return Map.of("host", InetAddress.getLocalHost().getHostName());
       }
   }
   ```

5. **Git**: the repository has no commit yet and the branch is `master` while the workflow below expects `main`. Make a first commit and use one branch name (`git branch -M main`). Add a `.dockerignore` (see Week 1).
6. **Keep versions in sync**: `build.gradle` has `JavaLanguageVersion.of(25)`, so the Dockerfile uses a Java 25 image. If you change one, change the other.
---

## 0. How We Will Work

### Weekly rhythm (about 6 to 8 hours per week)


| Step | What happens                                 | Time   |
| ---- | -------------------------------------------- | ------ |
| 1    | Read theory in chat (I explain, you ask)     | 1 hr   |
| 2    | Do the simple example by hand                | 1.5 hr |
| 3    | Deploy the next version of our running app   | 2 hr   |
| 4    | Enterprise view and pipeline mapping         | 1 hr   |
| 5    | Your-turn questions + interview Q/A practice | 1.5 hr |
| 6    | Write a 5-line summary in your own words     | 0.5 hr |

### The running app: `SocialFeed` (our real project)

A Spring Boot (Java) REST API: register, login (JWT), create posts, list posts. It talks to MySQL. It grows a little every week, so each new Kubernetes idea has a real thing to attach to.


| Week | What the app gains                                                         | Kubernetes idea it teaches  |
| ---- | -------------------------------------------------------------------------- | --------------------------- |
| 1    | Docker image of SocialFeed, running with a MySQL container                 | Containers                  |
| 2    | App + MySQL in one Pod (shared `localhost`)                                | Pod, kubectl, cluster parts |
| 3    | 3 app copies, health probes, rolling update, rollback                      | Deployment                  |
| 4    | Reachable by name inside, and from Postman outside                         | Service, Ingress            |
| 5    | DB URL, DB password and JWT secret come from outside the image             | ConfigMap, Secret           |
| 6    | MySQL keeps users and posts after its Pod is deleted                       | Volumes, StatefulSet        |
| 7    | Auto adds copies under login load                                          | Autoscaling, scheduling     |
| 8    | Locked down: non-root, RBAC, only the app may reach MySQL                  | RBAC, NetworkPolicy         |
| 9    | Packaged with Helm, built by GitHub Actions, mapped to Harness / OpenShift | Helm, GitOps, Harness       |
| 10   | Metrics, logs, break-and-fix drills (wrong DB password, probe blocked)     | Observability, debugging    |
| 11   | Graceful shutdown, Kafka, upgrades                                         | Advanced topics             |
| 12   | Full capstone + mock interview                                             | Everything                  |

### Tools to install (Week 1)

- Docker Desktop (runs containers on Windows)
- `kubectl` (the command-line tool, called "kube-control", that talks to Kubernetes)
- `minikube` (a tiny Kubernetes cluster on your laptop)
- `helm` (package manager for Kubernetes, used from Week 9)
- JDK (the version in `build.gradle`, currently 25). No Maven needed: the project ships the Gradle wrapper (`gradlew`)
- Postman (or PowerShell `Invoke-RestMethod`) to call the API
- Optional: OpenShift Local or the free Red Hat Developer Sandbox, to practice `oc` (the OpenShift command-line tool)

### Folder layout used in all commands

We work directly inside the real project:

```
C:\Users\rahul\Downloads\Home_Project\SocialFeed\
  build.gradle, gradlew, src\...   <- the Java app (already exists)
  Dockerfile, .dockerignore         <- added in Week 1
  k8s\                              <- all Kubernetes YAML files
  helm\                             <- Helm chart (Week 9)
  .github\workflows\                <- GitHub Actions (Week 9)
```

All commands below say which folder to run them from and use **PowerShell** syntax (the folder paths work in CMD too).

### Progress tracker


| Week | Topic                             | Done | Confidence (1-5) |
| ---- | --------------------------------- | ---- | ---------------- |
| 1    | Containers and Docker             | [ ]  |                  |
| 2    | Kubernetes architecture, Pods     | [ ]  |                  |
| 3    | Deployments and updates           | [ ]  |                  |
| 4    | Networking                        | [ ]  |                  |
| 5    | Config and Secrets                | [ ]  |                  |
| 6    | Storage and stateful apps         | [ ]  |                  |
| 7    | Scaling and scheduling            | [ ]  |                  |
| 8    | Security                          | [ ]  |                  |
| 9    | Helm, CI/CD, GitOps, OpenShift    | [ ]  |                  |
| 10   | Observability and troubleshooting | [ ]  |                  |
| 11   | Advanced and expert topics        | [ ]  |                  |
| 12   | Capstone and mock interviews      | [ ]  |                  |

---

# WEEK 1: Containers and Docker (the foundation)

**Why first:** Kubernetes manages containers. If containers are unclear, everything after is confusing.

## 1.1 Theory: the problem containers solve

- **Old problem:** "It works on my machine." Different laptops and servers have different Java versions, libraries, and settings.
- **Virtual machine (VM):** a full fake computer with its own operating system. Safe, but heavy and slow to start (minutes, gigabytes).
- **Container:** a normal process on the host machine, but wrapped so it only sees its own files, its own network, and limited CPU and memory. Starts in seconds, size in megabytes.
- **How the wrapping works (Linux features):**
  - **Namespaces**: give a process its own private view (own process list, own network, own file system). This is *isolation*.
  - **cgroups** (control groups): limit how much CPU and memory a process can use. This is *limits*.
- **Image vs container:** An image is a frozen, read-only package (the recipe). A container is a running copy of an image (the cooked dish). One image can start many containers.
- **Image layers:** An image is built in layers (base OS, Java, your jar). Layers are cached, so rebuilds are fast and shared layers are stored once.
- **Registry:** a storage and download service for images (Docker Hub, Amazon ECR, Azure Container Registry, Quay, JFrog Artifactory).
- **Tags and digests:** `socialfeed-service:1.0` is a tag (a movable label). `sha256:abc...` is a digest (a fixed fingerprint). Enterprises prefer digests or unique tags, never `latest`.

## 1.2 Simple example: containerize SocialFeed

No new Java code is needed. We package the existing app. Create `Dockerfile` in the project root, a "multi-stage" build (first stage builds, second stage is a small final image):

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
# gradlew edited on Windows may have CRLF line endings, which Linux cannot run
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# tests need a live MySQL, so skip them inside the image build
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
# the plain "-plain.jar" is not runnable, so match only the Spring Boot jar
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

(If a `25-jdk` or `25-jre` tag is not found, check the tags on Docker Hub for `eclipse-temurin`. The Java version must match the toolchain in `build.gradle`.)

`.dockerignore` (keeps the build context small and secrets/IDE files out of the image):

```
.git
.gradle
.idea
build
k8s
helm
```

**Commands** (run from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`). SocialFeed needs MySQL, so we start one container for it on a shared Docker network:

```
docker build -t socialfeed-service:1.0 .
docker network create sfnet
docker run -d --name mysql --network sfnet -e MYSQL_ROOT_PASSWORD=rootpw -e MYSQL_DATABASE=socialfeed mysql:8
docker run --rm --name app --network sfnet -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/socialfeed -e SPRING_DATASOURCE_PASSWORD=rootpw socialfeed-service:1.0
```

(MySQL needs 20 to 30 seconds on first start. If the app exits with a connection error, wait and run the last command again. That is your first lesson in startup order.)

In a second PowerShell window (any folder):

```
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType application/json -Body '{"username":"rahul","email":"r@x.com","password":"secret1"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/posts -ContentType application/json -Headers @{Authorization="Bearer $($r.token)"} -Body '{"content":"hello containers"}'
Invoke-RestMethod -Uri http://localhost:8080/api/posts -Headers @{Authorization="Bearer $($r.token)"}
```

Notes: `-e SPRING_DATASOURCE_URL=...` overrides `spring.datasource.url` from `application.yaml`. This is Spring Boot's "relaxed binding", and it is the mechanism we use again in Week 5 with ConfigMaps and Secrets.

**Try this (proves Q5 below):** run `docker rm -f mysql`, start MySQL again with the same command, restart the app and try to log in as `rahul`. The user is gone, because the database files lived inside the deleted container. Clean up with `docker rm -f mysql` and `docker network rm sfnet`.

## 1.3 Enterprise view

- Every service at a big company ships as an image built by an automated pipeline (not on a developer laptop).
- Images are scanned for known security holes (tools such as Trivy, Snyk, Prisma) before they are allowed into the registry.
- Teams use small, approved **base images** (for example "distroless" or Red Hat Universal Base Image) so security teams only patch a few images.
- Images are signed and stored in a private registry; clusters are set to pull only from approved registries.
- **Company pipeline link:** this is your GitHub Actions step: code push → build image → push to registry.

## 1.4 Key concepts

Container, image, layer, registry, tag, digest, namespace (Linux), cgroup, multi-stage build, base image.

## 1.5 Interview Q/A

**Q1. Container vs VM?**
A VM virtualizes hardware and runs a full guest operating system. A container shares the host's kernel and only isolates the process, so it is lighter and faster. Trade-off: containers have weaker isolation than VMs.

**Q2. What makes a container isolated?**
Linux namespaces (isolation of what a process can see) and cgroups (limits on what it can use).

**Q3. Why multi-stage builds?**
The final image contains only the runtime and the jar, not Maven or source code. Smaller, faster to pull, smaller attack surface.

**Q4. Why not use the `latest` tag in production?**
It is a moving label. You cannot tell which code is running, and rollbacks become unreliable. Use immutable version tags or digests.

**Q5. If a container is deleted, what happens to files written inside it?**
They are lost, because the writable layer dies with the container. Persistent data needs a volume (Week 6).

## 1.5b Your turn (answer in chat)

1. In your own words, what is the difference between an image and a container?
2. If 10 containers use the same base image, how many copies of that base image are stored on disk, and why?
3. Why might a bank prefer a digest over a tag?

## 1.6 Summary

A container is an isolated process started from an image. Images are layered, stored in registries, and identified by tags or digests. Kubernetes will run and manage these containers for us.

---

# WEEK 2: Kubernetes Architecture and the Pod

## 2.1 Theory: why Kubernetes exists

- Running 1 container is easy. Running 500 containers across 40 machines is not: who restarts a crashed one? Who decides which machine has space? How do containers find each other when their addresses keep changing?
- **Kubernetes (K8s: "K", 8 letters, "s")** is a *container orchestrator*: you tell it the **desired state** ("I want 3 copies of this app running") and it works continuously to make the **actual state** match. This idea is called the **control loop** or **reconciliation**. It is the single most important idea in Kubernetes.
- Origin: Google ran an internal system called Borg for years. Kubernetes (2014) is an open-source system built from those lessons, now maintained by the Cloud Native Computing Foundation (CNCF).

## 2.2 Theory: architecture

**Control plane** (the brain), usually 3 machines for safety:


| Part                       | Job (plain words)                                                                                           |
| -------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `kube-apiserver`           | The front door. Every command and every component talks through it. Checks who you are and what you may do. |
| `etcd`                     | The database. Stores the whole cluster state. Lose it without a backup and you lose the cluster's memory.   |
| `kube-scheduler`           | Chooses which worker machine (node) a new Pod should run on.                                                |
| `kube-controller-manager`  | Runs the control loops (for example "are there 3 copies? no, 2, create 1").                                 |
| `cloud-controller-manager` | Talks to the cloud provider (load balancers, disks).                                                        |

**Worker nodes** (the muscle):


| Part                                        | Job                                                                                      |
| ------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `kubelet`                                   | Agent on each node. Starts containers as told and reports health.                        |
| Container runtime (`containerd` or `CRI-O`) | Actually runs the containers. (CRI = Container Runtime Interface.) OpenShift uses CRI-O. |
| `kube-proxy`                                | Sets up network rules so Services work (Week 4).                                         |

**Flow when you run `kubectl apply`:**
`kubectl` → API server (checks, saves to etcd) → scheduler picks a node → kubelet on that node starts the container → status is reported back.

## 2.3 Theory: Pod, Namespace, Labels

- **Pod:** the smallest thing Kubernetes runs. One or more containers that share one network address and can share disks. Usually **one main container per Pod**. Extra helper containers are called **sidecars**.
- Pods are **disposable (mortal)**: they die and are replaced with a new name and new IP. Never depend on a Pod's IP.
- **Namespace:** a named folder inside the cluster to separate teams or environments (`dev`, `qa`, `payments`). OpenShift calls this a **Project**.
- **Labels:** key-value tags on objects (`app: socialfeed`). **Selectors** find objects by label. This is how Kubernetes connects things loosely.
- **Declarative vs imperative:** *Imperative* = "do this step". *Declarative* = "here is the end result I want" in a YAML file (YAML: a simple indented text format for configuration). Enterprises use declarative files stored in Git.

## 2.4 Simple example: run the app as one Pod

Start the cluster (from any folder):

```
minikube start --driver=docker --cpus=4 --memory=6144
kubectl get nodes
```

Load your image into minikube (from any folder):

```
minikube image load socialfeed-service:1.0
```

Create `k8s\pod.yaml` in the project folder. This Pod has **two containers**: the app and MySQL. Containers in one Pod share one network, so the app reaches MySQL on `localhost`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: socialfeed-pod
  labels:
    app: socialfeed
spec:
  containers:
    - name: app
      image: socialfeed-service:1.0
      imagePullPolicy: IfNotPresent
      ports:
        - containerPort: 8080
      env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mysql://localhost:3306/socialfeed
        - name: SPRING_DATASOURCE_PASSWORD
          value: rootpw
    - name: mysql
      image: mysql:8
      env:
        - name: MYSQL_ROOT_PASSWORD
          value: rootpw
        - name: MYSQL_DATABASE
          value: socialfeed
```

(Passwords in plain YAML are acceptable only for this throwaway lab. Week 5 fixes this.)

Commands (run from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl apply -f k8s\pod.yaml
kubectl get pods
kubectl describe pod socialfeed-pod
kubectl logs socialfeed-pod -c app
kubectl logs socialfeed-pod -c mysql
kubectl port-forward pod/socialfeed-pod 8080:8080
```

Now delete it and watch:

```
kubectl delete pod socialfeed-pod
kubectl get pods
```

Notice: nobody recreates it. A bare Pod has no manager. That is why Week 3 exists.
Two things to notice while it runs:

- `kubectl get pods` may show the `app` container with `RESTARTS` of 1 or 2 at first. MySQL takes about 30 seconds to be ready and the app gave up when it could not connect, so Kubernetes restarted it. Look at `kubectl logs socialfeed-pod -c app --previous` to see the connection error. This is the "wait for the database" problem. An init container (Q6 below) or a readiness check is the proper answer.
- The app calls `localhost:3306` and it works, because containers in a Pod share one network. In production, the database is **not** put in the same Pod as the app (they could not scale independently). We separate them from Week 3 on.

## 2.5 Enterprise view

- Nobody runs bare Pods in production. Pods are created by higher-level objects (Deployments and others).
- Clusters are split by namespace per team, with **resource quotas** (a cap on how much CPU and memory a namespace may use).
- Companies commonly run several clusters (dev, test, prod, per region) instead of one giant one. Managed services such as Amazon EKS (Elastic Kubernetes Service), Google GKE, and Azure AKS run the control plane for them. Banks often use Red Hat OpenShift.
- **Company pipeline link:** OpenShift Container Platform (OCP) is Kubernetes plus extra features (built-in security rules, web console, routes, builds). Everything you learn here applies there.

## 2.6 Key concepts

Desired vs actual state, control loop, API server, etcd, scheduler, kubelet, container runtime, Pod, namespace, label, selector, declarative YAML.

## 2.7 Interview Q/A

**Q1. Explain Kubernetes architecture.**
Control plane (API server, etcd, scheduler, controller manager) decides; worker nodes (kubelet, runtime, kube-proxy) run Pods. All communication goes through the API server.

**Q2. What happens when you run `kubectl apply -f pod.yaml`?**
kubectl sends the request to the API server → it authenticates, authorizes, validates, and saves to etcd → scheduler assigns a node → kubelet on the node asks the runtime to pull the image and start the containers → kubelet reports status back to the API server.

**Q3. What is etcd and why is it critical?**
A consistent key-value database storing all cluster state. If it is lost without a backup, the cluster loses its configuration. Production setups run 3 or 5 etcd members and take regular backups.

**Q4. Why is a Pod "mortal"?**
Pods are replaced, never repaired. A replacement gets a new name and IP. So we use Services (Week 4) for stable addressing.

**Q5. Why can a Pod have multiple containers?**
For tightly coupled helpers (sidecars): log shippers, proxies, or init tasks. They share network and storage.

**Q6. What is an init container?**
A container that must finish successfully before the main containers start. Example: wait for the database to be ready, or run a migration.

## 2.7b Your turn

1. You say "I want 3 copies" and one node crashes taking 1 copy with it. Which part notices, and which part fixes it?
2. Why do you think Kubernetes uses labels instead of fixed names to connect objects?
3. What is the difference between `kubectl create` and `kubectl apply`? (Think: imperative vs declarative.)

## 2.8 Summary

Kubernetes keeps actual state equal to desired state using control loops. The control plane decides, nodes execute, and the API server is the center. A Pod is the smallest unit but is mortal, so we manage Pods through higher-level objects.

---

# WEEK 3: Workloads: ReplicaSet, Deployment, Updates, Health Checks

## 3.1 Theory

- **ReplicaSet:** keeps N identical Pods alive. If one dies, it makes another.
- **Deployment:** manages ReplicaSets and gives **rolling updates** and **rollbacks**. This is what you use for stateless apps (apps that keep no important data inside themselves).
- **Rolling update:** replace old Pods with new ones a few at a time, so users see no downtime. Controlled by `maxSurge` (how many extra Pods allowed during update) and `maxUnavailable` (how many may be missing).
- **Rollback:** the Deployment keeps old ReplicaSets, so going back is quick.
- **Other deployment strategies (interview favorites):**
  - **Recreate**: kill all old, start all new (downtime).
  - **Blue/Green**: run two full environments, switch traffic at once.
  - **Canary**: send a small percent (say 5%) of traffic to the new version, then grow.
- **Probes (health checks):**
  - **Liveness**: "Is the app stuck? If yes, restart it."
  - **Readiness**: "Is the app ready for traffic? If no, remove it from the traffic list but do not restart."
  - **Startup**: "Give slow-starting apps extra time before the other probes begin." (Java apps are often slow to start.)
- **Resources:** `requests` = what the scheduler reserves for the Pod. `limits` = the maximum allowed. Over the memory limit → the container is killed (`OOMKilled`, Out Of Memory killed). Over the CPU limit → slowed down (throttled), not killed.

## 3.2 Simple example

Apply the Week 0 patches first (Actuator dependency, `management` settings in `application.yaml`, and `permitAll` for `/actuator/health/**` in `SecurityConfig`), then rebuild: `docker build -t socialfeed-service:1.0 .` and `minikube image load socialfeed-service:1.0`. Spring Boot then serves `/actuator/health/liveness` and `/actuator/health/readiness`. Those are the URLs the kubelet will probe.

**Step 0: a temporary MySQL.** The app needs a database, and Services are only explained next week, so for now treat the name `mysql` as "a stable address". Create `k8s\mysql-temp.yaml` (we throw it away in Week 6 and replace it with a proper StatefulSet):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: rootpw
            - name: MYSQL_DATABASE
              value: socialfeed
---
apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  selector:
    app: mysql
  ports:
    - port: 3306
```

```
kubectl apply -f k8s\mysql-temp.yaml
```

Create `k8s\deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: socialfeed-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: socialfeed
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: socialfeed
    spec:
      containers:
        - name: app
          image: socialfeed-service:1.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              value: jdbc:mysql://mysql:3306/socialfeed
            - name: SPRING_DATASOURCE_PASSWORD
              value: rootpw
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:MaxRAMPercentage=70"
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "768Mi"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 5
            failureThreshold: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
```

(`250m` means 250 "millicores" = a quarter of one CPU core. `Mi` = mebibytes.)

**Why these numbers for a Java app:** Spring Boot with JPA, Security and Kafka classes needs several hundred MB and can take 15 to 40 seconds to start on a small CPU share. A 384Mi limit would risk `OOMKilled`. `MaxRAMPercentage=70` tells the JVM to size its heap from the container limit instead of the whole machine. The **startup probe** gives the app up to 150 seconds (30 tries x 5 s) before liveness and readiness begin, so a slow start is not mistaken for a hang.

On the very first start all 3 Pods meet an empty database at once and Hibernate (`ddl-auto: update`) tries to create the tables in parallel. One Pod may restart once. That race is a real-world reason teams use Flyway or Liquibase migrations instead.

Commands (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl apply -f k8s\deployment.yaml
kubectl get deployment
kubectl get pods -o wide
kubectl delete pod one-pod-name
kubectl get pods
```

Watch the dead Pod get replaced automatically. Now do an update: make any visible change (for example add the optional `/api/public/host` endpoint from Week 0, or change the `version` in `build.gradle`) and rebuild as version 2.0.

```
docker build -t socialfeed-service:2.0 .
minikube image load socialfeed-service:2.0
kubectl set image deployment/socialfeed-service app=socialfeed-service:2.0
kubectl rollout status deployment/socialfeed-service
kubectl rollout history deployment/socialfeed-service
kubectl rollout undo deployment/socialfeed-service
```

## 3.3 Enterprise view

- Large teams deploy many times a day with rolling or canary releases. Netflix-style canary analysis (automatic comparison of the new version's errors and latency against the old) decides if a release continues or rolls back.
- Tools: Argo Rollouts and Flagger add canary and blue/green automatically. Harness has built-in canary and blue/green steps.
- Rules: always set requests and limits; always set readiness probes; run at least 2 to 3 replicas spread over different machines (and zones) for high availability.
- A **PodDisruptionBudget** (Week 7) protects availability when machines are drained for maintenance.
- **Company pipeline link:** the `replicas`, `image`, `resources`, and probe values in your Harness `values.yml` are filling in exactly this Deployment template.

## 3.4 Key concepts

ReplicaSet, Deployment, rolling update, maxSurge, maxUnavailable, rollback, canary, blue/green, liveness, readiness, startup probe, requests, limits, OOMKilled, CPU throttling.

## 3.5 Interview Q/A

**Q1. Deployment vs ReplicaSet vs Pod?**
Pod runs containers. ReplicaSet keeps N Pods alive. Deployment manages ReplicaSets to do updates and rollbacks. You write Deployments; the rest is created for you.

**Q2. Explain a rolling update.**
The Deployment creates a new ReplicaSet and scales it up step by step while scaling the old one down, within `maxSurge` and `maxUnavailable`. Readiness probes decide when a new Pod counts as available.

**Q3. Liveness vs readiness?**
Liveness failing → container restarted. Readiness failing → Pod removed from Service endpoints, no restart. A common mistake is making liveness check external dependencies like the database, which can cause restart storms.

**Q4. Requests vs limits?**
Requests are used for scheduling (guaranteed reservation). Limits are the cap enforced at runtime. Memory over limit → OOMKilled. CPU over limit → throttled.

**Q5. What are QoS (Quality of Service) classes?**
Guaranteed (requests equal limits for all containers), Burstable (requests below limits), BestEffort (none set). Under memory pressure, BestEffort Pods are evicted first.

**Q6. Pod stuck in `CrashLoopBackOff`: how do you debug?**
`kubectl logs pod --previous`, `kubectl describe pod pod` (events, exit code, OOMKilled?), check probes, config, and missing environment values.

**Q7. How would you do zero-downtime deployment for a Java app?**
Readiness probe, `maxUnavailable: 0`, graceful shutdown (Spring `server.shutdown=graceful`), a `preStop` hook or enough termination grace period, and at least 2 replicas.

## 3.5b Your turn

1. With `replicas: 4`, `maxSurge: 1`, `maxUnavailable: 0`, describe step by step what happens during an update.
2. Your Java app takes 60 seconds to start and keeps being restarted. Which probe do you adjust and how?
3. A Pod shows `OOMKilled`. List two possible fixes.

## 3.6 Summary

Deployments keep the desired number of Pods alive and replace them safely. Probes tell Kubernetes about health. Requests and limits control fair sharing of CPU and memory.

---

# WEEK 4: Networking: Services, DNS, Ingress

## 4.1 Theory

**Kubernetes networking rules (the "flat network" model):**

1. Every Pod gets its own IP.
2. Any Pod can reach any other Pod by IP without translation (unless a policy blocks it).
3. The network is implemented by a **CNI plugin** (Container Network Interface), for example Calico, Cilium, Flannel. OpenShift uses OVN-Kubernetes by default.
   **Service:** a stable name and virtual IP in front of a changing set of Pods, chosen by label selector. Traffic is spread across healthy (ready) Pods.


| Service type                 | Meaning                                                  |
| ---------------------------- | -------------------------------------------------------- |
| `ClusterIP` (default)        | Reachable only inside the cluster                        |
| `NodePort`                   | Opens a port on every node                               |
| `LoadBalancer`               | Asks the cloud for an external load balancer             |
| `ExternalName`               | A DNS alias to an outside name                           |
| Headless (`clusterIP: None`) | No virtual IP; DNS returns Pod IPs. Used by StatefulSets |

**DNS:** CoreDNS gives every Service a name: `service.namespace.svc.cluster.local`. Inside the same namespace, just `socialfeed-service` works.

**Ingress:** rules for HTTP/HTTPS traffic coming from outside (host and path routing, TLS certificates). Needs an **Ingress controller** (for example NGINX) to do the work. The newer **Gateway API** is the planned successor. **OpenShift** uses **Routes** (handled by HAProxy-based router) for the same job.

**NetworkPolicy** (Week 8) controls which Pods may talk to which.

## 4.2 Simple example

Create `k8s\service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: socialfeed-service
spec:
  selector:
    app: socialfeed
  ports:
    - port: 80
      targetPort: 8080
```

Commands (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl apply -f k8s\service.yaml
kubectl get svc
kubectl get endpoints socialfeed-service
kubectl port-forward svc/socialfeed-service 8080:80
```

In another PowerShell window run `curl.exe http://localhost:8080/api/public/host` (the optional Week 0 endpoint; use `curl.exe`, because plain `curl` is a PowerShell alias). Port-forward sends to one Pod, so to see load balancing run a test Pod inside the cluster:

```
kubectl run tester --rm -it --image=busybox --restart=Never -- wget -qO- http://socialfeed-service/api/public/host
```

Run that a few times; the `host` field changes between Pods. For the real flow, run `kubectl port-forward svc/socialfeed-service 8080:80` and repeat the register / login / post calls from Week 1 (or use Postman). The JWT you get from one Pod works on all Pods, because the token is signed with the same secret and no session is kept in the Pod. That is what "stateless" buys you.

Ingress (optional lab):

```
minikube addons enable ingress
```

Create `k8s\ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: socialfeed-ingress
spec:
  rules:
    - host: socialfeed.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: socialfeed-service
                port:
                  number: 80
```

```
kubectl apply -f k8s\ingress.yaml
```
On Windows with the Docker driver, the Ingress is not reachable until you (1) run `minikube tunnel` in a separate window and (2) add `127.0.0.1 socialfeed.local` to `C:\Windows\System32\drivers\etc\hosts`. Then call `http://socialfeed.local/api/auth/login` from Postman.

## 4.3 Enterprise view

- Typical path of a user request: **DNS → cloud load balancer → Ingress controller or API gateway → Service → Pod**.
- Large systems add a **service mesh** (Istio, Linkerd): a small proxy sidecar next to each Pod handles encryption between services (mTLS, mutual Transport Layer Security), retries, timeouts, and traffic splitting, without changing app code.
- Public traffic is protected by Web Application Firewalls and rate limits; internal services are kept on private networks.
- **Company pipeline link:** Harness deploys the Service and Route objects together with the Deployment. On OpenShift you will see `oc get route`.

## 4.4 Key concepts

Pod IP, CNI, Service, ClusterIP, NodePort, LoadBalancer, headless Service, endpoints, CoreDNS, Ingress, Ingress controller, Gateway API, OpenShift Route, service mesh.

## 4.5 Interview Q/A

**Q1. Why do we need Services if Pods have IPs?**
Pod IPs change every time a Pod is replaced. A Service gives a stable name and IP and load-balances over ready Pods.

**Q2. How does a Service find its Pods?**
By label selector. Matching ready Pods are listed in Endpoints (EndpointSlices).

**Q3. ClusterIP vs NodePort vs LoadBalancer?**
Internal only; node port opened on all nodes; cloud load balancer in front. Each builds on the previous.

**Q4. Ingress vs Service of type LoadBalancer?**
A LoadBalancer Service gives one external IP per Service (costly). One Ingress controller can route many hosts and paths to many Services over one entry point, and handles TLS.

**Q5. How does a Pod in namespace A call a Service in namespace B?**
`service-name.namespace-b.svc.cluster.local` (or `service-name.namespace-b`).

**Q6. How does `kube-proxy` work?**
It programs iptables or IPVS (IP Virtual Server) rules on each node so traffic to a Service's virtual IP is forwarded to one of the Pod IPs. Some CNIs like Cilium replace kube-proxy with eBPF (a fast in-kernel technology).

**Q7. Service returns no response. Debug steps?**
Check `kubectl get endpoints` (empty = selector or readiness problem), confirm labels match, confirm `targetPort`, test from a debug Pod, check NetworkPolicies.

## 4.5b Your turn

1. A Service has zero endpoints, but Pods are Running. Name two likely causes.
2. Why is a headless Service useful for a database cluster?
3. Draw (in words) the path of a request from a customer's phone to your Java Pod.

## 4.6 Summary

Pods get IPs, Services give stable access, DNS gives names, and Ingress (or OpenShift Route) brings outside HTTP traffic in. Service meshes add smart traffic control for large systems.

---

# WEEK 5: Configuration and Secrets

## 5.1 Theory

- **Twelve-factor app rule:** keep config out of the code and image. The same image should run in dev, test, and prod with different config.
- **ConfigMap:** non-secret settings (text values or whole files).
- **Secret:** sensitive values (passwords, tokens, certificates). **Important:** by default a Secret is only **base64-encoded, not encrypted**. Base64 is just a way to write text, anyone can decode it. Real protection needs encryption at rest in etcd, strict RBAC, and ideally an external vault.
- Ways to use them: environment variables, or files mounted into the container.
- **Immutable config and rollout:** changing a ConfigMap does not restart Pods automatically (for env variables). Pipelines usually add a checksum annotation so a change triggers a rolling update.
- **External secrets:** HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, accessed via the External Secrets Operator or the Secrets Store CSI (Container Storage Interface) driver.

## 5.2 Simple example

Today the DB URL, DB password and JWT signing secret sit inside `application.yaml`, which is baked into the image (and, worse, committed to Git). Anyone who knows the JWT secret can forge a valid login token. We move all three out.

`k8s\configmap.yaml` (non-secret settings):

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: socialfeed-config
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/socialfeed"
  SPRING_DATASOURCE_USERNAME: "root"
```

Create the Secret from the command line (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`). Use your own values; the JWT secret must be at least 32 characters:

```
kubectl create secret generic socialfeed-secret --from-literal=SPRING_DATASOURCE_PASSWORD=rootpw --from-literal=MYSQL_ROOT_PASSWORD=rootpw --from-literal=APP_JWT_SECRET=replace-this-with-a-long-random-32+chars
```

Spring Boot's relaxed binding means `SPRING_DATASOURCE_URL` overrides `spring.datasource.url` and `APP_JWT_SECRET` overrides `app.jwt.secret`. **No Java change is needed.** In `deployment.yaml` delete the plain `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_PASSWORD` entries and add this (keep `JAVA_TOOL_OPTIONS`):

```yaml
          envFrom:
            - configMapRef:
                name: socialfeed-config
            - secretRef:
                name: socialfeed-secret
```

Also make `mysql-temp.yaml` read its password from the same Secret (replace `value: rootpw`):

```yaml
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: socialfeed-secret
                  key: MYSQL_ROOT_PASSWORD
```

```
kubectl apply -f k8s\configmap.yaml
kubectl apply -f k8s\mysql-temp.yaml
kubectl apply -f k8s\deployment.yaml
kubectl rollout restart deployment/socialfeed-service
```

Prove it works: register and log in (Week 1 calls) through a port-forward, keep the token, then change `APP_JWT_SECRET` in the Secret and run `kubectl rollout restart deployment/socialfeed-service` again. Your old token is now rejected (the API answers 403), because the new Pods sign and verify with a different key. That one environment variable is the whole security of your logins.

Look at how weak base64 is:

```
kubectl get secret socialfeed-secret -o jsonpath="{.data.SPRING_DATASOURCE_PASSWORD}"
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("cm9vdHB3"))
```

The second command prints `rootpw`. Anyone with `get secret` rights can read it.

## 5.3 Enterprise view

- One image, many environments: dev, QA, staging, production each get their own config files, usually stored in Git.
- Secrets are never stored in Git in plain form. Common choices: Vault, cloud secret managers, or Sealed Secrets (encrypted in Git).
- Rotation: passwords are rotated automatically and Pods pick up new values through mounted files or restarts.
- **Company pipeline link:** your Harness `values.yml` is the per-environment config. It fills in ConfigMap values, replica counts, and image tags. Secrets are typically referenced by name (from a vault or secret manager), not written in the file.

## 5.4 Key concepts

Twelve-factor config, ConfigMap, Secret, base64 vs encryption, env vars vs mounted files, external secret managers, config-triggered rollout.

## 5.5 Interview Q/A

**Q1. Are Kubernetes Secrets secure?**
Not by default: only base64-encoded. Make them safer with etcd encryption at rest, RBAC limiting who can read them, avoiding logs, and external secret managers.

**Q2. Env variable vs mounted file for config?**
Env variables are simple but fixed at Pod start. Mounted files can update in place (for ConfigMaps and Secrets, with a short delay), and suit certificates and large config.

**Q3. How do you roll out Pods after a ConfigMap change?**
`kubectl rollout restart`, or in Helm add a checksum annotation of the ConfigMap in the Pod template so any change alters the template and triggers a rolling update.

**Q4. How do you keep secrets out of Git?**
Use Sealed Secrets, SOPS (Secrets OPerationS, an encryption tool), or External Secrets Operator pulling from Vault or a cloud secret manager.

**Q5. Can the same image run in dev and prod?**
Yes, that is the goal: build once, promote the same image, change only configuration.

## 5.5b Your turn

1. Why is "build once, promote everywhere" safer than building separately for each environment?
2. A teammate says "Secrets are encrypted, so I committed the YAML to Git." What do you tell them?
3. Which would you choose for a TLS certificate file: env variable or mounted file, and why?

## 5.6 Summary

Keep configuration outside the image. ConfigMaps hold normal settings, Secrets hold sensitive ones (but need extra protection). One image can serve every environment.

---

# WEEK 6: Storage and Stateful Applications

## 6.1 Theory

- Container files vanish with the container. For data that must survive, use **volumes**.
- **Volume types:** `emptyDir` (temporary, lives as long as the Pod), `configMap`/`secret` volumes, and **persistent** volumes.
- **PersistentVolume (PV):** a piece of real storage in the cluster (a cloud disk, a network disk).
- **PersistentVolumeClaim (PVC):** an app's request for storage ("I need 5 GB"). Kubernetes binds the claim to a volume.
- **StorageClass:** a template that creates volumes on demand (**dynamic provisioning**), for example fast SSD vs cheap disk.
- **Access modes:** `ReadWriteOnce` (one node at a time), `ReadWriteMany` (many nodes), `ReadOnlyMany`.
- **StatefulSet:** like a Deployment but for apps that need **stable identity** and **their own disk**: Pods are named `mysql-0`, `mysql-1`, started in order, and each keeps its own PVC even after restart. Used for databases, Kafka, Zookeeper, Elasticsearch.
- **Rule of thumb:** keep your own services stateless; keep state in managed data stores or StatefulSets.

## 6.2 Simple example: MySQL as a StatefulSet (the app's real database)

No Java change. The app already persists users and posts through JPA. We replace the throwaway MySQL with a proper one. `k8s\mysql.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  clusterIP: None
  selector:
    app: mysql
  ports:
    - port: 3306
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: socialfeed-secret
                  key: MYSQL_ROOT_PASSWORD
            - name: MYSQL_DATABASE
              value: socialfeed
          readinessProbe:
            exec:
              command: ["sh", "-c", "mysqladmin ping -h 127.0.0.1 -uroot -p$MYSQL_ROOT_PASSWORD"]
            initialDelaySeconds: 20
            periodSeconds: 5
          volumeMounts:
            - name: data
              mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

Commands (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl delete -f k8s\mysql-temp.yaml
kubectl apply -f k8s\mysql.yaml
kubectl get pvc
kubectl rollout restart deployment/socialfeed-service
```

(The `mysql` Service name is the same as before, so the app URL `jdbc:mysql://mysql:3306/socialfeed` does not change. The headless Service makes `mysql` resolve to the Pod `mysql-0`.)

Test: through a port-forward, register `rahul`, log in and create a post (Week 1 calls). Then:

```
kubectl delete pod mysql-0
kubectl get pods -w
```

After `mysql-0` comes back (the same name, the same disk), `GET /api/posts` still returns your post and the login still works. Compare with Week 3's throwaway MySQL, which lost everything when its Pod died. The 3 app Pods share the same data, which shows why state belongs outside the app Pods.

**Enterprise note:** `ddl-auto: update` lets Hibernate change the schema from whichever Pod starts first. Production teams switch it to `validate` or `none` and manage the schema with Flyway or Liquibase run once per release (an init container or a Job).

## 6.3 Enterprise view

- Most enterprises do **not** run production databases in Kubernetes by hand. They use managed services (Amazon RDS, Amazon ElastiCache, Azure Cache for Redis, managed Kafka) for easier backups and failover.
- When databases do run in Kubernetes, they use **Operators** (Week 11) such as Strimzi for Kafka, or vendor operators for PostgreSQL and Redis, which automate failover, backup, and upgrades.
- Storage is backed up with volume snapshots; disks are spread over zones; storage performance classes are chosen per workload.
- **Company pipeline link (your daily stack):** Kafka and Redis, which you already use in Java, map here. A Kafka cluster in Kubernetes is a StatefulSet with a PVC per broker.

## 6.4 Key concepts

Volume, emptyDir, PV, PVC, StorageClass, dynamic provisioning, access modes, StatefulSet, headless Service, stable identity, volumeClaimTemplates, snapshots.

## 6.5 Interview Q/A

**Q1. Deployment vs StatefulSet?**
Deployment: interchangeable Pods, random names, shared or no storage. StatefulSet: ordered startup, stable names and network identity (`pod-0`), and a dedicated PVC per Pod.

**Q2. PV vs PVC vs StorageClass?**
PV is the storage, PVC is the request for it, StorageClass is the recipe for creating PVs automatically.

**Q3. What happens to a PVC when its Pod is deleted?**
It stays. The new Pod with the same name reattaches to the same PVC (in a StatefulSet). The reclaim policy (`Retain` or `Delete`) decides what happens to the PV when the PVC itself is deleted.

**Q4. Why is the headless Service needed with a StatefulSet?**
It gives each Pod a stable DNS name like `mysql-0.mysql.default.svc.cluster.local`, needed for members to find each other.

**Q5. `ReadWriteOnce` Pod stuck in `ContainerCreating` after node failure. Why?**
The disk may still be attached to the failed node. It must detach before attaching elsewhere. Check events in `kubectl describe pod`.

## 6.5b Your turn

1. A user registers through Pod A and logs in through Pod B. Why does that work? What would break if the app kept sessions or users in a Java `static` variable?
2. Why would a bank choose a managed database over running one in Kubernetes themselves?
3. What breaks if two Pods write to the same `ReadWriteOnce` disk on different nodes?

## 6.6 Summary

Volumes keep data beyond a Pod's life. PVCs request storage, StorageClasses create it. StatefulSets give identity and own disks to stateful apps. Prefer stateless apps plus managed data stores.

---

# WEEK 7: Scaling, Scheduling, and Special Workloads

## 7.1 Theory

**Three kinds of scaling:**


| Kind                                         | What it does                                                   |
| -------------------------------------------- | -------------------------------------------------------------- |
| **HPA** (Horizontal Pod Autoscaler)          | Changes number of Pods based on CPU, memory, or custom metrics |
| **VPA** (Vertical Pod Autoscaler)            | Adjusts a Pod's CPU and memory requests                        |
| **Cluster Autoscaler** (or Karpenter on AWS) | Adds or removes nodes when Pods cannot fit or nodes are empty  |

HPA formula (worth remembering): `desired = ceil(current replicas x current metric / target metric)`.
Event-driven scaling: **KEDA** (Kubernetes Event-Driven Autoscaling) can scale on Kafka lag or queue length, very useful for your Kafka-based services.

**Scheduling controls (how Pods are placed):**

- `nodeSelector` / node affinity: run only on nodes with certain labels.
- Pod affinity / anti-affinity: keep Pods together or spread them apart.
- **Taints and tolerations:** a node "taint" repels Pods unless they have a matching "toleration" (for dedicated or GPU nodes).
- **Topology spread constraints:** spread Pods evenly across zones.
- **PriorityClass:** important Pods may evict less important ones.
- **PodDisruptionBudget (PDB):** "at least 2 of my Pods must stay up" during voluntary disruptions like node upgrades.
  **Other workload types:**
- **Job:** run to completion once (data migration).
- **CronJob:** Job on a schedule (nightly report).
- **DaemonSet:** one Pod per node (log collector, monitoring agent).

## 7.2 Simple example

Enable metrics (from any folder):

```
minikube addons enable metrics-server
```

`k8s\hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: socialfeed-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: socialfeed-service
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

Commands (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl apply -f k8s\hpa.yaml
kubectl get hpa -w
```

Register a user first (`rahul` / `secret1`, Week 1 call through a port-forward). Login uses BCrypt, which is deliberately CPU heavy, so it is a perfect load generator. `k8s\load.yaml`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: load
spec:
  restartPolicy: Never
  containers:
    - name: load
      image: busybox
      command: ["/bin/sh", "-c"]
      args:
        - while true; do wget -q -O- --header='Content-Type: application/json' --post-data='{"username":"rahul","password":"secret1"}' http://socialfeed-service/api/auth/login > /dev/null; done
```

```
kubectl apply -f k8s\load.yaml
kubectl get hpa -w
```

Watch replicas grow. Stop the load:

```
kubectl delete -f k8s\load.yaml
```

Add a PodDisruptionBudget, `k8s\pdb.yaml`:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: socialfeed-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: socialfeed
```

## 7.3 Enterprise view

- Retail and banking traffic is spiky (sales events, market open, month-end). HPA plus cluster autoscaling absorbs this and saves cost at night.
- Niantic's Pokemon GO launch on Google Kubernetes Engine is a well-known public example of Kubernetes handling traffic far above forecast.
- Teams spread Pods across availability zones so one data center failure does not stop service.
- FinOps (cloud cost control): right-size requests, use spot or preemptible machines for batch work, set namespace quotas.
- **Company pipeline link:** `replicas`, `minReplicas`, `maxReplicas`, and CPU targets typically sit in `values.yml`. Many banks keep CronJobs for end-of-day batch work.

## 7.4 Key concepts

HPA, VPA, Cluster Autoscaler, KEDA, affinity, anti-affinity, taint, toleration, topology spread, PriorityClass, PDB, Job, CronJob, DaemonSet.

## 7.5 Interview Q/A

**Q1. How does HPA decide the replica count?**
It reads metrics from the metrics server, compares to the target, and applies `ceil(current replicas x current / target)` within min and max, with stabilization windows to avoid flapping.

**Q2. HPA is not scaling. What do you check?**
Metrics server running? CPU **requests** set (HPA percent is based on requests)? Target reference correct? Hit `maxReplicas`? Check `kubectl describe hpa`.

**Q3. HPA vs Cluster Autoscaler?**
HPA changes Pod count. Cluster Autoscaler changes node count when Pods are `Pending` for lack of space (or nodes are underused). They work together.

**Q4. Taint vs node affinity?**
A taint repels Pods from a node unless they tolerate it. Affinity attracts Pods to nodes. Taints say "keep out", affinity says "I prefer or need to be there". For dedicated nodes you often use both.

**Q5. What is a PodDisruptionBudget and when does it apply?**
It limits voluntary disruptions (drain, upgrade) so a minimum number of Pods stays up. It does not protect against crashes or node failures.

**Q6. Pod is `Pending`. Why?**
Not enough CPU or memory on any node, no node matches selectors or tolerations, PVC not bound, or quota exceeded. `kubectl describe pod` shows the scheduler's reason.

## 7.5b Your turn

1. Current 4 Pods at 90% CPU, target 60%. How many Pods will HPA want?
2. Why should important Pods use anti-affinity or topology spread across zones?
3. For a Kafka consumer app, why might scaling on CPU be a poor signal compared to consumer lag?

## 7.6 Summary

HPA scales Pods, the cluster autoscaler scales machines, scheduling rules decide placement, PDBs protect availability, and Jobs, CronJobs, and DaemonSets cover non-service workloads.

---

# WEEK 8: Security

## 8.1 Theory

**Four layers (the "4Cs": Cloud, Cluster, Container, Code):** protect each layer.

**Access to the API server has 3 gates:**

1. **Authentication**: who are you? (certificates, tokens, OIDC single sign-on)
2. **Authorization**: what may you do? Mostly **RBAC** (Role-Based Access Control).
3. **Admission control**: final checks and edits (policy tools such as OPA Gatekeeper and Kyverno live here).
   **RBAC building blocks:**

- `Role` (permissions inside one namespace) and `ClusterRole` (cluster-wide).
- `RoleBinding` / `ClusterRoleBinding`: attach a role to a user, group, or **ServiceAccount**.
- **ServiceAccount:** the identity a Pod uses when it calls the API server.
- Principle: **least privilege**, give only what is needed.
  **Pod and container security:**
- Run as non-root, read-only root file system, drop Linux capabilities, no privilege escalation (`securityContext`).
- **Pod Security Standards:** Privileged, Baseline, Restricted levels (enforced by Pod Security Admission).
- **OpenShift SCC** (Security Context Constraints): by default OpenShift runs containers with a random non-root user ID. Images that insist on root will fail until fixed. This is a very common surprise for new OpenShift users.
  **Network security:** a **NetworkPolicy** is a firewall rule for Pods. By default everything can talk to everything; a policy flips this to allow-list.

**Supply chain security:** scan images, sign images, use trusted registries, keep a Software Bill of Materials (SBOM, a list of everything inside an image).

## 8.2 Simple example

Run the Java container as non-root. In `deployment.yaml` under the container:

```yaml
          securityContext:
            runAsNonRoot: true
            runAsUser: 10001
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: ["ALL"]
```

(With a read-only file system Spring Boot needs a writable `/tmp`: add an `emptyDir` volume mounted at `/tmp`.)

Update the Dockerfile final stage to create a user (add before `ENTRYPOINT`):

```dockerfile
RUN useradd -u 10001 appuser
USER 10001
```

RBAC: let a ServiceAccount only read ConfigMaps. `k8s\rbac.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: socialfeed-sa
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: configmap-reader
rules:
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: socialfeed-sa-binding
subjects:
  - kind: ServiceAccount
    name: socialfeed-sa
roleRef:
  kind: Role
  name: configmap-reader
  apiGroup: rbac.authorization.k8s.io
```

Test permissions (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl apply -f k8s\rbac.yaml
kubectl auth can-i get configmaps --as=system:serviceaccount:default:socialfeed-sa
kubectl auth can-i delete pods --as=system:serviceaccount:default:socialfeed-sa
```

NetworkPolicy: only the app may talk to MySQL. `k8s\netpol.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: mysql-allow-app-only
spec:
  podSelector:
    matchLabels:
      app: mysql
  policyTypes: ["Ingress"]
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: socialfeed
      ports:
        - port: 3306
```

Two more SocialFeed-specific hardening steps:

- **Database least privilege:** the app currently connects as MySQL `root`. Add `MYSQL_USER` and `MYSQL_PASSWORD` environment variables (from the Secret) to the MySQL StatefulSet. MySQL then creates a normal user that owns only the `socialfeed` database. Point `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` at that user and keep `root` for admins only.
- **No API token needed:** SocialFeed never calls the Kubernetes API, so set `automountServiceAccountToken: false` in the Pod spec. A compromised app then has no cluster credentials to steal.

(Minikube's default network add-on may not enforce policies; start it with `minikube start --cni=calico` if you want to see blocking in action.)

## 8.3 Enterprise view

- Banks and regulated industries (PCI DSS for card data, SOC 2, and similar standards) require: no root containers, encrypted traffic (mTLS), audit logs of every API call, secrets in vaults, and signed images only.
- **Policy as code:** Kyverno or OPA Gatekeeper reject non-compliant YAML before it enters the cluster (for example "no `latest` tags, no privileged Pods, must have resource limits").
- Cluster access is through company single sign-on, with just-in-time admin access and full audit trails.
- Runtime threat detection (Falco and commercial tools) watches for strange behavior inside containers.
- **Company pipeline link:** OpenShift's stricter defaults (SCC, random user IDs) and Harness approvals and RBAC are your everyday security guardrails.

## 8.4 Key concepts

Authentication, authorization, admission control, RBAC, Role, ClusterRole, binding, ServiceAccount, least privilege, securityContext, Pod Security Standards, SCC, NetworkPolicy, image scanning and signing, SBOM, policy as code.

## 8.5 Interview Q/A

**Q1. Explain RBAC.**
Roles list allowed verbs on resources. Bindings attach roles to users, groups, or ServiceAccounts. Role and RoleBinding are namespaced; ClusterRole and ClusterRoleBinding are cluster-wide.

**Q2. What is a ServiceAccount?**
An identity for Pods. Pods use its token to call the API server. Give each app its own ServiceAccount with minimum rights and disable token mounting if not needed.

**Q3. Default network behavior and how to secure it?**
All Pods can talk to all Pods. Use NetworkPolicies, start with a "default deny" policy, then allow only needed paths. Requires a CNI that enforces policies.

**Q4. Why do containers running as root cause problems on OpenShift?**
OpenShift's restricted SCC assigns a random non-root user ID, so images that need root or fixed ownership of folders fail. Fix: build the image to run as non-root, with group-writable folders.

**Q5. How would you stop developers from deploying unsafe Pods?**
Admission controllers: Pod Security Admission plus policy engines (Kyverno, OPA Gatekeeper).

**Q6. Where could secrets leak?**
Git, image layers, logs, environment variable dumps, over-broad RBAC (`get secrets`), unencrypted etcd backups.

## 8.5b Your turn

1. Why is giving `cluster-admin` to a CI/CD pipeline dangerous, and what would you do instead?
2. A new image works on Docker but fails on OpenShift with a permission error. What do you suspect first?
3. What would a "default deny" NetworkPolicy break, and how do you fix it safely?

## 8.6 Summary

Secure every layer: authenticate, authorize with least-privilege RBAC, enforce policies at admission, run containers as non-root, restrict network paths, and protect the image supply chain.

---

# WEEK 9: Helm, CI/CD, GitOps, and the OpenShift Pipeline

## 9.1 Theory

**The problem:** 10 YAML files per app x 5 environments x 200 apps = unmanageable copy-paste.

**Helm** (package manager for Kubernetes):

- **Chart:** a folder of YAML **templates** plus default values.
- **values.yaml:** the settings that fill the templates. Environment-specific values files override defaults.
- **Release:** one installed instance of a chart, with revision history (`helm rollback` works).
- Templates use Go template syntax such as `{{ .Values.image.tag }}`.
  **Kustomize:** a template-free alternative built into `kubectl`. A **base** plus **overlays** (dev, prod) that patch it.

**CI/CD** (Continuous Integration / Continuous Delivery):

- **CI:** on every code push, build, test, scan, and create an image.
- **CD:** deploy that image to environments, with approvals and checks.
  **GitOps:** Git is the single source of truth for what should be running. A tool inside the cluster (Argo CD or Flux) constantly compares Git with the cluster and fixes any difference ("drift"). Benefits: audit trail, easy rollback (revert a commit), no pipeline needing cluster admin credentials.

**Push vs pull deployment:**

- **Push:** the pipeline (such as Harness) connects to the cluster and applies changes.
- **Pull:** an agent in the cluster (Argo CD) pulls from Git.

## 9.2 The company pipeline, mapped step by step

```
Developer pushes code
        |
        v
GitHub Actions (CI): build Java app -> run tests -> build Docker image -> scan -> push to registry
        |
        v
Container registry stores image  (e.g. registry.company.com/wealth/socialfeed-service:1.4.7)
        |
        v
Harness (CD): reads values.yml (image tag, replicas, resources, env settings)
        |   -> renders templates (Helm or manifests) -> checks approvals -> deploys stage by stage
        v
OpenShift (OCP, Kubernetes-based): Deployment -> ReplicaSet -> Pods;  Service;  Route
        |
        v
Users / other services call the app
```


| Company step                 | Concept from this course                 |
| ---------------------------- | ---------------------------------------- |
| GitHub Actions workflow file | CI pipeline as code (Week 1 image build) |
| Registry                     | Image storage (Week 1)                   |
| `values.yml`                 | Helm-style values (Weeks 3, 5, 7)        |
| Harness stage and approvals  | CD, canary / blue-green (Week 3)         |
| OpenShift Project            | Namespace (Week 2)                       |
| Route                        | Ingress (Week 4)                         |
| SCC                          | Security (Week 8)                        |
| `oc` commands                | `kubectl` plus OpenShift extras          |

**Typical `values.yml` anatomy** (simplified, we will read your real one together):

```yaml
image:
  repository: registry.company.com/wealth/socialfeed-service
  tag: "1.4.7"
replicaCount: 3
resources:
  requests: { cpu: 250m, memory: 384Mi }
  limits:   { cpu: 500m, memory: 512Mi }
env:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/socialfeed"
route:
  host: socialfeed.apps.company.com
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 8
```

## 9.3 Simple example: Helm chart for our app

Commands (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
helm create helm\socialfeed-chart
helm lint helm\socialfeed-chart
helm template socialfeed helm\socialfeed-chart
helm install socialfeed helm\socialfeed-chart --set image.repository=socialfeed-service --set image.tag=3.0
helm list
helm upgrade socialfeed helm\socialfeed-chart --set image.tag=2.0
helm history socialfeed
helm rollback socialfeed 1
```

(Before installing, delete the earlier hand-made Deployment and Service so names do not clash: `kubectl delete -f k8s\deployment.yaml` and `kubectl delete -f k8s\service.yaml`.)

Compare with a prod values file:

```
helm upgrade socialfeed helm\socialfeed-chart -f helm\socialfeed-chart\values-prod.yaml
```

**Mini GitHub Actions file** (`.github\workflows\build.yml` in the project folder):

```yaml
name: build
on:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew build -x test
      - run: docker build -t ghcr.io/your-user/socialfeed-service:${{ github.sha }} .
```

Notes: `contextLoads()` needs a database, so this skips tests. To run them, add a `services: mysql:` block (image `mysql:8`, with `MYSQL_ROOT_PASSWORD` and `MYSQL_DATABASE=socialfeed`) to the job and set `SPRING_DATASOURCE_PASSWORD`. If the build fails with "permission denied" on `gradlew`, run `git update-index --chmod=+x gradlew` once, because Git on Windows may not store the executable bit. Action versions move on; use the latest major versions.

## 9.4 Enterprise view

- Large organizations have a **platform team** that offers a "golden path": a standard pipeline template, standard Helm chart, and standard cluster, so product teams only fill in `values.yml`.
- Environments move **dev → QA → staging → prod** using the same image and different values, with approvals and automated tests between stages.
- Pipelines include security scanning, license checks, and quality gates (for example SonarQube code quality).
- GitOps (Argo CD) is widespread at technology companies; Harness and Spinnaker-style push pipelines are common in large enterprises. Many companies mix both.
- Releases are tied to change-management records in regulated industries (who approved, when, what changed).

## 9.5 Key concepts

Helm chart, template, values, release, rollback, Kustomize, CI, CD, GitOps, drift, push vs pull, Argo CD, Harness, promotion, golden path, `oc`, Route, Project.

## 9.6 Interview Q/A

**Q1. Why Helm?**
Reuse one template across apps and environments, version releases, and roll back easily. Values change per environment while the template stays the same.

**Q2. Helm vs Kustomize?**
Helm uses templating and packaging with release tracking. Kustomize uses plain YAML with patch overlays, no templates. Many teams use Helm for third-party charts and Kustomize for in-house tweaks, or both.

**Q3. What is GitOps and why use it?**
Git holds the desired state; an in-cluster agent syncs it. Benefits: audit history, easy rollback, drift correction, safer credentials.

**Q4. Design a CI/CD pipeline for a microservice.**
Push → build and unit test → static analysis and security scan → build and scan image → push to registry with unique tag → deploy to dev automatically → integration tests → promote to QA and staging → manual approval → canary in prod → automated verification → full rollout or automatic rollback.

**Q5. Difference between OpenShift and Kubernetes?**
OpenShift is a Kubernetes distribution with added features: Routes, built-in image registry and ImageStreams, source-to-image builds, stricter security defaults (SCC), integrated monitoring, a web console, and an Operator-based lifecycle. Project = namespace with extras.

**Q6. How do you roll back in a pipeline?**
Redeploy the previous image tag and values (or `helm rollback`, or `kubectl rollout undo`), ideally automated on failed health verification. In GitOps, revert the Git commit.

**Q7. How do you promote the same build across environments safely?**
Immutable image tag or digest, per-environment values, same pipeline stages, and tests plus approvals between environments.

## 9.6b Your turn

1. Walk me through what happens, in order, from `git push` to a new Pod serving users in your company setup. Which parts do you already know, and which are unclear?
2. In the `values.yml` of one of your real services, what do you think each top-level key controls? (Share one with sensitive values removed and we will read it together.)
3. What does "drift" mean and why can it be dangerous in production?

## 9.7 Summary

Helm packages and parameterizes YAML. CI builds and verifies images, CD deploys them stage by stage, and GitOps keeps clusters matching Git. Your company pipeline is a concrete instance of this: GitHub Actions → registry → Harness with `values.yml` → OpenShift.

---

# WEEK 10: Observability and Troubleshooting

## 10.1 Theory

**Observability = seeing inside a running system.** Three pillars:

1. **Logs**: text records of events. Collected by an agent on each node and sent to central storage (ELK: Elasticsearch, Logstash, Kibana; EFK with Fluentd or Fluent Bit; Loki; Splunk).
2. **Metrics**: numbers over time (CPU, request rate, error rate, latency). **Prometheus** collects them; **Grafana** draws dashboards; **Alertmanager** sends alerts.
3. **Traces**: the path of one request across many services (OpenTelemetry, Jaeger, Zipkin, Datadog).
   **Key monitoring ideas:**

- **Golden signals** (Google SRE, Site Reliability Engineering): latency, traffic, errors, saturation.
- **SLI / SLO / SLA:** Service Level Indicator (what you measure), Objective (your target, e.g. 99.9% success), Agreement (the contract with customers).
- **Error budget:** how much failure the SLO allows; when spent, slow down releases.
  **Kubernetes debugging ladder:**

1. `kubectl get pods` (status)
2. `kubectl describe pod` (events at the bottom)
3. `kubectl logs` (and `--previous`)
4. `kubectl exec` / `kubectl debug` (look inside)
5. `kubectl get events --sort-by=.lastTimestamp`
6. `kubectl top` (resource use)
   **Common Pod states and what they usually mean:**


| Status                              | Usual cause                                                     |
| ----------------------------------- | --------------------------------------------------------------- |
| `Pending`                           | No node fits, PVC unbound, quota                                |
| `ImagePullBackOff` / `ErrImagePull` | Wrong image name or tag, no registry credentials                |
| `CrashLoopBackOff`                  | App crashes on start: bad config, missing dependency, OOMKilled |
| `Running` but `0/1 Ready`           | Readiness probe failing                                         |
| `OOMKilled`                         | Memory limit too low or a memory leak                           |
| `Evicted`                           | Node ran out of resources                                       |
| `Terminating` forever               | Finalizers or stuck process                                     |

## 10.2 Simple example: break it and fix it

Add `runtimeOnly 'io.micrometer:micrometer-registry-prometheus'` to `build.gradle`, add `"/actuator/prometheus"` to the `permitAll` list for this lab (in production protect it, for example with a NetworkPolicy or a separate management port), rebuild as a new version and deploy. The metrics then appear at `/actuator/prometheus`.

Drill 1, image problem (from `C:\Users\rahul\Downloads\Home_Project\SocialFeed`):

```
kubectl set image deployment/socialfeed-service app=socialfeed-service:9.9
kubectl get pods
kubectl describe pod new-pod-name
kubectl rollout undo deployment/socialfeed-service
```

Drill 2, memory problem: set the memory limit to `64Mi`, apply, watch `OOMKilled`, then fix.

Drill 3, readiness problem: point the readiness path to `/nope`, apply, see `0/1 Ready` and an empty endpoint list.

Drill 4, a broken Service selector: change the Service selector to `app: wrong`, then run `kubectl get endpoints socialfeed-service`.
Drill 5, wrong database password: edit the Secret so `SPRING_DATASOURCE_PASSWORD` is wrong, then `kubectl rollout restart deployment/socialfeed-service`. Watch the new Pods go to `CrashLoopBackOff` while the old Pods keep serving traffic (that is what `maxUnavailable: 0` protects). Run `kubectl logs new-pod-name --previous` and find `Access denied for user 'root'`. Fix the Secret and roll back or restart.

Drill 6, probes blocked by Spring Security: remove `"/actuator/health/**"` from the `permitAll` list in `SecurityConfig`, rebuild and deploy. The kubelet probes are anonymous, so they get HTTP 403, the startup probe fails and the container is killed over and over. `kubectl describe pod` shows `Startup probe failed: HTTP probe failed with statuscode: 403`. The app is fine; the platform could not look at it. Put the line back.

Debug a running Pod:

```
kubectl exec -it pod-name -- sh
kubectl top pods
kubectl get events --sort-by=.lastTimestamp
```

## 10.3 Enterprise view

- A central observability stack (often Prometheus plus Grafana, Datadog, Dynatrace, Splunk, or New Relic) serves all teams, with ready-made dashboards per service.
- **On-call** engineers get alerts based on symptoms users feel (error rate, latency), not just machine stats.
- **Runbooks** (step-by-step guides) and **post-incident reviews** without blame improve reliability over time.
- Distributed tracing is vital in microservice systems with hundreds of services, as one slow call deep in the chain can slow the whole request.
- Chaos engineering (deliberately breaking things in a controlled way) is used by mature teams to test resilience.

## 10.4 Key concepts

Logs, metrics, traces, Prometheus, Grafana, Alertmanager, OpenTelemetry, golden signals, SLI/SLO/SLA, error budget, runbook, `kubectl describe/logs/exec/debug/top`, common Pod states.

## 10.5 Interview Q/A

**Q1. A Pod is in `CrashLoopBackOff`. Walk through debugging.**
`kubectl describe pod` (exit code, reason, events) → `kubectl logs --previous` → check config, Secrets, dependencies, probes, memory limit → reproduce locally with the same image and env.

**Q2. Users report the app is slow. Where do you start?**
Check golden signals on a dashboard: latency, error rate, traffic, saturation. Then trace a slow request to find the slow service, then check that Pod's CPU throttling, memory, garbage collection, and dependencies (database, Kafka lag).

**Q3. `ImagePullBackOff`?**
Wrong name or tag, image not in registry, missing `imagePullSecrets`, registry unreachable, or hitting rate limits.

**Q4. How does Prometheus get metrics from Pods?**
It "scrapes" (pulls) an HTTP metrics endpoint on a schedule, discovering targets via Kubernetes service discovery (or ServiceMonitor objects with the Prometheus Operator).

**Q5. Logs vs metrics vs traces?**
Logs: what happened (detail). Metrics: how much and how often (trends and alerts). Traces: where time went across services.

**Q6. Node is `NotReady`. What do you check?**
`kubectl describe node`, kubelet status and logs, disk, memory, and network pressure, container runtime health, and cloud provider events.

**Q7. How do you debug a distroless container with no shell?**
`kubectl debug` with an ephemeral debug container sharing the Pod's namespaces.

## 10.5b Your turn

1. I will describe a failing Pod situation in chat and you tell me your first three commands. (We will do 5 of these as drills.)
2. Why is alerting on "CPU is 80%" often worse than alerting on "error rate above 2%"?
3. What is the difference between a Java heap OutOfMemoryError and a container OOMKilled?

## 10.6 Summary

Observe with logs, metrics, and traces. Debug with a fixed ladder: status → describe → logs → exec → events → top. Know the common Pod states by heart.

---

# WEEK 11: Advanced and Expert Topics

## 11.1 Theory

### A. Extending Kubernetes

- **Custom Resource Definition (CRD):** teach Kubernetes a new object type (for example `KafkaTopic`).
- **Operator:** a controller that watches custom objects and automates human operations (install, upgrade, backup, failover). Pattern = CRD + controller + control loop. Examples: Strimzi (Kafka), Prometheus Operator, cert-manager. OpenShift itself is built from operators.

### B. Deeper internals (frequent expert questions)

- **How the scheduler works:** filter nodes that cannot host the Pod, score the rest, bind to the best.
- **etcd:** uses the Raft algorithm for agreement; needs a majority (quorum), hence 3 or 5 members.
- **Watch mechanism:** controllers use informers (a local cache plus watch on the API server) instead of polling.
- **Pod lifecycle and graceful shutdown:** Pod set to Terminating → removed from endpoints and `preStop` hook runs → `SIGTERM` (polite stop signal) → wait `terminationGracePeriodSeconds` → `SIGKILL` (forced stop).
- **Container runtime interfaces:** CRI (runtime), CNI (network), CSI (storage). Kubernetes plugs into replaceable parts.

### C. Messaging and data on Kubernetes (your Kafka background)

- Kafka brokers as a StatefulSet with PVCs; use an operator such as Strimzi.
- Consumers are Deployments; scale on consumer lag with KEDA; remember the number of consumers useful in a group is capped by the number of partitions.

### D. Multi-cluster, multi-region, upgrades, disaster recovery

- Why multiple clusters: isolation, region latency, failure containment, legal data location.
- **Cluster upgrades:** one minor version at a time; control plane first, then nodes; drain nodes respecting PDBs.
- **Backups:** etcd snapshots; Velero for objects and volumes.
- **High availability:** 3 control plane nodes across zones; multiple replicas spread across zones.
- **Disaster recovery targets:** RTO (Recovery Time Objective: how long you can be down) and RPO (Recovery Point Objective: how much data you can lose).

### E. Service mesh and advanced traffic

- Istio and Linkerd: sidecars (or newer sidecar-less "ambient" mode) for mTLS, retries, circuit breaking, traffic split, and telemetry.

### F. Platform engineering and cost

- Internal developer platforms, namespace-as-a-service, quotas, FinOps, spot instances, bin-packing, right-sizing.

### G. Security extras

- Workload identity (Pods get cloud identity without static keys), image signing (Sigstore/cosign), runtime detection.

## 11.2 Simple example

1. **Graceful shutdown:** Spring Boot 3.4 and later (we are on 4.0) already shuts down gracefully by default, so you only need `spring.lifecycle.timeout-per-shutdown-phase=20s` in `application.yaml` (the default is 30 s) and `terminationGracePeriodSeconds: 30` in the Deployment. While running `kubectl get pods -w`, delete a Pod during a load test (a loop calling `GET /api/posts` with a valid token) and confirm no requests fail.
2. **Look at a real operator** (from any folder):
   ```
   kubectl get crd
   ```

   Study the CRDs that appear when you install cert-manager or Strimzi and read one custom resource YAML.
3. **Simulate node drain** (from any folder):
   ```
   minikube node add
   kubectl get nodes
   kubectl drain node-name --ignore-daemonsets
   kubectl uncordon node-name
   ```

   Confirm the PDB from Week 7 kept at least 2 Pods alive.

## 11.3 Enterprise view

- **Platform teams** run hundreds of clusters; developers get self-service namespaces. Tools such as Rancher, OpenShift Advanced Cluster Management, and Cluster API manage fleets.
- Public examples: Google runs Borg (Kubernetes' ancestor) internally; OpenAI has described running a single Kubernetes cluster at about 7,500 nodes for large machine learning training; Spotify moved from an in-house orchestrator to Kubernetes. Banks widely use OpenShift for regulated workloads.
- **Capacity and cost reviews:** quarterly right-sizing of requests, quotas, and reserved capacity.
- **Compliance:** automatic policy checks, audit logs shipped to security teams, regular penetration tests.
- Disaster recovery drills: practice failing over a region.

## 11.4 Key concepts

CRD, operator, reconciliation, informer, scheduler filter and score, Raft and quorum, graceful termination, SIGTERM/SIGKILL, CRI/CNI/CSI, KEDA, multi-cluster, Velero, RTO/RPO, service mesh, workload identity, FinOps.

## 11.5 Interview Q/A

**Q1. What is an Operator?**
A custom controller plus CRDs that encodes human operational knowledge (for example how to fail over a database), so Kubernetes manages complex apps automatically.

**Q2. Walk through what happens when a Pod is deleted.**
API marks it Terminating → endpoint removed from Services (traffic stops) and `preStop` hook runs → `SIGTERM` sent → app drains → after the grace period `SIGKILL`. Race conditions between endpoint removal and SIGTERM are why a short `preStop` sleep is common.

**Q3. How does the scheduler pick a node?**
Filtering (resources, selectors, taints, affinity, volume constraints) then scoring (spreading, resource balance, preferences), then binding.

**Q4. Why 3 or 5 etcd nodes, not 2 or 4?**
Raft needs a majority. 3 tolerates 1 failure; 5 tolerates 2. An even number adds cost without adding fault tolerance.

**Q5. How do you upgrade a production cluster safely?**
Read release notes and deprecated APIs, upgrade in a lower environment first, back up etcd, upgrade control plane then nodes in rolling batches (drain respecting PDBs), one minor version at a time, verify, keep rollback options.

**Q6. Design a highly available, multi-zone app on Kubernetes.**
3+ replicas with topology spread across zones, readiness probes, PDB, HPA, requests and limits, multi-zone worker nodes and control plane, zone-aware storage, stateless design with managed data stores replicated across zones, health-checked load balancer, and tested failover.

**Q7. How would you reduce Kubernetes cost?**
Right-size requests from real usage, autoscale (HPA plus cluster autoscaler), spot machines for tolerant workloads, remove idle namespaces, quotas, scale non-prod to zero at night, and monitor cost per team.

**Q8. Kafka consumers on Kubernetes: how do you scale?**
Scale replicas based on consumer lag (KEDA), never beyond the partition count, and handle rebalances gracefully (shutdown hooks, static membership).

**Q9. What is a service mesh and when is it worth the cost?**
A proxy layer for service-to-service traffic giving mTLS, retries, traffic splitting, and telemetry. Worth it with many services and strong security or traffic-control needs; adds complexity and resource overhead for small systems.

## 11.5b Your turn

1. Why does a rolling update sometimes drop a few requests even with readiness probes, and how do you fix it?
2. Explain in your own words how an Operator is "a human operator written as code".
3. Pick one: design a multi-zone setup for a Kafka-based trade-event service. What would you put in StatefulSets and what in Deployments?

## 11.6 Summary

Kubernetes is extensible (CRDs and Operators), built on pluggable interfaces, and designed for failure. Expert answers focus on trade-offs: availability, cost, security, and operational simplicity.

---

# WEEK 12: Capstone and Mock Interviews

## 12.1 Capstone project

Deploy **socialfeed-service** end to end like a company would:


| Requirement                                                           | Skills tested |
| --------------------------------------------------------------------- | ------------- |
| Image built by GitHub Actions, pushed to registry with commit-SHA tag | Weeks 1, 9    |
| Helm chart with dev and prod values                                   | Week 9        |
| Deployment with probes, requests and limits, rolling update           | Week 3        |
| Service plus Ingress (or Route)                                       | Week 4        |
| ConfigMap and Secret                                                  | Week 5        |
| MySQL StatefulSet with PVC (users and posts survive Pod deletion)     | Week 6        |
| HPA and PDB, spread across nodes                                      | Week 7        |
| Non-root, RBAC, NetworkPolicy                                         | Week 8        |
| Prometheus metrics and a Grafana dashboard                            | Week 10       |
| Graceful shutdown, zero failed requests during rollout                | Week 11       |
| README explaining the architecture with a diagram                     | Communication |

Stretch goals: Argo CD for GitOps, Flyway migrations instead of `ddl-auto: update`, using the already-included `spring-boot-starter-kafka` (for example publish an event when a post is created) with a consumer scaled by KEDA, a canary release with Argo Rollouts.

## 12.2 Mock interview plan


| Round | Format                                                                                |
| ----- | ------------------------------------------------------------------------------------- |
| 1     | 20 rapid-fire theory questions (Appendix A)                                           |
| 2     | Hands-on: I give a broken manifest, you fix it live (5 scenarios)                     |
| 3     | Design: "Deploy and scale a payments microservice on Kubernetes" (explain trade-offs) |
| 4     | Troubleshooting: I describe symptoms, you drive the debugging                         |
| 5     | Behavioral: "Tell me about a deployment problem you solved" (use your real work)      |

## 12.3 Summary

You should now be able to explain every layer from container to cluster to pipeline, deploy and debug a real app, and discuss trade-offs like an engineer who has done it.

---

# Appendix A: Rapid-Fire Interview Questions (with short answers)

1. **What is Kubernetes?** A system that runs and manages containers across machines to match the desired state you declare.
2. **Pod?** Smallest deployable unit; one or more containers sharing network and storage.
3. **Why not run bare Pods?** No self-healing, scaling, or updates.
4. **Deployment?** Manages ReplicaSets for rolling updates and rollbacks of stateless apps.
5. **StatefulSet?** Stable identity and storage per Pod, for stateful apps.
6. **DaemonSet?** One Pod per node.
7. **Job vs CronJob?** Run once to completion vs on a schedule.
8. **Service types?** ClusterIP, NodePort, LoadBalancer, ExternalName, headless.
9. **Ingress?** HTTP(S) routing from outside to Services, needs a controller.
10. **ConfigMap vs Secret?** Plain config vs sensitive data (base64 only, protect it further).
11. **Namespace?** Logical separation inside a cluster.
12. **Label vs annotation?** Labels are for selecting and grouping; annotations hold extra metadata not used for selection.
13. **Liveness vs readiness vs startup?** Restart if stuck; send traffic only if ready; give slow starters time.
14. **Requests vs limits?** Reserved amount vs maximum allowed.
15. **What does the scheduler do?** Picks a node for each new Pod.
16. **What does kubelet do?** Runs Pods on its node and reports status.
17. **What is etcd?** The cluster's key-value database.
18. **HPA?** Scales Pod count by metrics.
19. **Taint and toleration?** Node repels Pods; Pod tolerates the repel.
20. **PDB?** Minimum Pods that must stay up during voluntary disruptions.
21. **RBAC?** Role-based permissions for API access.
22. **NetworkPolicy?** Pod-level firewall rules.
23. **Helm?** Package manager with templates, values, and releases.
24. **CRD and Operator?** New object types and controllers that automate their lifecycle.
25. **Blue/green vs canary?** Switch all traffic between two full environments vs gradually shift a small percent.
26. **How do Pods in different Services talk?** DNS names of Services.
27. **What is a sidecar?** Helper container in the same Pod.
28. **What is an init container?** Runs to completion before the app starts.
29. **How do you view logs of a crashed container?** `kubectl logs pod --previous`.
30. **How is OpenShift different?** Kubernetes plus Routes, SCC, built-in registry, builds, console, operators.

# Appendix B: kubectl Cheat Sheet (PowerShell, run from any folder)


| Task                            | Command                                                                           |
| ------------------------------- | --------------------------------------------------------------------------------- |
| List Pods with node and IP      | `kubectl get pods -o wide`                                                        |
| List everything in a namespace  | `kubectl get all -n namespace`                                                    |
| Describe an object              | `kubectl describe pod <name>`                                                     |
| Logs (follow)                   | `kubectl logs -f <pod>`                                                           |
| Logs of previous crash          | `kubectl logs <pod> --previous`                                                   |
| Shell inside a Pod              | `kubectl exec -it <pod> -- sh`                                                    |
| Apply a file                    | `kubectl apply -f <file>`                                                         |
| Delete using a file             | `kubectl delete -f <file>`                                                        |
| Scale                           | `kubectl scale deployment <name> --replicas=5`                                    |
| Update image                    | `kubectl set image deployment/<name> <container>=<image>`                         |
| Rollout status / history / undo | `kubectl rollout status deployment/<name>` (also `history`, `undo`)               |
| Restart a Deployment            | `kubectl rollout restart deployment/<name>`                                       |
| Resource use                    | `kubectl top pods` and `kubectl top nodes`                                        |
| Recent events                   | `kubectl get events --sort-by=.lastTimestamp`                                     |
| Port forward                    | `kubectl port-forward svc/<name> 8080:80`                                         |
| Explain a field                 | `kubectl explain deployment.spec.strategy`                                        |
| Check permissions               | `kubectl auth can-i <verb> <resource>`                                            |
| Switch namespace                | `kubectl config set-context --current --namespace=<ns>`                           |
| OpenShift equivalents           | `oc get pods`, `oc project <name>`, `oc get route`, `oc rollout status deployment/<name>` |

# Appendix C: Troubleshooting Flow

```
Pod not working?
 |-- Pending ............ describe pod -> resources? selectors? PVC? quota?
 |-- ImagePullBackOff ... image name/tag? registry access? pull secret?
 |-- CrashLoopBackOff ... logs --previous -> config? secret? dependency? OOMKilled?
 |-- Running, not Ready . readiness probe path/port? app slow to start?
 |-- Ready but no traffic  endpoints empty? selector labels? targetPort? NetworkPolicy? Ingress/Route host?
 `-- Slow ............... CPU throttling? memory/GC? dependency latency? HPA at max? Kafka lag?
```

# Appendix D: Glossary (plain words)


| Term                                    | Meaning                                                                             |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| API (Application Programming Interface) | A way for programs to talk to each other                                            |
| CLI (Command-Line Interface)            | A tool you use by typing commands                                                   |
| YAML                                    | A simple indented text format for configuration files                               |
| CNCF                                    | Cloud Native Computing Foundation, the group that hosts Kubernetes                  |
| CRI / CNI / CSI                         | Container Runtime / Network / Storage Interface: plug-in standards                  |
| CRD                                     | Custom Resource Definition: a new object type added to Kubernetes                   |
| HA                                      | High Availability: keeps working when parts fail                                    |
| mTLS                                    | Mutual Transport Layer Security: both sides prove identity and traffic is encrypted |
| OCP                                     | OpenShift Container Platform                                                        |
| SCC                                     | Security Context Constraints (OpenShift's rules for what Pods may do)               |
| SLI / SLO / SLA                         | Service Level Indicator / Objective / Agreement                                     |
| OOM                                     | Out Of Memory                                                                       |
| RTO / RPO                               | Recovery Time Objective / Recovery Point Objective                                  |
| KEDA                                    | Kubernetes Event-Driven Autoscaling                                                 |
| SBOM                                    | Software Bill of Materials: list of everything inside an image                      |
| Drift                                   | When the live cluster differs from what Git says it should be                       |

---

# Appendix E: How to Use This Roadmap with Me

- Start each session by telling me the **week and section** (for example "Week 3, section 3.2").
- I explain theory first, then the simple example, then the enterprise view, then ask you the "Your turn" questions.
- Answer the questions in your own words, even if unsure. Wrong answers teach more than skipped ones.
- Share a real (sanitized) `values.yml` or error from work whenever you can, and I will map it to the concept.
- At the end of each week, say "summary" and I will quiz you on the 5 most important points.
- Never paste real passwords, tokens, or private company data into chat. Replace them with `XXXX`.

