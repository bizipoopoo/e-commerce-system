#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly COMPOSE_FILE="$DEPLOY_DIR/compose.release.yml"
readonly ENV_FILE="$DEPLOY_DIR/.env"
readonly RELEASE_FILE="$DEPLOY_DIR/.release"
readonly STATE_FILE="$DEPLOY_DIR/.release-state"
readonly HISTORY_FILE="$DEPLOY_DIR/deployment-history.tsv"
readonly TARGET_RELEASE="${1:-}"
readonly TARGET_ENVIRONMENT="${2:-}"
readonly TARGET_BACKEND_IMAGE="${3:-}"
readonly TARGET_STOREFRONT_IMAGE="${4:-}"
readonly TARGET_ADMIN_IMAGE="${5:-}"

is_image_digest() {
  [[ "$1" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[0-9a-f]{64}$ ]]
}

if [[ ! "$TARGET_RELEASE" =~ ^(sha-[0-9a-f]{40}|v[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?)$ ]] \
  || [[ ! "$TARGET_ENVIRONMENT" =~ ^(staging|production)$ ]] \
  || ! is_image_digest "$TARGET_BACKEND_IMAGE" \
  || ! is_image_digest "$TARGET_STOREFRONT_IMAGE" \
  || ! is_image_digest "$TARGET_ADMIN_IMAGE"; then
  echo "Usage: $0 <release> <environment> <backend@digest> <storefront@digest> <admin@digest>" >&2
  exit 2
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE; create it from release.env.example before deploying." >&2
  exit 2
fi

exec 9>"$DEPLOY_DIR/.deploy.lock"
if ! flock -n 9; then
  echo "Another deployment is already running." >&2
  exit 3
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

wait_until_healthy() {
  local attempt
  for attempt in {1..60}; do
    if compose exec -T storefront \
      wget -qO- http://localhost/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 2
  done
  return 1
}

previous_release=""
previous_backend_image=""
previous_storefront_image=""
previous_admin_image=""
if [[ -f "$STATE_FILE" ]]; then
  IFS=$'\t' read -r previous_release previous_backend_image \
    previous_storefront_image previous_admin_image < "$STATE_FILE"
  if [[ ! "$previous_release" =~ ^(sha-[0-9a-f]{40}|v[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?)$ ]] \
    || ! is_image_digest "$previous_backend_image" \
    || ! is_image_digest "$previous_storefront_image" \
    || ! is_image_digest "$previous_admin_image"; then
    echo "Deployment state in $STATE_FILE is invalid; refusing an unsafe rollout." >&2
    exit 4
  fi
fi

export RELEASE_VERSION="$TARGET_RELEASE"
export DEPLOY_ENVIRONMENT="$TARGET_ENVIRONMENT"
export BACKEND_IMAGE="$TARGET_BACKEND_IMAGE"
export STOREFRONT_IMAGE="$TARGET_STOREFRONT_IMAGE"
export ADMIN_IMAGE="$TARGET_ADMIN_IMAGE"
echo "Pulling Aurora Commerce $RELEASE_VERSION..."
compose pull

if compose up -d --remove-orphans && wait_until_healthy; then
  printf '%s\t%s\t%s\t%s\n' \
    "$RELEASE_VERSION" "$BACKEND_IMAGE" "$STOREFRONT_IMAGE" "$ADMIN_IMAGE" \
    > "$STATE_FILE.tmp"
  mv "$STATE_FILE.tmp" "$STATE_FILE"
  printf '%s\n' "$RELEASE_VERSION" > "$RELEASE_FILE"
  printf '%s\t%s\t%s\t%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$RELEASE_VERSION" "$previous_release" "SUCCESS" \
    >> "$HISTORY_FILE"
  compose ps
  echo "Deployment $RELEASE_VERSION is healthy."
  exit 0
fi

echo "Deployment $RELEASE_VERSION failed health verification." >&2
compose ps >&2 || true
compose logs --no-color --tail=200 >&2 || true
printf '%s\t%s\t%s\t%s\n' \
  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$RELEASE_VERSION" "$previous_release" "FAILED" \
  >> "$HISTORY_FILE"

if [[ -z "$previous_release" || "$previous_release" == "$RELEASE_VERSION" ]]; then
  echo "No distinct previous release is available for automatic rollback." >&2
  exit 1
fi

echo "Rolling back to $previous_release..." >&2
export RELEASE_VERSION="$previous_release"
export BACKEND_IMAGE="$previous_backend_image"
export STOREFRONT_IMAGE="$previous_storefront_image"
export ADMIN_IMAGE="$previous_admin_image"
compose pull
compose up -d --remove-orphans
if wait_until_healthy; then
  printf '%s\n' "$previous_release" > "$RELEASE_FILE"
  printf '%s\t%s\t%s\t%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$previous_release" "$TARGET_RELEASE" "ROLLBACK" \
    >> "$HISTORY_FILE"
  echo "Rollback to $previous_release succeeded; the deployment workflow remains failed." >&2
else
  echo "Rollback to $previous_release also failed; manual recovery is required." >&2
fi
exit 1
