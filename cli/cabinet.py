import sys
import os
import json
import zipfile
import io
import requests
from pathlib import Path

CONFIG_PATH = Path.home() / ".cabinet" / "config.json"
AUTH_BASE_PATH = "/auth"

# Configs
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

def config():
    url = get_server_url()
    token = get_token_from_config()
    headers = {}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    return url, headers


# Helpers
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
def auth_request(method, path, username=None, password=None):
    url = get_server_url()
    payload = {
        "username": username,
        "password": password,
    }

    response = requests.request(method, f"{url}{path}", json=payload)
    return response

def register(username, password):
    response = auth_request("POST", f"{AUTH_BASE_PATH}/register", username, password)

    if response.status_code == 200:
        data = response.json()
        token = data.get("token")

        if token:
            save_token_to_config(token)

        print(f"'{username}' registered")
    else:
        print(f"Error registering '{username}': {response.status_code}")
        print(response.json())

def login(username, password):
    response = auth_request("GET", f"{AUTH_BASE_PATH}/login", username, password)

    if response.status_code == 200:
        data = response.json()
        token = data.get("token")

        if token:
            save_token_to_config(token)

        print(f"'{username}' logged in")
    else:
        print(f"Error logging in as '{username}': {response.status_code}")
        print(response.json())

def logout():
    url, headers = config()
    token = get_token_from_config()

    if not token:
        print("No token found in config.")
        sys.exit(1)

    response = requests.delete(f"{url}{AUTH_BASE_PATH}/logout", headers=headers)

    if response.status_code in (200, 204):
        clear_token_from_config()
        print("Logged out")
    else:
        print(f"Error logging out: {response.status_code}")
        print(response.json())


# cabinet operations
def insert(name):
    url, headers = config()
    cwd = get_cwd()
    zip_bytes = zip_and_prepare(cwd)
    headers = {**headers, "Content-Type": "application/octet-stream"}
    
    response = requests.post(f"{url}/{name}", data=zip_bytes, headers=headers)
    
    if response.status_code == 200:
        print(f"'{name}' inserted")
    else:
        print(f"Error inserting '{name}': {response.status_code}")
        print(response.json())

def grab(name):
    url, headers = config()
    
    response = requests.get(f"{url}/{name}", headers=headers)
    
    if response.status_code == 200:
        zip_bytes = response.content

        folder_path = os.path.join(get_cwd(), name)
        os.makedirs(folder_path, exist_ok=True)
        
        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zip_file:
            zip_file.extractall(folder_path)
        
        print(f"'{name}' grabbed")
    else:
        print(f"Error grabbing '{name}': {response.status_code}")
        print(response.text)

def peek():
    url, headers = config()
    
    response = requests.get(f"{url}/peek", headers=headers)
    
    if response.status_code == 200:
        print_file_table(response.json())
    else:
        print(f"Error peeking: {response.status_code}")
        print(response.text)

def delete(name):
    url, headers = config()
    
    response = requests.delete(f"{url}/{name}", headers=headers)
    
    if response.status_code == 204:
        print(f"'{name}' deleted")
    else:
        print(f"Error deleting '{name}': {response.status_code}")
        print(response.json())


# Main CLI handling
def main():
    if len(sys.argv) < 2:
        print("Usage: cabinet <command> [name]")
        print("Commands:\n insert \n grab \n peek \n delete \n register \n login \n logout \n")
        sys.exit(1)

    command = sys.argv[1]

    match command:
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

        case "peek":
            peek()

        case "delete":
            if len(sys.argv) < 3:
                print("Usage: cabinet delete <name>")
                sys.exit(1)
            name = sys.argv[2]
            delete(name)

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

        case _:
            print(f"Unknown command: {command}")
            print("Commands: insert, grab, peek, delete, register, login, logout")
            sys.exit(1)

if __name__ == "__main__":
    main()