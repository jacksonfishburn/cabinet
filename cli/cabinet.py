#!/usr/bin/env python3
from pathlib import Path
import sys
import os
import zipfile
import io
import requests
import json

CONFIG_PATH = Path.home() / ".cabinet" / "config.json"

# ─────────────────────────────────────────
#           Config management
# ─────────────────────────────────────────
# region Config

def load_config():
    if not CONFIG_PATH.exists():
        return {}
    with open(CONFIG_PATH) as f:
        return json.load(f)

def save_config(config_data):
    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(CONFIG_PATH, "w") as f:
        json.dump(config_data, f, indent=2)

def get_server_url():
    config_data = load_config()
    url = config_data.get("server_url")

    if not url:
        print("No server_url found in config.")
        sys.exit(1)

    return url

def get_token_from_config():
    return load_config().get("token")

def save_token_to_config(token):
    config_data = load_config()
    config_data["token"] = token
    save_config(config_data)

def clear_token_from_config():
    config_data = load_config()
    if "token" in config_data:
        del config_data["token"]
    save_config(config_data)

def get_default_cabinet_id():
    return load_config().get("defaultCabinetId")

def save_default_cabinet_id(cabinet_id):
    config_data = load_config()
    config_data["defaultCabinetId"] = cabinet_id
    save_config(config_data)

def get_active_cabinet_id():
    config_data = load_config()
    active = config_data.get("activeCabinetId")
    if active:
        return active
    return get_default_cabinet_id()

def set_active_cabinet_id(cabinet_id):
    config_data = load_config()
    config_data["activeCabinetId"] = cabinet_id
    save_config(config_data)

def clear_active_cabinet_id():
    config_data = load_config()
    if "activeCabinetId" in config_data:
        del config_data["activeCabinetId"]
    save_config(config_data)

def get_cabinets():
    """Returns dict of cabinet_name -> cabinet_id"""
    return load_config().get("cabinets", {})

def save_cabinets(cabinets_dict):
    """Save cabinet name -> id mapping"""
    config_data = load_config()
    config_data["cabinets"] = cabinets_dict
    save_config(config_data)

def add_cabinet(name, cabinet_id):
    """Add a cabinet to the config"""
    cabinets = get_cabinets()
    cabinets[name] = cabinet_id
    save_cabinets(cabinets)

def get_cabinet_id_by_name(name):
    return get_cabinets().get(name)

def config():
    url = get_server_url()
    token = get_token_from_config()
    headers = {}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    return url, headers

# endregion
# ─────────────────────────────────────────
#               API functions
# ─────────────────────────────────────────
# region API

EXPECTED_ERRORS = {
    400: "Bad request",
    401: "Unauthorized",
    403: "Not logged in",
    404: "Not found",
    409: "Conflict",
    413: "File too large",
}

def handle_response_error(response, action):
    status = response.status_code

    if status in EXPECTED_ERRORS:
        try:
            message = response.json().get("error", EXPECTED_ERRORS[status])
        except:
            message = EXPECTED_ERRORS[status]
        print(f"Error: {message}")
    else:
        try:
            message = response.json().get("error", response.text)
        except:
            message = response.text
        print(f"Unexpected error {status} while trying to {action}: {message}")

# Utility functions
def get_cwd():
    return os.getcwd()

def zip_and_prepare(dir_path):
    zip_buffer = io.BytesIO()
    with zipfile.ZipFile(zip_buffer, 'w', zipfile.ZIP_DEFLATED) as zip_file:
        for root, dirs, files in os.walk(dir_path):
            for file in files:
                full_path = os.path.join(root, file)
                arcname = os.path.relpath(full_path, dir_path)
                zip_file.write(full_path, arcname=arcname)
    return zip_buffer.getvalue()

def print_file_table(records):
    def fmt_size(b):
        if b >= 1_000_000:
            return f"{b / 1_000_000:.1f} MB"
        elif b >= 1_000:
            return f"{b / 1_000:.1f} KB"
        return f"{b} B"

    def fmt_date(iso):
        return iso.replace("T", " ")[:19]

    headers = ["Name", "Size", "MD5", "Created", "Updated"]
    rows = [
        [
            r["name"],
            fmt_size(r["sizeBytes"]),
            r["md5"][:8] + "...",
            fmt_date(r["createdAt"]),
            fmt_date(r["updatedAt"]),
        ]
        for r in records
    ]

    col_widths = [max(len(h), max(len(row[i]) for row in rows)) for i, h in enumerate(headers)]

    def fmt_row(cells):
        return "  ".join(c.ljust(w) for c, w in zip(cells, col_widths))

    divider = "  ".join("-" * w for w in col_widths)

    print(fmt_row(headers))
    print(divider)
    for row in rows:
        print(fmt_row(row))

