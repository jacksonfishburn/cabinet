# Cabinet

## What Cabinet is

Cabinet is a self-hosted personal file-sync tool. You point it at a folder, it zips it and pushes it to a server. Pull it back anywhere. Think lightweight Dropbox or simplified Git — for one person, self-hosted on a VPS.

It is also a learning project, so code quality and understanding the "why" matter, not just getting things working.

## Stack

- **Backend:** Spring Boot 4 REST API (Java), running in Docker
- **Database:** PostgreSQL, running in Docker
- **Frontend:** React + TypeScript + Vite — mostly deprioritized, don't focus here
- **CLI:** Python script (`cabinet.py`), shell function installed in `~/.bashrc`

## What's already built

### Backend API

All endpoints are working:

|Method|Route|Auth|Description|
|---|---|---|---|
|GET|`/api/ping`|No|Health check|
|GET|`/api/peek`|Yes|List all stored items for the current user|
|POST|`/api/{name}`|Yes|Upload a zip (raw bytes in body)|
|GET|`/api/{name}`|Yes|Download a zip|
|DELETE|`/api/{name}`|Yes|Delete an item|
|POST|`/api/auth/register`|No|Create account, returns token|
|GET|`/api/auth/login`|No|Verify credentials, returns token|
|DELETE|`/api/auth/logout`|Yes|Revoke current token|

### Authentication (current — token-based, not JWT)

- Bearer token auth on all protected routes: `Authorization: Bearer <token>`
- Tokens stored in the `api_tokens` table with `created_at`, `expires_at`, `revoked` columns
- Spring Security filter validates token on every request by hitting the database
- Passwords hashed with bcrypt

### File storage

- Zips stored at `cabinet-storage/{username}/{name}.zip`
- Metadata (size, MD5, timestamps) in PostgreSQL `file_records` table
- Uploading to an existing name overwrites and updates `updated_at` and `md5`
- Upload size limit configurable via env var, default ~100 MB

### Database schema

**users:** `id`, `username` (unique), `password_hash`

**api_tokens:** `id`, `token`, `created_at`, `expires_at`, `revoked`, `user_id`

**file_records:** `id`, `name`, `size_bytes`, `md5`, `created_at`, `updated_at`, `user_id`

### CLI

Python script with these commands:

- `cabinet insert <name>` — zip current dir and upload
- `cabinet fetch <name>` — download and unzip to current dir
- `cabinet peek` — list stored items
- `cabinet delete <name>` — delete a stored item
- `cabinet login <name> <password>` — log in, saves token to config
- `cabinet register <name> <password>` — create account, saves token to config

Config lives at `~/.cabinet/config.json` with `serverUrl` and `token`.

### Frontend

React + TypeScript + Vite. Has a working UI for browse/upload/download/delete and a login/register screen. **Not a priority — don't spend time on this unless asked.**

### Docker

Backend and database are containerized in `docker-compose.yml`. Frontend is not yet dockerized.

### Error handling

Improved error handling is in place across the CLI and backend.

---

## Roadmap — what to build next (in order)

### 1. JWT Auth (next up)

Replace the current database-backed token system with JWTs. Goals:

- Stateless auth — no DB lookup on every request
- Tokens self-contained with expiry, user info, etc.
- Keep the same API surface (Bearer token in header)
- The `api_tokens` table and logout endpoint may need rethinking — JWTs can't be truly revoked without a denylist, decide on the right tradeoff

### 2. Testing and logging

- Unit and integration tests for the backend (Spring Boot — use JUnit + Mockito, Spring Boot Test)
- Make Test Containers and setup testing environment for test driven development moving forward
- just set up some type of logging for debugging moving forward

### 3. Caching for auth and peek

- Cache token validation results so repeated requests don't hit the DB every time (especially relevant before JWT, but may still be useful after)
- Cache the `GET /api/peek` response per user — invalidate on upload/delete
- Consider Spring Cache abstraction with a simple in-memory store (Caffeine) first before reaching for Redis

### 4. Shared cabinets

- Allow a user to share a named cabinet item with another user (read-only or read-write)
- New DB table needed: something like `shares` with `owner_user_id`, `target_user_id`, `file_record_id`, `permission`
- API endpoints TBD — think through what makes sense
- CLI commands TBD

## General guidance

- This is a learning project
- Prefer simple and clear over clever
- add only the parts I tell you to, work on piece of code at a time