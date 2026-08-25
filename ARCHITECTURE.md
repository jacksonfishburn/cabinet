# Architecture

## Overview

Cabinet is a self-hosted file-sync system: clients zip a directory, upload it to a named slot in a “cabinet,” and later download and extract it. The core is a Spring Boot REST API backed by PostgreSQL for metadata, the local filesystem for zip blobs, and Redis for short-lived caching of cabinet file listings. Clients are a Python CLI (primary workflow for shared cabinets and invite/join) and a React/Vite web UI (auth + default-cabinet browse/upload/download). Everything is packaged with Docker Compose for single-host deployment.

## Data Model

Hibernate `ddl-auto=update` maps JPA entities to PostgreSQL. Zip blobs live on the filesystem at `{storageDir}/{cabinetId}/{name}.zip`.

Relationships: `users 1—* cabinet_members *—1 cabinets; cabinets 1—* file_records; cabinets 1—* invite_codes`. Deleting a cabinet cascades to members, file records, and invite codes (orphan removal).

### `users`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `bigint` | PK, identity | |
| `username` | `varchar(50)` | `NOT NULL`, unique | |
| `password_hash` | `varchar(255)` | `NOT NULL` | BCrypt hash |

### `cabinets`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `bigint` | PK, identity | Path param `{cabinet}` is this id, not the name |
| `name` | `varchar(50)` | `NOT NULL`, unique | Globally unique. Default cabinets use the user id string as the name |
| `is_default` | `boolean` | `NOT NULL` | Personal cabinet created at register |

### `cabinet_members`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `bigint` | PK, identity | |
| `cabinet_id` | `bigint` | FK → `cabinets.id` | |
| `user_id` | `bigint` | FK → `users.id` | Membership is flat; no roles beyond `ROLE_USER` |

### `file_records`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `bigint` | PK, identity | |
| `name` | `varchar(255)` | `NOT NULL` | Archive slot name; uniqueness is enforced in application code per cabinet, not by a DB unique constraint |
| `size_bytes` | `bigint` | `NOT NULL` | Counts toward `CABINET_MAX_SIZE_MB` |
| `md5` | `varchar(255)` | | Hex MD5 of the zip; identical hash on re-insert skips rewriting the blob |
| `created_at` | `timestamptz` | | Set on first insert |
| `updated_at` | `timestamptz` | | Touched on insert/update |
| `cabinet_id` | `bigint` | `NOT NULL`, FK → `cabinets.id` | |

### `invite_codes`

| Column | Type | Constraints | Notes |
| --- | --- | --- | --- |
| `id` | `bigint` | PK, identity | |
| `code` | `varchar(255)` | `NOT NULL`, unique | 5-character alphanumeric (`A–Z`, `0–9`) |
| `created_at` | `timestamptz` | `NOT NULL` | |
| `expires_at` | `timestamptz` | `NOT NULL` | `created_at` + `CABINET_CODE_EXPIRATION_MS` |
| `used` | `boolean` | `NOT NULL` | Single-use; set true on successful join |
| `cabinet_id` | `bigint` | `NOT NULL`, FK → `cabinets.id` | |

## API Endpoints

Base path `/api`. Public routes: `GET /api/ping` and `/api/auth/**`. Everything else requires `Authorization: Bearer <jwt>`. Non-members of a cabinet get the same 404 as a missing cabinet. Errors are JSON `{ "error": "<message>" }`.

Jackson uses camelCase for JSON fields.

### Auth — `/api/auth`

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | JSON `{ "username", "password" }` (both required) | `200` `{ "defaultCabinetId", "username", "token" }`. Duplicate username → `409` |
| `POST` | `/api/auth/login` | Public | JSON `{ "username", "password" }` | `200` `{ "defaultCabinetId", "username", "token" }`. Bad credentials → `401` |
| `DELETE` | `/api/auth/logout` | Bearer header required by handler (route is otherwise public) | none | Empty `200`. Token is **not** revoked server-side; clients discard it locally |

### Cabinets and archives — `/api`

`{cabinet}` is a numeric cabinet **id**. `{name}` is the archive slot name (`[a-zA-Z0-9._-]+` on disk). `{code}` is the invite string.

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| `GET` | `/api/ping` | Public | none | `200` `true` (health check) |
| `GET` | `/api/list` | Bearer | none | `200` `{ "cabinets": [ { "id", "name" } ] }` |
| `POST` | `/api/create/{name}` | Bearer | none | `200` `{ "id", "name" }` for the new (non-default) cabinet |
| `GET` | `/api/peek/{cabinet}` | Bearer, member | none | `200` JSON array of file records: `{ "id", "name", "sizeBytes", "md5", "createdAt", "updatedAt" }`. Cached in Redis 10 minutes |
| `POST` | `/api/{cabinet}/{name}` | Bearer, member | raw zip bytes, `Content-Type: application/octet-stream` | `200` `{ "name", "sizeBytes", "md5" }`. Over-quota → `413`; bad name → `400`. Evicts peek cache |
| `GET` | `/api/{cabinet}/{name}` | Bearer, member | none | `200` zip bytes, `Content-Type: application/zip`. Missing slot → `404` |
| `DELETE` | `/api/{cabinet}/{name}` | Bearer, member | none | `204` empty. Missing slot → `404`. Evicts peek cache |
| `POST` | `/api/invite/{cabinet}` | Bearer, member | none | `200` invite code as a string (5 chars) |
| `POST` | `/api/join/{code}` | Bearer | none | `200` `{ "id", "name" }` of the joined cabinet |

