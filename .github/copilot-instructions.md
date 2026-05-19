# Cabinet — Frontend Build Context

## What is Cabinet?

A personal file-sync tool (mini Dropbox/Git) for moving directories between machines. You zip a directory, push it to a server, pull it back anywhere. Currently fully functional as a CLI + VPS backend.

---

## Backend (already built & deployed)

Spring Boot 3 REST API running on a VPS behind Docker. All routes live under `/api/`.

### Endpoints

| Method | Route         | Auth | Description                      | command |
| ------ | ------------- | ---- | -------------------------------- | ------- |
| GET    | `/api/ping`   | ❌    | Health check, returns `true`     | n/a     |
| GET    | `/api/peek`   | ✅    | List all stored items            | peek    |
| POST   | `/api/{name}` | ✅    | Upload a zip (raw bytes in body) | insert  |
| GET    | `/api/{name}` | ✅    | Download a zip                   | grab    |
| DELETE | `/api/{name}` | ✅    | Delete an item                   | delete  |

### Auth

Every request (except `/ping`) requires the header:

```
Authorization: Bearer <token>
```

### Response shapes

**`GET /api/peek`** — returns a map of name → FileRecord:

```json
{
  "my-project": {
	"name": "my-prject",
    "sizeBytes": 204800,
    "md5": "a1b2c3d4...",
    "createdAt": "2025-05-01T12:00:00",
    "updatedAt": "2025-05-01T14:30:00"
  },
  "config-backup": { ... }
}
```

**`POST /api/{name}`** — body is raw zip bytes (`application/octet-stream`), returns the FileRecord for the inserted item.

**`GET /api/{name}`** — returns raw zip bytes (`application/octet-stream`).

**`DELETE /api/{name}`** — returns 200 on success.

### Constraints

- Max upload size is configured via env var (default ~100MB)
- Storage is a flat directory of `.zip` files + a `cabinet-meta.json` sidecar

---

## CLI (already built)

`cabinet.py` driven by `~/.cabinet/config.json` (`serverUrl`, `token`). Commands: `insert <name>`, `fetch <name>`, `peek`, `delete <name>`. Installed via `install.sh` as a shell function.


# FRONTEND
- Now i want to build a web page GUI which allows for the same functionality
- Built in TypeScript with React and TailWind
- I want you to write small peices of code for me as we go through building this

### General plan
1. make api fetcher
2. make page that shows your files 
3. add ways to grab and insert
4. add login