# auth

def register(username, password):
    url = get_server_url()
    payload = {
        "username": username,
        "password": password,
    }

    response = requests.post(f"{url}/api/auth/register", json=payload)

    if response.status_code == 200:
        data = response.json()
        token = data.get("token")
        default_cabinet_id = data.get("defaultCabinetId")

        if token:
            save_token_to_config(token)
        if default_cabinet_id:
            save_default_cabinet_id(default_cabinet_id)
            add_cabinet(username, default_cabinet_id)

        clear_active_cabinet_id()

        print(f"'{username}' registered")
    else:
        handle_response_error(response, f"register '{username}'")

def login(username, password):
    url = get_server_url()
    payload = {
        "username": username,
        "password": password,
    }

    response = requests.post(f"{url}/api/auth/login", json=payload)

    if response.status_code == 200:
        data = response.json()
        token = data.get("token")
        default_cabinet_id = data.get("defaultCabinetId")

        if token:
            save_token_to_config(token)
        if default_cabinet_id:
            save_default_cabinet_id(default_cabinet_id)
            add_cabinet(username, default_cabinet_id)

        clear_active_cabinet_id()

        print(f"'{username}' logged in")
    else:
        handle_response_error(response, f"login as '{username}'")

def logout():
    url, headers = config()
    token = get_token_from_config()

    if not token:
        print("No token found in config.")
        sys.exit(1)

    response = requests.delete(f"{url}/api/auth/logout", headers=headers)

    if response.status_code in (200, 204):
        clear_token_from_config()
        print("Logged out")
    else:
        handle_response_error(response, "log out")


# cabinet operations

