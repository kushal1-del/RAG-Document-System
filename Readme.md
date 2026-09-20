# RAG Document Q&A System

A production-style, full-stack multi-tenant RAG (Retrieval-Augmented Generation) application. Users register, upload documents (PDF, DOCX, TXT), and ask natural-language questions answered strictly from the content of their own documents — grounded, not hallucinated. Includes hybrid semantic+keyword search, an admin panel, and a Bring-Your-Own-Key (BYOK) system for answer generation.

---

## Table of Contents

1. [Tech Stack](#1-tech-stack)
2. [Architecture Overview](#2-architecture-overview)
3. [Core RAG Pipeline](#3-core-rag-pipeline)
4. [Configuration Reference (`application.properties`)](#4-configuration-reference-applicationproperties)
5. [Features — Simple to Complex](#5-features--simple-to-complex)
6. [BYOK System (Bring Your Own Key)](#6-byok-system-bring-your-own-key)
7. [Frontend](#7-frontend)
8. [API Endpoint Reference](#8-api-endpoint-reference)
9. [Key Engineering Decisions & Constraints](#9-key-engineering-decisions--constraints)
10. [Bugs Found & Root Causes](#10-bugs-found--root-causes)
11. [Working Conventions](#11-working-conventions)
12. [Roadmap / What's Left](#12-roadmap--whats-left)

---

## 1. Tech Stack

### Backend
| Technology | Role | Why chosen |
|---|---|---|
| Java 17 + Spring Boot 3.5.4 | Core framework | Mature DI/REST ecosystem, compile-time type safety |
| PostgreSQL | Relational data | Users, documents, API keys — structured, ACID |
| Qdrant (server 1.19.0 / client 1.15.0) | Vector database | Native dense + sparse vector support, built-in RRF fusion |
| Google Gemini API | Embeddings + generation | `gemini-embedding-001` (768-dim, fixed) for search; a configurable chat model for answers |
| Apache PDFBox / Apache POI | Document parsing | Extract text from PDF / DOCX before chunking |
| Spring Security + JWT (jjwt) | Authentication | Stateless, standard for REST APIs |
| BCrypt | Password hashing | Industry-standard slow hash |
| AES-256-GCM (custom `EncryptionService`) | API key encryption | User-supplied AI provider keys must never be stored as plain text |
| Bucket4j | Rate limiting | Token-bucket algorithm, no external infra needed |
| SLF4J + Logback | Logging | Leveled, timestamped, filterable |

### Frontend
| Technology | Role | Why chosen |
|---|---|---|
| React + Vite | UI framework / build tool | Fast dev server, hot reload |
| Tailwind CSS v4 | Styling | Utility-first, custom theme tokens |
| react-router-dom | Routing | Protected / admin-only route guards |
| axios | HTTP client | Interceptors auto-attach JWT and handle 401s globally |

### Infrastructure
- Docker containerization (in progress — see [Roadmap](#12-roadmap--whats-left))
- Google AI Studio dashboard used as the source of truth for quota/rate-limit confirmation

---

## 2. Architecture Overview

```
Frontend (React)
   │  JWT in Authorization header (axios interceptor)
   ▼
Spring Boot REST API
   │
   ├── Auth (JWT + BCrypt)
   ├── Document ingestion (PDFBox/POI → chunk → embed → store)
   ├── RAG query (embed question → hybrid search → prompt → Gemini)
   ├── Admin (user/document management)
   └── BYOK (per-user API keys, encrypted, for answer generation)
   │
   ├── PostgreSQL (users, documents, api keys, ai settings)
   └── Qdrant (dense + sparse vectors, per-chunk payload)
```

Every document belongs to exactly one user (`ownerId`), and every vector-search call is filtered by that `ownerId` at the query level — not just gated at the API layer. This was explicitly verified with two separate test accounts confirming neither could retrieve the other's data.

---

## 3. Core RAG Pipeline

### Ingestion (on upload)
```
PDF/DOCX/TXT upload
  → PDFBox / Apache POI text extraction
  → TextChunker (sentence-aware — BreakIterator, never cuts mid-sentence)
  → for each chunk (batched, concurrent):
       Gemini dense embedding (768-dim)
       SparseVectorService term-frequency vector (local, no API call)
  → both vectors + payload (documentId, chunkIndex, content, ownerId)
    stored as one Qdrant point, upserted in batches of 100
```

### Retrieval (on question)
```
Question
  → Gemini embeds the question (dense)
  → SparseVectorService encodes the question (sparse)
  → Qdrant runs dense + sparse prefetch queries in parallel
  → Reciprocal Rank Fusion (RRF) merges both rankings
  → top-K results filtered by ownerId (always) + documentId (optional)
  → chunk text inserted into the prompt
  → Gemini generates the final answer
```

**Why hybrid, not dense-only:** validated with real documents — dense/semantic search alone can under-rank exact facts (specific numbers, names). Sparse/BM25-style search catches these via IDF-weighted exact-term matching. Confirmed via a "Show sources" toggle in the UI that displays the true post-fusion ranked chunks behind every answer — not a separate or pre-fusion view.

---

## Quick Start

### Prerequisites

- Java 17
- Maven (or the included Maven Wrapper)
- Node.js and npm
- PostgreSQL
- Qdrant
- A Google Gemini API key

### Repository Structure

```text
rag-document-system/
├── rag-backend/
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
├── rag-frontend/
│   ├── src/
│   ├── public/
│   └── package.json
├── screenshots/
├── .gitignore
└── README.md
```

### Backend

```bash
cd rag-backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd rag-backend
.\mvnw.cmd spring-boot:run
```

Configure your local database, Qdrant connection, Gemini API key, JWT secret, and encryption secret using your local environment/profile configuration. **Do not commit real secrets to GitHub.**

### Frontend

```bash
cd rag-frontend
npm install
npm run dev
```

The frontend is built with React + Vite and communicates with the Spring Boot REST API.

> For the exact configuration values and application properties used by this project, see the Configuration Reference section below.

## 4. Configuration Reference (`application.properties`)

Every tunable value is externalized — nothing load-bearing is hardcoded in Java.

### Database & core
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ragdb
spring.datasource.username=postgres
spring.datasource.password=<YOUR_DB_PASSWORD>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Gemini models
```properties
gemini.api.key=<YOUR_GEMINI_API_KEY>
gemini.embedding.model=gemini-embedding-001
gemini.embedding.dimensions=768
gemini.chat.model=...
```
- `gemini.api.key` — the application's own key. Used for **all embeddings, always**, and for chat generation during a user's free trial.
- `gemini.embedding.model` / `dimensions` — fixed permanently at 768-dim `gemini-embedding-001`. Must never vary per-user (see [§9](#9-key-engineering-decisions--constraints)).
- `gemini.chat.model` — safe to change; doesn't touch stored vectors.

### Qdrant
```properties
qdrant.host=localhost
qdrant.port=6334
qdrant.collection.name=documents
```

### JWT
```properties
jwt.secret=<YOUR_JWT_SECRET>
jwt.expiration.ms=86400000
```
Secrets live in `application-local.properties` (git-ignored); container networking values live in `application-docker.properties`.

### Logging
```properties
logging.level.root=INFO
logging.level.com.kushal.rag=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n
```
Root stays `INFO` to quiet framework noise; the app's own package logs at `DEBUG` for per-chunk visibility during development.

### RAG search / pagination / rate limiting
```properties
rag.search.default-limit=5
pagination.default-page-size=20
rate-limit.register.capacity=5
rate-limit.register.refill-minutes=60
```

### File upload limits
```properties
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=25MB
server.tomcat.max-http-form-post-size=25MB
```
All three must agree — Tomcat has its own separate ceiling from Spring's multipart config. (A 5MB PDF once silently failed because Spring's default is only 1MB — see [§10](#10-bugs-found--root-causes).)

### Embedding batching & concurrency
```properties
gemini.embedding.batch-size=5
embedding.concurrency=1
```
Batching groups multiple chunks into one Gemini API call (fewer round-trips). Concurrency controls how many batches run at once. **Critical distinction:** batching reduces call *overhead*, but each individual text still counts against Gemini's quota (RPM/RPD) — batching is a throughput win, never a quota bypass.

### Chunking
```properties
chunking.target-size=1000
chunking.overlap-sentences=2
chunking.large-doc-char-threshold=200000
chunking.large-doc-target-size=2500
```
Normal documents chunk at ~1000 characters. Documents over 200,000 characters (full books) automatically switch to 2500-character chunks — fewer total chunks and embedding calls, trading a little retrieval precision for staying within API quotas at scale.

---

## 5. Features — Simple to Complex

### 5.1 — User & Document entities
Plain JPA entities: `User` (email, BCrypt password, `Role`, `enabled` flag), `Document` (id, fileName, storageKey, status, ownerId). The foundation everything else builds on.

### 5.2 — Authentication (JWT)
Register/login endpoints, BCrypt hashing, a `JwtService` that signs/verifies tokens, and a `JwtAuthFilter` validating the `Authorization` header before any request reaches a controller. Stateless — no server-side session storage.

### 5.3 — Multi-tenancy
Every `Document` row and every Qdrant point carries an `ownerId`. Every search/ask call filters by the logged-in user's ID at the query level, not just the API gate.

### 5.4 — Document-scoped search
An optional `documentId` parameter on search/ask, restricting a question to one specific document instead of blending across a user's whole library.

### 5.5 — Admin panel (backend + frontend)
Role-protected `/api/admin/**` routes; paginated user/document lists; create-user-with-any-role endpoint (public registration is always `USER`); soft delete (disable/enable, reversible) and hard delete (cascades to documents + Qdrant vectors, irreversible). Full React admin UI with tabs, search, and status buckets.

### 5.6 — Hybrid search (dense + sparse + RRF)
Qdrant collection recreated with **named** dense and sparse vectors per point. A `SparseVectorService` built from scratch in Java (tokenize → hash to a stable non-negative int → term-frequency count) — no external ML model needed. Qdrant's `Modifier.Idf` applies BM25-style IDF weighting automatically at query time. Search fires dense and sparse prefetch queries in parallel, fused via Qdrant's native RRF.

### 5.7 — Production hardening
- Bean Validation (`@NotBlank`/`@Email`/`@Size`) on every request DTO
- Explicit CORS configuration for the separate frontend origin
- All `System.out.println` replaced with SLF4J leveled logging
- Every hardcoded value externalized to `application.properties`
- Spring Data pagination on admin endpoints
- Bucket4j rate limiting on registration with a proper `429` response
- `GlobalExceptionHandler` returning clean JSON errors app-wide

### 5.8 — Full React frontend
Login/Register pages; JWT stored client-side, auto-attached via an axios request interceptor; a response interceptor that clears the token and redirects to `/login` on any `401` (graceful expiry handling); protected and admin-only route guards; a Dashboard with document upload, live search/filter, newest-first ordering, hover-to-delete with confirmation; a chat-style Ask interface with message bubbles, auto-clearing input, auto-scroll, and a typing indicator; a full light/dark theme system persisted in `localStorage`.

### 5.9 — Source-chunk transparency
A "Show sources" toggle in the chat UI that, when on, also fires the raw `/api/search` call and displays the actual retrieved chunks (content, chunk index, similarity score) behind each answer in an expandable panel. Confirmed to be the true post-fusion RRF-ranked results — genuine answer verifiability.

### 5.10 — Ingestion performance rework
Triggered by a real incident: uploading a 600-page book (2,316 chunks) took an extremely long time under the original one-call-per-chunk design.
- `TextChunker` rewritten using Java's `BreakIterator` for real sentence-boundary detection (correctly handles abbreviations like "Mr.") — chunks never split mid-sentence; overlap repeats whole trailing sentences.
- Large documents (>200,000 chars) automatically use a bigger target chunk size.
- Embedding pipeline batches multiple chunks per Gemini call with configurable concurrency, plus exponential-backoff retry for transient `429`s.
- Qdrant storage batch-upserts up to 100 points per call instead of one call per chunk.
- Root cause confirmed precisely via Google AI Studio's dashboard: Gemini's free tier hard-caps at ~1,000 embedding requests/day. This clarified that batching helps throughput but can never bypass a quota — the real fix for huge documents is bigger chunks, splitting uploads across days, or a paid tier.

---

## 6. BYOK System (Bring Your Own Key)

A self-serve system letting users control their own AI cost/usage for answer generation, built as clean separate entities rather than bolted onto `User`.

- **`UserAiSettings`** — one row per user: `generationMode` (`FREE_TRIAL` / `MY_API_KEY`), `selectedApiKeyId`, `freeCallsAllowed` (default 5), `freeCallsUsed`.
- **`UserApiKey`** — one row per configured key: `provider` (enum, currently `GEMINI` — modeled this way so OpenAI/Anthropic can be added later without a schema change), `label`, `encryptedKey`, `expiresAt`, `enabled`.
- **AES-256-GCM encryption** — a fresh random IV per encryption, master secret from an environment variable, never stored in the database itself.
- **Free-call counter increments only after a successful generation** — a failed Gemini call never costs the user one of their five free tries.
- **Expiry enforced server-side** on every use — an expired or disabled key is rejected before ever reaching Gemini.
- **Ownership enforced at the repository layer** (`findByIdAndUserId`) — a user can never select or act on another user's key by guessing an ID.
- **Full settings page UI** (`/ai-settings`) mirroring Login/Register's visual style: mode switcher with a live progress bar for remaining free calls, a list of configured keys with Select/Disable actions, and an add-key form with a masked/show-toggle password field plus decoy autofill-absorbing inputs to stop browsers mis-filling the real API key field with saved credentials.

**Hard rule:** BYOK applies **only** to chat generation, never to embeddings. Embeddings stay fixed on the application's own key/model always — see [§9](#9-key-engineering-decisions--constraints) for why.

---

## 7. Frontend

```
src/
├── api/          — axios call functions (client.js has the JWT interceptor + 401 handler)
├── pages/         — Login, Register, Dashboard, Admin, AiSettings
├── components/    — Button, Input, Navbar, DocumentItem, Pagination, ThemeToggle, route guards
└── context/       — AuthContext, ThemeContext
```

- **Theming:** purple/white/black with gradients, class-based dark mode (`.dark` on `<html>`), toggle persisted in `localStorage`.
- **Design language:** Fraunces (display serif) + Inter (body) + IBM Plex Mono (technical/data labels like chunk scores).
- **Navigation:** all internal links use React Router's `<Link>`, never raw `<a href>` (see [§10](#10-bugs-found--root-causes)).

---

## 8. API Endpoint Reference

### Public
- `POST /api/auth/register`
- `POST /api/auth/login`

### Authenticated (any user)
- `POST /api/documents/upload`
- `GET /api/documents` — list own documents
- `GET /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `POST /api/search` — raw hybrid search results
- `POST /api/ask` — full RAG answer
- `GET /api/ai/settings`
- `PUT /api/ai/settings/mode` — switch `FREE_TRIAL` / `MY_API_KEY`
- `POST /api/ai/keys` — add a provider API key
- `GET /api/ai/keys` — list own keys (never returns the raw/encrypted key)
- `POST /api/ai/keys/{id}/select`
- `DELETE /api/ai/keys/{id}` — disable

### Admin-only
- `GET /api/admin/users` (paginated)
- `GET /api/admin/documents` (paginated)
- `POST /api/admin/createUser`
- `PATCH /api/admin/users/{id}/disable`
- `PATCH /api/admin/users/{id}/enable`
- `DELETE /api/admin/users/{id}` — hard delete, cascades

---

## 9. Key Engineering Decisions & Constraints

- **Never assume API method signatures.** Always verify via IDE autocomplete before writing code. Established after repeated Qdrant Java-client API mismatches caused compile-error chains; applied consistently afterward to Qdrant, jjwt, the Gemini Java SDK, Bucket4j, and React Router.
- **Embedding models must never be mixed within a Qdrant collection, even at the same output dimension.** Two models can both output "768 numbers," but each learns its own internal meaning space — comparing vectors across models is mathematically meaningless. This is the entire reason BYOK is scoped to chat generation only, never embeddings.
- **Gemini's free tier hard-caps at ~1,000 embedding requests/day**, confirmed via the Google AI Studio dashboard rather than assumed. Batching improves throughput but cannot bypass quota.
- **Qdrant upserts are batched at 100 points per call**, not one call per chunk.
- **Frontend partial edits are unreliable.** The established fix for recurring merge errors (duplicate imports, misplaced hooks, stray fragments left outside a component body) is always: select-all, delete, paste the file fresh — never a partial patch.
- **Configuration-first.** Every tunable value — chunk sizes, batch sizes, concurrency, rate limits, pagination defaults, file upload limits, model names, JWT expiry, collection name — lives in `application.properties`, never hardcoded.

---

## 10. Bugs Found & Root Causes

| Symptom | Root cause | Fix |
|---|---|---|
| Qdrant `NullPointerException` on search | Old points (pre-payload code) mixed with new points in the same collection | Deleted and recreated the collection after payload logic was finalized |
| Repeated Qdrant Builder API errors | Assumed the protobuf-generated client's API shape instead of checking the actual installed version | Adopted the "verify via autocomplete first" rule (see §9) |
| React "multiple copies of React" / blank white screen | `npm install` run from the wrong (parent) folder created a stray second `node_modules` sibling | Deleted the stray installs; made "check `pwd` before any `npm` command" a standing habit |
| Recurring "identifier already declared" errors | Manual merges left old fragments behind — duplicate imports, hooks/handlers placed outside the component body | "Select-all, delete, paste fresh" for any file touched by a merge |
| Anchor tags silently losing their opening bracket on paste | Unknown — something in the copy/paste path stripped `<a href=...>` markup specifically | Switched to React Router's `<Link>` for all internal navigation |
| All existing users showing as "disabled" after adding the `enabled` field | Existing Postgres rows predated the column; Java's `= true` default only applies to newly constructed objects, not retroactively | One-time SQL: `UPDATE app_user SET enabled = true WHERE enabled IS NULL` |
| 5MB PDF upload silently failing | Spring Boot's default max upload size is 1MB | Added `max-file-size` / `max-request-size` / `max-http-form-post-size` together |
| Java "variable must be final or effectively final" in a lambda | A for-loop index referenced directly inside a lambda (e.g. an exception-message supplier) | Captured the loop variable into a separate `final` local before use |
| Book upload extremely slow / eventually failing | Sequential one-embedding-call-per-chunk design colliding with Gemini's free-tier daily quota | Batched + concurrent embedding calls, batched Qdrant upserts, automatic larger chunk sizing for large documents |

---

## 11. Working Conventions

- Strict compile-first discipline: every external API call verified before being written, never assumed from docs or memory.
- Secrets separated by Spring profile: `application-local.properties` (git-ignored) for local secrets, `application-docker.properties` for container networking values.
- Frontend code changes delivered as complete file replacements, not partial patches, to avoid merge-fragment bugs.
- Strong emphasis throughout on correctness and production-readiness over speed — this is built as a deployable application, not a prototype.

---

## Security Notes

- Never commit `.env`, local secret configuration files, API keys, database passwords, JWT secrets, or encryption secrets.
- User-supplied API keys are encrypted with AES-256-GCM before storage.
- API-key ownership is enforced at the repository layer.
- Document and vector access is scoped by `ownerId`.
- Replace all placeholder values with your own local configuration before running the application.

## 12. Roadmap / What's Left

- **Docker containerization** — in progress as of the most recent session.
- Multi-provider BYOK (OpenAI/Anthropic) — the `provider` enum was deliberately designed to support this without a schema change.
- Conversational memory (multi-turn chat sessions).
- Additional document types beyond PDF/DOCX/TXT (e.g. Markdown, images/tables via a structured-extraction service).
