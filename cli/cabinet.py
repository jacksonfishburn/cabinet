from pathlib import Path
import sys
import config as c

# Import api functions after config is available
from api import (
    register,
    login,
    logout,
    list_cabinets,
    create_cabinet,
    peek,
    insert,
    grab,
    delete,
    invite,
    join,
)

CONFIG_PATH = Path.home() / ".cabinet" / "config.json"

def open_cabinet(name):
    """Set a cabinet as active"""
    cabinet_id = c.get_cabinet_id_by_name(name)
    if not cabinet_id:
        print(f"Cabinet '{name}' not found. Try running 'cabinet list' first.")
        sys.exit(1)

    c.set_active_cabinet_id(cabinet_id)
    print(f"Active cabinet: {name}")

def close_cabinet():
    """Return to default cabinet"""
    c.clear_active_cabinet_id()
    default_id = c.get_default_cabinet_id()

    # Find the default cabinet name
    cabinets = c.get_cabinets()
    default_name = next((name for name, cid in cabinets.items() if cid == default_id), "default")

    print(f"Active cabinet: {default_name}")


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
                cabinet_id = c.get_cabinet_id_by_name(name)
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