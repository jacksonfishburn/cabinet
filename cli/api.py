import sys
import os
import zipfile
import io
import requests
import config as c

EXPECTED_ERRORS = {
    400: "Bad request",
    401: "Unauthorized",
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
    url = c.get_server_url()
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
            c.save_token_to_config(token)
        if default_cabinet_id:
            c.save_default_cabinet_id(default_cabinet_id)
            c.add_cabinet(username, default_cabinet_id)

        print(f"'{username}' registered")
    else:
        handle_response_error(response, f"register '{username}'")

def login(username, password):
    url = c.get_server_url()
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
            c.save_token_to_config(token)
        if default_cabinet_id:
            c.save_default_cabinet_id(default_cabinet_id)
            c.add_cabinet(username, default_cabinet_id)

        print(f"'{username}' logged in")
    else:
        handle_response_error(response, f"login as '{username}'")

def logout():
    url, headers = c.config()
    token = c.get_token_from_config()

    if not token:
        print("No token found in config.")
        sys.exit(1)

    response = requests.delete(f"{url}/api/auth/logout", headers=headers)

    if response.status_code in (200, 204):
        c.clear_token_from_config()
        print("Logged out")
    else:
        handle_response_error(response, "log out")


# cabinet operations

def list_cabinets():
    """GET /api/list - Fetch all cabinets for user"""
    url, headers = c.config()

    response = requests.get(f"{url}/api/list", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinets = data.get("cabinets", [])

        # Update config with cabinet mapping
        cabinets_dict = {c["name"]: c["id"] for c in cabinets}
        c.save_cabinets(cabinets_dict)

        # Print cabinets
        if cabinets:
            print("Cabinets:")
            for cabinet in cabinets:
                print(f"  {cabinet['name']} (id: {cabinet['id']})")
        else:
            print("No cabinets found")
    else:
        handle_response_error(response, "list cabinets")

def create_cabinet(name):
    """POST /api/create/{name} - Create a new cabinet"""
    url, headers = c.config()

    response = requests.post(f"{url}/api/create/{name}", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinet_id = data.get("id")
        cabinet_name = data.get("name")

        if cabinet_id:
            c.add_cabinet(cabinet_name, cabinet_id)

        print(f"Cabinet '{cabinet_name}' created")
    else:
        handle_response_error(response, f"create cabinet '{name}'")

def peek(cabinet_id=None):
    """GET /api/peek/{cabinet_id} - List files in cabinet"""
    if cabinet_id is None:
        cabinet_id = c.get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = c.config()

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
    cabinet_id = c.get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = c.config()
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
        handle_response_error(response, f"insert '{name}'")

def grab(name):
    """GET /api/{cabinet_id}/{name} - Download file from cabinet"""
    cabinet_id = c.get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = c.config()

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
    cabinet_id = c.get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = c.config()

    response = requests.delete(f"{url}/api/{cabinet_id}/{name}", headers=headers)

    if response.status_code == 204:
        print(f"'{name}' deleted")
    else:
        handle_response_error(response, f"delete '{name}'")

def invite():
    """POST /api/invite/{cabinet_id} - Get invite code for cabinet"""
    cabinet_id = c.get_active_cabinet_id()

    if not cabinet_id:
        print("No active cabinet.")
        sys.exit(1)

    url, headers = c.config()

    response = requests.post(f"{url}/api/invite/{cabinet_id}", headers=headers)

    if response.status_code == 200: 
        code = response.text
        print(f"Invite code: {code}")
    else:
        handle_response_error(response, "get invite code")

def join(code):
    """POST /api/join/{code} - Join a cabinet with invite code"""
    url, headers = c.config()

    response = requests.post(f"{url}/api/join/{code}", headers=headers)

    if response.status_code == 200:
        data = response.json()
        cabinet_id = data.get("id")
        cabinet_name = data.get("name")

        if cabinet_id:
            c.add_cabinet(cabinet_name, cabinet_id)

        print(f"Joined cabinet '{cabinet_name}'")
    else:
        handle_response_error(response, "join cabinet")