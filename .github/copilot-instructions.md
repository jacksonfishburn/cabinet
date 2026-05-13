Mini personal dropbox/git for getting files from my laptop to my pc.

I want to start with just a backend server that holds the files for me and then a cli for uplaoding and fetching the files
its called cabinet, you just go
cabinet insert "name"
and then it will put the directory for where ever you are (if its not too big) into the cabinet.
then when u want to get it out you go cabinet fetch name and it will copy the directory into the directory you are at. you can go cabinet peek and it will show all the names for all the things you can get.

I'll start with a cli putting the program on a a vps and being able to download and use it on any computer, then ill make a front end and auth so different people can make accounts and use it on a website.

# File Structure
```
cabinet/
├── docker-compose.yml
├── .env                        ← CABINET_TOKEN, port, storage path
├── README.md
│
└── backend/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/cabinet/
        │   ├── CabinetApplication.java
        │   ├── config/
        │   │   └── TokenAuthFilter.java    ← swapped for JWT later
        │   ├── controller/
        │   │   └── CabinetController.java  ← add more controllers later
        │   ├── service/
        │   │   └── CabinetService.java
        │   └── storage/
        │       └── MetadataStore.java      ← reads/writes cabinet-meta.json
        └── resources/
            └── application.properties
```


> When you add a frontend later it just sits at `cabinet/frontend/` and calls the backend. Nothing in the backend needs to change — the controller layer is already a clean API boundary.

>When you add auth later, `TokenAuthFilter` gets replaced with JWT middleware, `MetadataStore` gets replaced with a proper repository, and you add a `model/` and `repository/` package. The controller and service layers barely change.


# Build Plan

---

### Phase 1 — Backend

**1. Project skeleton**
- Create the Maven project with Spring Boot 3, just the web dependency for now
- Set up `application.properties` to read everything from env vars (port, token, storage dir)
- Write the `Dockerfile` — build stage with Maven, run stage with just the JRE

**2. Storage layer**
- Decide on a storage dir (e.g. `./cabinet-storage/`) that will be mounted as a Docker volume
- Write `MetadataStore` — a single class that owns reading/writing `cabinet-meta.json`
- The JSON structure is just a map of `name → { sizeBytes, md5, createdAt, updatedAt }`

**3. Service layer**
- `CabinetService` handles all the actual logic: zip streams, MD5 hashing, size limit enforcement, calling `MetadataStore`
- No file paths or JSON parsing should leak into the controller

**4. Controller**
- Four endpoints, all under `/api/`:
    - `GET /api/ping` — health check, no auth
    - `GET /api/peek` — list all items from metadata
    - `POST /api/insert/{name}` — receive zip, store it, update metadata
    - `GET /api/fetch/{name}` — stream zip back
    - `DELETE /api/delete/{name}` — remove zip + metadata entry

**5. Auth filter**
- Simple servlet filter that checks `X-Cabinet-Token` header on all routes except `/ping`
- Reads the expected token from env var

---

### Phase 2 — Docker

**6. Dockerfile**
- Multi-stage: Maven build → slim JRE runtime image
- Exposes port 8765

**7. docker-compose.yml + .env**
- Single service, mounts a host volume into `/cabinet-storage` inside the container
- All config (token, port, max size) comes from `.env` so you never hardcode anything
- `docker compose up -d` and it's running

---

### Phase 3 — CLI

**8. `cabinet.py`**
- On `insert`: zip CWD in memory, POST it with the token header
- On `fetch`: GET the zip, unzip it into CWD
- On `peek`: GET the list, print it nicely
- On `delete`: send DELETE
- Reads server URL + token from `~/.cabinet/config.json`

**9. `install.sh`**
- Copies `cabinet.py` somewhere (e.g. `~/.cabinet/cabinet.py`)
- Writes a `cabinet` function into `~/.bashrc` so you can just type `cabinet insert foo`
- Prompts you for server URL + token and writes `config.json`

---

### Future phases (not building now, but the structure supports it)

**Phase 4 — Auth + accounts**

- Add Postgres as a second Docker service
- Add `User` model, link items to users
- Swap `TokenAuthFilter` for Spring Security + JWT
- Add `/api/auth/register` and `/api/auth/login` endpoints
- `MetadataStore` becomes a proper JPA repository

**Phase 5 — UI**

- Add `cabinet/frontend/` (React or whatever)
- Calls the same `/api/` endpoints
- Add it as a second service in `docker-compose.yml`, or serve static files from Spring Boot