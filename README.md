# Cabinet
---
Cabinet is self-hosted, file-sync tool for personal or team use - build as both a personal utility and a learning project. You can use your personal cabinet to upload and retrieve directories, or create shared cabinets which you can invite other users to so you can access the same shared folders. 

**Live demo:** [cabinet.jacksonfishburn.dev](https://cabinet.jacksonfishburn.dev/)

> Note: The demo covers auth and file upload/download. Shared cabinet features are CLI-only.

### What I learned
I wanted to take what I had learned in my classes, where I built PlanMyDay and the chess server, and build on it by using industry standard tools. 
- My chess project required that I build the Server and manage class dependencies from scratch, which helped me understand the work that Spring was doing for me behind the scenes.
- Using Docker Compose to containerize the different services (Frontend, Backend, Postgres, Redis) taught me the value of containerization when it comes to sharing work and deployment.
- Implementing stateless auth with JWT helped me think about decisions in a performance focused way.
- Doing things on my own with no outline or handholding taught me a lot about designing software and configuration. I had to:
	- Decide on my own dependencies and manage them myself
	- Manage environment variables and secrets
	- Understand and set up running it locally for dev and running it in deployment
	- Design my DB schema and repositories
	- Decide on endpoint shapes and separation of concerns for client and server

### How it Works
The backend is a Spring Boot REST API running in Docker alongside a PostgreSQL database. Files are stored on the server's filesystem, with metadata tracked in the database. A lightweight Python CLI handles zipping, uploading, and downloading from the command line.

Database schema and HTTP API: see [ARCHITECTURE.md](ARCHITECTURE.md).

---
### Self-hosting

**Prerequisites:** Docker and Docker Compose must be installed on your server.

**1. Clone the repo**

```bash
git clone https://github.com/jacksonfishburn/cabinet.git
cd cabinet
```

**2. Configure environment**

```bash
cp .env.example .env
# edit values in .env (see table below)
```

**3. Start the server**

```bash
docker compose up -d
```

**4. Verify it's running**

```bash
curl http://your-server:8080/api/ping
# should respond 'true'
```

---
### Environment Variables
| Variable                     | Description                                                   | Default / Example   |
| ---------------------------- | ------------------------------------------------------------- | ------------------- |
| `CABINET_STORAGE_DIR`        | Directory inside the container where uploaded zips are stored | `/cabinet-storage`  |
| `CABINET_MAX_SIZE_MB`        | Maximum upload size in MB                                     | `1000`              |
| `CABINET_PORT`               | Port the API listens on                                       | `8080`              |
| `CABINET_CODE_EXPIRATION_MS` | How long a share invite code is valid, in milliseconds        | `86400000` (24 hrs) |
| `POSTGRES_USER`              | PostgreSQL username                                           | `postgres_user`     |
| `POSTGRES_PASSWORD`          | PostgreSQL password                                           | —                   |
| `JWT_SECRET`                 | Secret key used to sign JWTs — should be long and random      | —                   |
| `JWT_EXPIRATION_MS`          | How long a JWT stays valid, in milliseconds                   | `86400000` (24 hrs) |
| `REDIS_HOST`                 | Redis hostname (use `redis` when running via Docker Compose)  | `redis`             |
| `REDIS_PORT`                 | Redis port                                                    | `6379`              |
| `REDIS_PASSWORD`             | Redis password                                                | —                   |
- Recommend you use `openssl rand -base64 32` command for the JWT secret

---
## Installing the CLI

```bash 
curl -sSL https://raw.githubusercontent.com/jacksonfishburn/cabinet/main/install.sh | bash -s -- http://your-server:8080 
``` 

Replace `http://yourserver:8080` with your Cabinet server's address. The script will:
- Download `cabinet.py` to `~/.cabinet/cabinet.py`
- Write your server URL to `~/.cabinet/config.json`
- Symlink `cabinet` to `/usr/local/bin` so it works as a normal command

Once it's done, register: 

```bash
cabinet register <username> <password>
```

---
### CLI usage
| Command                | Arguments               | Description                                          |
| ---------------------- | ----------------------- | ---------------------------------------------------- |
| **Authentication**     |                         |                                                      |
| `register`             | `<username> <password>` | Create a new account                                 |
| `login`                | `<username> <password>` | Log in and save token to config                      |
| `logout`               | —                       | Revoke current token                                 |
| **Cabinet Management** |                         |                                                      |
| `list`                 | —                       | List all your cabinets                               |
| `create`               | `<name>`                | Create a new cabinet                                 |
| `open`                 | `<name>`                | Set the active cabinet                               |
| `close`                | —                       | Return to the default cabinet                        |
| **Cabinet Operations** |                         |                                                      |
| `peek`                 | `[name]`                | List files in the active cabinet, or a specified one |
| `invite`               | —                       | Get an invite code for the active cabinet            |
| `join`                 | `<code>`                | Join a cabinet using an invite code                  |
| `insert`               | `<name>`                | Zip the current directory and upload it              |
| `grab`                 | `<name>`                | Download and extract a file to the current directory |
| `delete`               | `<name>`                | Delete a file from the active cabinet                |


