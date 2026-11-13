#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
suite=${1:-python}
case "$suite" in
    python|java|all) ;;
    *) echo "Usage: $0 [python|java|all]" >&2; exit 2 ;;
esac

mkdir -p artifacts
appium --address 127.0.0.1 --port 4723 --log-no-colors > artifacts/appium.log 2>&1 &
appium_pid=$!
trap 'kill "$appium_pid" 2>/dev/null || true' EXIT

for ((attempt = 1; attempt <= 30; attempt++)); do
    if curl -fsS http://127.0.0.1:4723/status 2>/dev/null | grep -q '"ready":true'; then
        status=0
        if [[ "$suite" != java ]]; then
            python -m pytest -q --junitxml=artifacts/junit.xml || status=$?
        fi
        if [[ "$suite" != python ]]; then
            mvn -B -ntp -f java-tests/pom.xml test || status=$?
        fi
        exit "$status"
    fi
    if ! kill -0 "$appium_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done

cat artifacts/appium.log >&2
exit 1
