#!/usr/bin/env bash
set -euo pipefail

# sync-git-dev.sh
# One-command workflow without rsync:
# 1) local git add/commit/push
# 2) remote git pull
# 3) ensure remote dev container is running (hot-reload mode)
#
# Usage:
#   ./sync-git-dev.sh --host <ip-or-domain> --user <ssh-user> --remote-dir <remote-project-dir>
#
# Example:
#   ./sync-git-dev.sh --host 1.2.3.4 --user ubuntu --remote-dir ~/Embedding_competition/web/front/dorm-power-console --branch main --message "feat: update"
#
# Optional:
#   --branch main
#   --port 22
#   --identity ~/.ssh/id_rsa
#   --message "your commit message"
#   --skip-commit   # skip local commit/push, only remote pull + up

HOST=""
USER_NAME=""
PORT="22"
IDENTITY=""
REMOTE_DIR=""
BRANCH="main"
MESSAGE="chore: sync from local"
SKIP_COMMIT="0"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      HOST="$2"; shift 2 ;;
    --user)
      USER_NAME="$2"; shift 2 ;;
    --port)
      PORT="$2"; shift 2 ;;
    --identity)
      IDENTITY="$2"; shift 2 ;;
    --remote-dir)
      REMOTE_DIR="$2"; shift 2 ;;
    --branch)
      BRANCH="$2"; shift 2 ;;
    --message)
      MESSAGE="$2"; shift 2 ;;
    --skip-commit)
      SKIP_COMMIT="1"; shift ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *)
      echo "Unknown argument: $1"
      exit 1 ;;
  esac
done

if [[ -z "$HOST" || -z "$USER_NAME" || -z "$REMOTE_DIR" ]]; then
  echo "Missing required args."
  echo "Run: ./sync-git-dev.sh --help"
  exit 1
fi

if ! command -v git >/dev/null 2>&1; then
  echo "git not found locally."
  exit 1
fi
if ! command -v ssh >/dev/null 2>&1; then
  echo "ssh not found locally."
  exit 1
fi

LOCAL_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$LOCAL_DIR"

SSH_ARGS=(-p "$PORT" -o StrictHostKeyChecking=accept-new)
if [[ -n "$IDENTITY" ]]; then
  SSH_ARGS+=(-i "$IDENTITY")
fi
REMOTE="$USER_NAME@$HOST"

echo "[sync-git] local:  $LOCAL_DIR"
echo "[sync-git] remote: $REMOTE:$REMOTE_DIR"
echo "[sync-git] branch: $BRANCH"

if [[ "$SKIP_COMMIT" != "1" ]]; then
  echo "[sync-git] git add/commit/push..."
  git add -A
  if ! git diff --cached --quiet; then
    git commit -m "$MESSAGE"
  else
    echo "[sync-git] no staged changes, skip commit"
  fi
  git push origin "$BRANCH"
else
  echo "[sync-git] skip local commit/push"
fi

echo "[sync-git] remote pull + start dev container..."
ssh "${SSH_ARGS[@]}" "$REMOTE" "set -e; cd '$REMOTE_DIR'; git fetch --all --prune; git checkout '$BRANCH'; git pull --ff-only origin '$BRANCH'; docker compose -f docker-compose.dev.yml up -d"

echo "[ok] done."
echo "[tip] Open: http://$HOST:3000/dashboard"
