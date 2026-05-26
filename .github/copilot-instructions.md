# Cabinet

Cabinet is a personal file-sync tool — a lightweight alternative to Dropbox or a simplified Git — built for moving directories between machines. You point it at a folder, it zips it and pushes it to your server. Pull it back anywhere. 

Built as both a personal utility and a learning project, Cabinet is self-hosted and designed to be straightforward to run. A demo/test mode is planned so visitors can explore the interface without actually uploading files.

---

## Architecture Overview

Cabinet has three main components: a Spring Boot REST API backend, a PostgreSQL database, and a React frontend — all containerized with Docker. A Python CLI provides the primary day-to-day interface.

```
┌─────────────────┐     HTTP      ┌──────────────────────────┐
│   Python CLI    │ ────────────► │   Spring Boot REST API   │
└─────────────────┘               │        (Docker)          │
                                  └────────────┬─────────────┘
┌─────────────────┐     HTTP               ▼
│  React Frontend │ ────────────►  ┌───────────────────┐    ┌────────────────────┐
│    (Docker)     │               │   PostgreSQL DB    │    │  cabinet-storage/  │
└─────────────────┘               │    (Docker)        │    │  (flat .zip files) │
                                  └───────────────────┘    └────────────────────┘
```

---

## Backend

A Spring Boot 3 REST API running on a VPS behind Docker. Handles file storage, user authentication, and all business logic.

### API Endpoints

|Method|Route|Auth|Description|
|---|---|---|---|
|GET|`/api/ping`|❌|Health check, returns `true`|
|GET|`/api/peek`|✅|List all stored items for your account|
|POST|`/api/{name}`|✅|Upload a zip (raw bytes in body)|
|GET|`/api/{name}`|✅|Download a zip|
|DELETE|`/api/{name}`|✅|Delete an item|
|POST|`/api/auth/register`|❌|Create an account, returns a token|
|GET|`/api/auth/login`|❌|Verify credentials, returns a token|
|DELETE|`/api/auth/logout`|✅|Revoke the current token|

### Authentication

Every request except `/api/ping` and the `/api/auth/*` routes requires a Bearer token:

```
Authorization: Bearer <token>
```

Tokens are stored in the database with `created_at`, `expires_at`, and `revoked` columns. A Spring Security filter validates the token on every protected request. Passwords are hashed with bcrypt.

### File Storage

Files are stored as `.zip` archives under `cabinet-storage/{username}/{name}.zip`, giving each user their own isolated directory. Metadata (size, MD5 hash, timestamps) is persisted in PostgreSQL. Uploading to an existing name overwrites the file and updates `updated_at` and `md5` in the database.

**Upload limit:** configurable via environment variable (default ~100 MB).

### Response Shapes

**`GET /api/peek`** — returns a list of FileRecords:

json

```json
[
  {
    "name": "my-project",
    "sizeBytes": 204800,
    "md5": "a1b2c3d4...",
    "createdAt": "2025-05-01T12:00:00",
    "updatedAt": "2025-05-01T14:30:00"
  }
]
```

**`POST /api/{name}`** — body is raw zip bytes (`application/octet-stream`), returns the FileRecord for the inserted item.

**`GET /api/{name}`** — returns raw zip bytes (`application/octet-stream`).

**`DELETE /api/{name}`** — returns 200 on success

---

## Database

PostgreSQL, running as a second Docker service alongside the API.

### Schema

**`users`**

|Column|Type|Notes|
|---|---|---|
|`id`|serial|Primary key|
|`username`|text|Unique|
|`password_hash`|text|bcrypt|

**`api_tokens`**

|Column|Type|Notes|
|---|---|---|
|`id`|serial|Primary key|
|`token`|text|Unique|
|`created_at`|timestamp||
|`expires_at`|timestamp||
|`revoked`|boolean||
|`user_id`|FK|Many-to-one → `users`|

**`file_records`**

|Column|Type|Notes|
|---|---|---|
|`id`|serial|Primary key|
|`name`|text|Key used to store/retrieve|
|`size_bytes`|bigint||
|`md5`|text||
|`created_at`|timestamp||
|`updated_at`|timestamp||
|`user_id`|FK|Many-to-one → `users`|

---

## Docker Setup

The backend and database run as separate services defined in `docker-compose.yml`. The React frontend will be added as a third service once its Dockerfile is written.

```
services:
  backend  — Spring Boot app
  db       — PostgreSQL
  frontend — React/Vite (planned)
```

---

## CLI

A Python script (`cabinet.py`) that wraps the REST API and provides a shell-friendly interface. Installed as a shell function in `~/.bashrc`, which calls the script and forwards all arguments.

**Configuration** lives in `~/.cabinet/config.json`:

```json
{
  "serverUrl": "https://your-vps.example.com",
  "token": "your-api-token"
}
```

**Commands:**

|Command|Description|
|---|---|
|`cabinet insert <name>`|Zips the current directory and uploads it|
|`cabinet fetch <name>`|Downloads and unzips to the current directory|
|`cabinet peek`|Lists all your stored items|
|`cabinet delete <name>`|Deletes a stored item|

> **Note:** Login/register support is not yet wired into the CLI. For now, the token is set manually in the config file.

---

## Frontend (GUI)

A React + TypeScript frontend built with Vite. Currently functional for browsing, uploading, downloading, and deleting files, with a polished UI. Will be containerized in its own Docker service.

**Planned additions:**

- Login / register screen
- CLI download page

> **Note:** Auth is not yet integrated into the frontend.

---

## Roadmap

- [ ] Wire auth into the CLI (login/register commands, auto-save token to config)
- [ ] Improve Error Handling
- [ ] Add login/register screen to the React frontend
- [ ] Dockerize the frontend
- [ ] Deploy
- [ ] CLI download page on the frontend
- [ ] Demo / test mode (explore the UI without uploading real files)
- [ ] Rate limiting or upload quotas to protect the server
- [ ] Per-user file namespacing (currently files are global by name)