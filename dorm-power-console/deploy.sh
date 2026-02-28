#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
BRANCH="${1:-master}"
NO_BUILD="${NO_BUILD:-0}"

cd "$APP_DIR"

echo "[deploy] app dir: $APP_DIR"
echo "[deploy] branch: $BRANCH"

if [[ ! -f docker-compose.yml ]]; then
  echo "[deploy] docker-compose.yml not found in $APP_DIR"
  exit 1
fi

if [[ -d .git ]]; then
  echo "[deploy] fetching latest code..."
  git fetch --all --prune
  git checkout "$BRANCH"
  git pull --ff-only origin "$BRANCH"
else
  echo "[deploy] warning: .git not found, skip git pull"
fi

echo "[deploy] building/starting containers..."
if [[ "$NO_BUILD" == "1" ]]; then
  docker compose up -d
else
  docker compose up -d --build
fi

echo "[deploy] current containers:"
docker compose ps

echo "[deploy] latest logs:"
docker compose logs --tail=80

echo "[deploy] done"
