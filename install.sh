#!/bin/bash

set -e

CABINET_DIR="$HOME/.cabinet"
RAW_URL="https://raw.githubusercontent.com/jacksonfishburn/cabinet/main/cli/cabinet.py"
BIN_PATH="/usr/local/bin/cabinet"

echo "Installing Cabinet CLI..."

# check for python
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is required but not installed."
    exit 1
fi

# curl check
if ! command -v curl &> /dev/null; then
    echo "Error: curl is required but not installed."
    exit 1
fi

# create ~/.cabinet if it doesn't exist
mkdir -p "$CABINET_DIR"

# download cabinet.py
echo "Downloading cabinet.py..."
curl -sSL "$RAW_URL" -o "$CABINET_DIR/cabinet.py"
chmod +x "$CABINET_DIR/cabinet.py"

# get server url
if [ -n "$1" ]; then
    SERVER_URL="$1"
else
    read -p "Enter your Cabinet server URL (e.g. http://yourserver.com): " SERVER_URL
fi

if [ -z "$SERVER_URL" ]; then
    echo "Error: server URL cannot be empty."
    exit 1
fi

# write config.json
cat > "$CABINET_DIR/config.json" <<EOF
{
  "serverUrl": "$SERVER_URL",
  "token": ""
}
EOF

# symlink to /usr/local/bin
echo "Adding cabinet to PATH (may require your password)..."

if [ "$EUID" -ne 0 ]; then
    sudo ln -sf "$CABINET_DIR/cabinet.py" "$BIN_PATH"
else
    ln -sf "$CABINET_DIR/cabinet.py" "$BIN_PATH"
fi

echo ""
echo "Cabinet installed successfully."
echo "Run: cabinet login <username> <password>"
