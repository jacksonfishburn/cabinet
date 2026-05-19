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
- I want all software design decisions to be kept **As Simple As Possible!**

### General plan
1. make api fetcher
2. make page that shows your files 
3. add ways to grab and insert
4. add login

### Design aesthetic for Cabinet
- Cabinet is a personal file-sync tool. The UI should feel retro-warm and utilitarian — like a well-organized physical filing system translated into software. The goal is structured, calm, and a little characterful without being decorative.
- Typography — use IBM Plex Serif or a slab serif for headings and display text, paired with IBM Plex Mono or JetBrains Mono for filenames, metadata, sizes, and dates. The monospace text should do a lot of the heavy lifting visually.
- Color — warm off-whites and aged paper tones for backgrounds (not pure white). One rich accent color — deep teal or muted rust/terracotta. Muted warm grays for secondary text. Nothing bright or saturated.
- Layout — think in horizontal rows, not cards or grids. Items should feel like labeled drawer entries — tight, structured, with clear dividers between them. Heavy-ish horizontal rules. Metadata pushed to the right like a file tag. No floating cards, no shadows.
- Details — uppercase or small-caps for column headers and labels. Flat and structural throughout. No rounded corners everywhere, no gradients, no glassmorphism. Minimal iconography. The feeling of the file cabinet comes from the structure and rhythm of the layout, not from any literal imagery.
- Overall feel — something a developer built for themselves that happens to look considered. Warm but precise. Analog soul, digital execution.