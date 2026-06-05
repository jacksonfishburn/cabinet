import sys
import json
from pathlib import Path

CONFIG_PATH = Path.home() / ".cabinet" / "config.json"

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
    """Get cabinet id from name"""
    return get_cabinets().get(name)

def config():
    url = get_server_url()
    token = get_token_from_config()
    headers = {}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    return url, headers