def list_cabinets():
    """GET /api/list - Fetch all cabinets for user"""
    url, headers = config()

    response = requests.get(f"{url}/api/list", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinets = data.get("cabinets", [])
        default_id = get_default_cabinet_id()

        # Update config with cabinet mapping, using "Default" as the display name
        cabinets_dict = {
            ("Default" if c["id"] == default_id else c["name"]): c["id"]
            for c in cabinets
        }
        save_cabinets(cabinets_dict)

        if cabinets:
            for cabinet in cabinets:
                display_name = "Default" if cabinet["id"] == default_id else cabinet["name"]
                print(f"    {display_name}", end="")
        else:
            print("No cabinets found.")
    else:
        handle_response_error(response, "list cabinets")

def create_cabinet(name):
    """POST /api/create/{name} - Create a new cabinet"""
    url, headers = config()

    response = requests.post(f"{url}/api/create/{name}", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinet_id = data.get("id")
        cabinet_name = data.get("name")

        if cabinet_id:
            add_cabinet(cabinet_name, cabinet_id)

        print(f"Cabinet '{cabinet_name}' created")
    else:
        handle_response_error(response, f"create cabinet '{name}'")

def peek(cabinet_id=None):
    """GET /api/peek/{cabinet_id} - List files in cabinet"""
    if cabinet_id is None:
        cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = config()

    response = requests.get(f"{url}/api/peek/{cabinet_id}", headers=headers)

    if response.status_code == 200:
        files = response.json()
        if files:
            print_file_table(files)
        else:
            print("Cabinet is empty")
    else:
        handle_response_error(response, "peek cabinet")

def insert(name):
    """POST /api/{cabinet_id}/{name} - Upload file to cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = config()
    cwd = get_cwd()
    zip_bytes = zip_and_prepare(cwd)
    headers = {**headers, "Content-Type": "application/octet-stream"}

    response = requests.post(f"{url}/api/{cabinet_id}/{name}", data=zip_bytes, headers=headers)

    if response.status_code == 200:
        data = response.json()
        file_name = data.get("name")
        print(f"'{file_name}' inserted")
    else:
        handle_response_error(response, f"insert '{name}'")

def grab(name):
    """GET /api/{cabinet_id}/{name} - Download file from cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = config()

    response = requests.get(f"{url}/api/{cabinet_id}/{name}", headers=headers)

    if response.status_code == 200:
        zip_bytes = response.content

        cwd = get_cwd()

        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zip_file:
            zip_file.extractall(cwd)

        print(f"'{name}' grabbed")
    else:
        handle_response_error(response, f"grab '{name}'")

def delete(name):
    """DELETE /api/{cabinet_id}/{name} - Delete file from cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = config()

    response = requests.delete(f"{url}/api/{cabinet_id}/{name}", headers=headers)

    if response.status_code == 204:
        print(f"'{name}' deleted")
    else:
        handle_response_error(response, f"delete '{name}'")

def invite():
    """POST /api/invite/{cabinet_id} - Get invite code for cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = config()

    response = requests.post(f"{url}/api/invite/{cabinet_id}", headers=headers)

    if response.status_code == 200: 
        code = response.text
        print(f"Invite code: {code}")
    else:
        handle_response_error(response, "get invite code")

def join(code):
    """POST /api/join/{code} - Join a cabinet with invite code"""
    url, headers = config()

    response = requests.post(f"{url}/api/join/{code}", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinet_id = data.get("id")
        cabinet_name = data.get("name")

        if cabinet_id:
            add_cabinet(cabinet_name, cabinet_id)

        print(f"Joined cabinet '{cabinet_name}'")
    else:
        handle_response_error(response, "join cabinet")

# endregion
# ─────────────────────────────────────────
#           Cabinet management
# ─────────────────────────────────────────
# region Main

def open_cabinet(name):
    """Set a cabinet as active"""
    cabinet_id = get_cabinet_id_by_name(name)
    if not cabinet_id:
        print(f"Cabinet '{name}' not found. Try running 'cabinet list' first.")
        sys.exit(1)

    set_active_cabinet_id(cabinet_id)
    print(f"Active cabinet: {name}")

def close_cabinet():
    """Return to default cabinet"""
    clear_active_cabinet_id()
    print("Active cabinet: Default")


# Main CLI handling
def main():
    if len(sys.argv) < 2:
        print("Usage: cabinet <command> [args]")
        print("Authentication:")
        print("  register <username> <password>")
        print("  login <username> <password>")
        print("  logout")
        print("Cabinet management:")
        print("  list                      - List all cabinets")
        print("  create <name>             - Create new cabinet")
        print("  open <name>               - Set active cabinet")
        print("  close                     - Return to default cabinet")
        print("Cabinet operations:")
        print("  peek [name]               - List files (in active cabinet or specified cabinets)")
        print("  invite                    - Get invite code for active cabinet")
        print("  join <code>               - Join cabinet with invite code")
        print("  insert <name>             - Upload current dir as file")
        print("  grab <name>               - Download and extract file")
        print("  delete <name>             - Delete file from active cabinet")
        sys.exit(1)

    command = sys.argv[1]

    match command:
        case "register":
            if len(sys.argv) < 4:
                print("Usage: cabinet register <username> <password>")
                sys.exit(1)
            username = sys.argv[2]
            password = sys.argv[3]
            register(username, password)

        case "login":
            if len(sys.argv) < 4:
                print("Usage: cabinet login <username> <password>")
                sys.exit(1)
            username = sys.argv[2]
            password = sys.argv[3]
            login(username, password)

        case "logout":
            logout()

        case "list":
            list_cabinets()

        case "create":
            if len(sys.argv) < 3:
                print("Usage: cabinet create <name>")
                sys.exit(1)
            name = sys.argv[2]
            create_cabinet(name)

        case "open":
            if len(sys.argv) < 3:
                print("Usage: cabinet open <name>")
                sys.exit(1)
            name = sys.argv[2]
            open_cabinet(name)

        case "close":
            close_cabinet()

        case "peek":
            if len(sys.argv) >= 3:
                name = sys.argv[2]
                cabinet_id = get_cabinet_id_by_name(name)
                if not cabinet_id:
                    print(f"Cabinet '{name}' not found. Try running 'cabinet list' first.")
                    sys.exit(1)
                peek(cabinet_id)
            else:
                peek()

        case "invite":
            invite()

        case "join":
            if len(sys.argv) < 3:
                print("Usage: cabinet join <code>")
                sys.exit(1)
            code = sys.argv[2]
            join(code)

        case "insert":
            if len(sys.argv) < 3:
                print("Usage: cabinet insert <name>")
                sys.exit(1)
            name = sys.argv[2]
            insert(name)

        case "grab":
            if len(sys.argv) < 3:
                print("Usage: cabinet grab <name>")
                sys.exit(1)
            name = sys.argv[2]
            grab(name)

        case "delete":
            if len(sys.argv) < 3:
                print("Usage: cabinet delete <name>")
                sys.exit(1)
            name = sys.argv[2]
            delete(name)

        case _:
            print(f"Unknown command: {command}")
            sys.exit(1)

if __name__ == "__main__":
    main()

# endregion
