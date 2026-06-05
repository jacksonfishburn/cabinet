import sys
import os
import json
import zipfile
import io
import requests
from pathlib import Path
from config import *

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

        print(f"'{username}' registered")
    else:
        print(f"Error registering '{username}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

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

        print(f"'{username}' logged in")
    else:
        print(f"Error logging in as '{username}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

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
        print(f"Error logging out: {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)


# cabinet operations

def list_cabinets():
    """GET /api/list - Fetch all cabinets for user"""
    url, headers = config()

    response = requests.get(f"{url}/api/list", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinets = data.get("cabinets", [])

        # Update config with cabinet mapping
        cabinets_dict = {c["name"]: c["id"] for c in cabinets}
        save_cabinets(cabinets_dict)

        # Print cabinets
        if cabinets:
            print("Cabinets:")
            for cabinet in cabinets:
                print(f"  {cabinet['name']} (id: {cabinet['id']})")
        else:
            print("No cabinets found")
    else:
        print(f"Error listing cabinets: {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

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
        print(f"Error creating cabinet '{name}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

def peek(cabinet_id=None):
    """GET /api/peek/{cabinet_id} - List files in cabinet"""
    if cabinet_id is None:
        cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet. Use 'cabinet open <name>' or 'cabinet create <name>'")
        sys.exit(1)

    url, headers = config()

    response = requests.get(f"{url}/api/peek/{cabinet_id}", headers=headers)

    if response.status_code == 200:
        files = response.json()
        if files:
            print_file_table(files)
        else:
            print("No files in cabinet")
    else:
        print(f"Error peeking cabinet: {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

def insert(name):
    """POST /api/{cabinet_id}/{name} - Upload file to cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet. Use 'cabinet open <name>' or 'cabinet create <name>'")
        sys.exit(1)

    url, headers = config()
    cwd = get_cwd()
    zip_bytes = zip_and_prepare(cwd)
    headers = {**headers, "Content-Type": "application/octet-stream"}

    response = requests.post(f"{url}/api/{cabinet_id}/{name}", data=zip_bytes, headers=headers)

    if response.status_code == 200:
        data = response.json()
        file_name = data.get("name")
        size = data.get("size")
        print(f"'{file_name}' inserted ({size} bytes)")
    else:
        print(f"Error inserting '{name}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

def grab(name):
    """GET /api/{cabinet_id}/{name} - Download file from cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet. Use 'cabinet open <name>' or 'cabinet create <name>'")
        sys.exit(1)

    url, headers = config()

    response = requests.get(f"{url}/api/{cabinet_id}/{name}", headers=headers)

    if response.status_code == 200:
        zip_bytes = response.content

        folder_path = os.path.join(get_cwd(), name)
        os.makedirs(folder_path, exist_ok=True)

        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zip_file:
            zip_file.extractall(folder_path)

        print(f"'{name}' grabbed")
    else:
        print(f"Error grabbing '{name}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

def delete(name):
    """DELETE /api/{cabinet_id}/{name} - Delete file from cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet. Use 'cabinet open <name>' or 'cabinet create <name>'")
        sys.exit(1)

    url, headers = config()

    response = requests.delete(f"{url}/api/{cabinet_id}/{name}", headers=headers)

    if response.status_code == 204:
        print(f"'{name}' deleted")
    else:
        print(f"Error deleting '{name}': {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

def invite():
    """POST /api/invite/{cabinet_id} - Get invite code for cabinet"""
    cabinet_id = get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet. Use 'cabinet open <name>' or 'cabinet create <name>'")
        sys.exit(1)

    url, headers = config()

    response = requests.post(f"{url}/api/invite/{cabinet_id}", headers=headers)

    if response.status_code == 200:
        data = response.json()
        code = data.get("code")
        print(f"Invite code: {code}")
    else:
        print(f"Error getting invite code: {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)

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
        print(f"Error joining cabinet: {response.status_code}")
        try:
            print(response.json())
        except:
            print(response.text)
