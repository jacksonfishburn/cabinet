import sys
import os
import json
import zipfile
import io
import requests
from pathlib import Path

CONFIG_PATH = Path.home() / ".cabinet" / "config.json"

def load_config():
    if not CONFIG_PATH.exists():
        print("No config found. Run install.sh first.")
        sys.exit(1)
    with open(CONFIG_PATH) as f:
        return json.load(f)

def config():
    config = load_config()
    url = config["server_url"]
    token = config["token"]
    auth_header = {"Authorization": f"Bearer {token}"}

    return url, auth_header

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

def insert(name):
    url, headers = config()
    cwd = get_cwd()
    zip_bytes = zip_and_prepare(cwd)
    headers = {**headers, "Content-Type": "application/octet-stream"}
    
    response = requests.post(f"{url}/{name}", data=zip_bytes, headers=headers)
    
    if response.status_code == 200:
        print(f"Successfully inserted '{name}'")
        print(response.json())
    else:
        print(f"Error inserting '{name}': {response.status_code}")
        print(response.text)

def grab(name):
    url, headers = config()
    
    response = requests.get(f"{url}/{name}", headers=headers)
    
    if response.status_code == 200:
        zip_bytes = response.content

        folder_path = os.path.join(get_cwd(), name)
        os.makedirs(folder_path, exist_ok=True)
        
        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zip_file:
            zip_file.extractall(folder_path)
        
        print(f"Successfully grabbed '{name}'")
    else:
        print(f"Error grabbing '{name}': {response.status_code}")
        print(response.text)

def peek():
    url, headers = config()
    
    response = requests.get(f"{url}/peek", headers=headers)
    
    if response.status_code == 200:
        print("Cabinet contents:")
        print(json.dumps(response.json(), indent=2))
    else:
        print(f"Error peeking: {response.status_code}")
        print(response.text)

def delete(name):
    url, headers = config()
    
    response = requests.delete(f"{url}/{name}", headers=headers)
    
    if response.status_code == 204:
        print(f"Successfully deleted '{name}'")
    else:
        print(f"Error deleting '{name}': {response.status_code}")
        print(response.text)


def main():
    if len(sys.argv) < 2:
        print("Usage: cabinet <command> [name]")
        print("Commands: insert, grab, peek, delete")
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

        case _:
            print(f"Unknown command: {command}")
            print("Commands: insert, grab, peek, delete")
            sys.exit(1)

if __name__ == "__main__":
    main()