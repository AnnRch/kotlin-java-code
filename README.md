Back-end service overview

A high-performance, non-blocking reactive backend service built using Spring Boot WebFlux, Kotlin Coroutines, R2DBC (PostgreSQL), and Redis. The system syncs data asynchronously with a remote developer board upstream API, maintaining localized data consistency with high-speed caching layers.

🏗️ Architectural Sync Sequence Diagram

When an endpoint is hit (e.g., fetching details or profile syncs), the system coordinates actions across layers non-blockingly:

[ Client ] ---> [ WebFlux Controller ]
|
v
[ Developer / Job / Company Service ]
|
+--------------+--------------+
|                             |
v                             v
[ Redis Cache Layer ]         [ PostgreSQL (R2DBC) ]
(Short-Circuit Hit)           (Fallback Query)
|                             |
| (If Cache/DB Miss)          |
v                             |
[ WebClient (Remote API) ] ----------+
|
v
[ Async Side-Effects ] ---> (Update Redis Cache & Evict Tags collections)


🛠️ Prerequisites & Local Setup

1. Environment Requirements

Java 21 / Kotlin 1.9+

Docker & Docker Compose (For running PostgreSQL and Redis infrastructure dependencies)

2. Spinning up Local Infrastructure

Run the following terminal commands to provision your local database and cache:

docker run --name jobs-postgres -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres:16
docker run --name jobs-redis -p 6379:6379 -d redis:7-alpine


3. Application Configurations (application.yaml)

Your localized configurations are set up to run against localhost automatically using safe fallback defaults:

spring:
r2dbc:
url: r2dbc:postgresql://localhost:5432/postgres
username: postgres
password: secret
data:
redis:
host: localhost
port: 6379


📡 API Endpoint Reference (Localhost)

All endpoints run on http://localhost:8080.

1. Synchronize & Fetch Jobs

Endpoint: GET /api/v1/jobs

Flow Pipeline:

Requests recent data from the Remote Upstream API.

Pipelines incoming items through a non-blocking flatMapMerge (concurrency limit: 10).

Cache Check: Checks Redis by Job ID. If present, it flows forward instantly.

Database Mutation: Relational misses upsert into PostgreSQL natively via an atomic SQL structure.

Asynchronous Hand-off: Fires background coroutines to populate the Redis cache layer and evict global tag tracking safely.

2. Targeted Job Sync

Endpoint: GET /api/v1/jobs/{id}

Flow Pipeline:

Performs a fast path lookup inside Redis. Returns immediately if hit.

Falls back to PostgreSQL via R2DBC if missed.

Queries the remote API if missing everywhere, updates PostgreSQL, and dispatches a background worker to cache the missing DTO structure.

3. Company Profile Sync

Endpoint: GET /api/v1/companies/{slug}

Flow Pipeline:

Pulls the latest corporate statistics from the upstream API.

Compares hashes via hasChanged logic against the local cache.

Triggers incremental state calculation cascades via database level reactive triggers.

4. Search Metrics Analytics

Endpoint: GET /api/v1/analytics/search?requestedDays={days}

Flow Pipeline:

Evaluates input ranges (1..90 days). Falls back to a 30-day baseline context automatically.

Reads from pre-aggregated analytics blocks maintained by the materialized view mv_search_analytics_summary_30_days.

⏰ Cron & Automated Maintenance

View Maintenance Scheduler

Execution Interval: 0 0 * * * * (Every Hour on the hour)

Mechanics: The scheduling component triggers refreshMaterializedView() non-blockingly using Spring's native coroutine support (suspend fun). This runs a concurrent database maintenance task to refresh the materialized summaries without locking incoming client search queries.