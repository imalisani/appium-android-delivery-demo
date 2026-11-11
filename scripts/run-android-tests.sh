#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p artifacts
appium --address 127.0.0.1 --port 4723 --log-no-colors > artifacts/appium.log 2>&1 &
appium_pid=$!
trap 'kill "$appium_pid" 2>/dev/null || true' EXIT

for ((attempt = 1; attempt <= 30; attempt++)); do
    if curl -fsS http://127.0.0.1:4723/status 2>/dev/null | grep -q '"ready":true'; then
        python -m pytest -q --junitxml=artifacts/junit.xml
        exit
    fi
    if ! kill -0 "$appium_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done

cat artifacts/appium.log >&2
exit 1