Other statuses the API may return: `400` validation / illegal argument, `401` missing or invalid JWT, `404` cabinet or file not found, `500` storage or unexpected failure.

## Components

**Spring Boot API server.** Hosts all business logic under `/api`: auth, cabinet CRUD/membership, and archive insert/grab/peek/delete. Built with Spring Web, Spring Security (stateless), Spring Data JPA, and Bean Validation. Controllers are thin; `CabinetService` / `CabinetManagementService` / `AuthService` / `FileService` own the rules. Deployed as a multi-stage Maven → JRE image listening on **8765** inside Compose (host port from `CABINET_PORT`).

**Auth layer (JWT + BCrypt).** Registration and login hash passwords with BCrypt, issue HS256 JWTs via jjwt (`subject` = username, `uid` claim = user id), and return the user’s default cabinet id. `AuthFilter` loads the user from Postgres on each protected request and sets a Spring Security principal. `/api/ping` and `/api/auth/**` are public; everything else requires a valid Bearer token. Logout accepts a Bearer header but does not revoke or blacklist the token server-side (clients discard local credentials).

**File storage layer.** `FileService` writes and reads zip bytes on a configured directory (`CABINET_STORAGE_DIR`), with path-segment sanitization (`[a-zA-Z0-9._-]+`) to avoid traversal. Metadata (`FileRecord`: name, size, MD5, timestamps) lives in Postgres, linked to a cabinet. Per-cabinet total size is capped by `CABINET_MAX_SIZE_MB` (re-uploads of the same name get their prior size credited back into the budget). Identical MD5 on re-insert skips rewriting the blob.

**Cabinet / sharing layer.** Cabinets are first-class entities with a many-to-many membership via `cabinet_members`. Register creates a default personal cabinet (name = user id string). Additional cabinets are created by name; invite codes are 5-character alphanumeric strings stored in `invite_codes` with expiry (`CABINET_CODE_EXPIRATION_MS`) and single-use semantics. Membership is flat — any member can peek/insert/grab/delete/invite; there are no roles beyond `ROLE_USER`.

**Redis peek cache.** Spring Cache + `RedisCacheManager` caches `CabinetService.peek` by cabinet id for 10 minutes; insert and delete `@CacheEvict` that entry. Values are JSON-serialized. This is the only Redis usage in application code.

**Python CLI.** Single-file `requests` client installed via `install.sh` into `~/.cabinet`. It owns local config (server URL, JWT, default/active cabinet ids, name→id map), zips the cwd before upload, and extracts on grab. Implements the full surface including list/create/open/close, invite, and join — the path meant for shared-cabinet workflows.

**React web UI.** Vite + React 19 + Tailwind + JSZip, served by Nginx in Compose. Talks to `/api` (proxied to the backend). Covers register/login/logout and default-cabinet peek/upload/download/delete; `api.ts` also has list/create helpers, but the UI does not expose invite/join or multi-cabinet switching. Upload builds a zip in the browser before POSTing raw octets.

**Schema management.** There is no Flyway/Liquibase. Hibernate `ddl-auto=update` creates/updates tables from JPA entities at startup. Tests use Testcontainers (Postgres + Redis) for integration coverage of auth and cache behavior.

## Data Flow: Key Operations

### 1. User registers

1. Client `POST /api/auth/register` with `{ username, password }`.
2. `AuthService` rejects duplicate usernames; BCrypt-hashes the password; persists `User`.
3. `CabinetManagementService.createDefaultCabinet` creates a cabinet (`isDefault=true`) and a `CabinetMember` row linking the user.
4. `JwtService` issues a JWT; response returns `{ defaultCabinetId, username, token }`.
5. CLI/web store the token (and default cabinet id) locally for subsequent authenticated calls.

### 2. Insert (upload) an archive

1. Client zips content locally (CLI: cwd; web: JSZip from selected files) and `POST /api/{cabinetId}/{name}` with raw `application/octet-stream` body + Bearer token.
2. `AuthFilter` validates JWT and sets the `User` principal.
3. `CabinetService.insert` loads the cabinet, verifies membership (non-members get the same not-found path as missing cabinets), checks per-cabinet size, and computes MD5.
4. If a `FileRecord` with that name exists and MD5 matches → touch `updatedAt` only (no disk write). If MD5 differs → update metadata and overwrite `{storage}/{cabinetId}/{name}.zip`. If new → persist metadata then write the zip.
5. `@CacheEvict` clears the Redis `peek` entry for that cabinet id; response is `{ name, sizeBytes, md5 }`.

[DIAGRAM: sequence diagram for insert — participants: {CLI or Web UI, AuthFilter, CabinetService, FileService, PostgreSQL, Redis, filesystem volume}. Key steps: {JWT auth → membership check → size/MD5 → upsert FileRecord → write/skip zip → evict peek cache → InsertResponse}]

### 3. Invite and join a shared cabinet

1. Member `POST /api/invite/{cabinetId}` → membership check → 5-char code generated (retry on collision) → `InviteCode` saved with expiry → code returned as plain text.
2. Another authenticated user `POST /api/join/{code}` → lookup code; reject if missing, used, or expired → create `CabinetMember` → mark code used → return `{ id, name }` for the joined cabinet.
3. CLI stores the name→id mapping and can `open` that cabinet for subsequent peek/insert/grab/delete.

(Peek and grab are simpler membership-gated reads: peek may hit Redis; grab always streams the zip from disk into a `byte[]` response with `Content-Type: application/zip`.)
