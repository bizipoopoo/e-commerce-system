#!/usr/bin/env bash
set -Eeuo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly TEST_ROOT="$(mktemp -d)"
readonly TEST_APP="$TEST_ROOT/app"
readonly TEST_BIN="$TEST_ROOT/bin"
readonly RELEASE_A="sha-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly RELEASE_B="sha-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
readonly DIGEST_A="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly DIGEST_B="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
readonly BACKEND_A="ghcr.io/bizipoopoo/aurora-commerce-backend@$DIGEST_A"
readonly STOREFRONT_A="ghcr.io/bizipoopoo/aurora-commerce-storefront@$DIGEST_A"
readonly ADMIN_A="ghcr.io/bizipoopoo/aurora-commerce-admin@$DIGEST_A"
readonly BACKEND_B="ghcr.io/bizipoopoo/aurora-commerce-backend@$DIGEST_B"
readonly STOREFRONT_B="ghcr.io/bizipoopoo/aurora-commerce-storefront@$DIGEST_B"
readonly ADMIN_B="ghcr.io/bizipoopoo/aurora-commerce-admin@$DIGEST_B"

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT

mkdir -p "$TEST_APP" "$TEST_BIN"
cp "$PROJECT_ROOT/deployment/deploy.sh" "$TEST_APP/deploy.sh"
cp "$PROJECT_ROOT/deployment/compose.release.yml" "$TEST_APP/compose.release.yml"
printf '%s\n' "MYSQL_PASSWORD=test" > "$TEST_APP/.env"

cat > "$TEST_BIN/docker" <<'EOF'
#!/usr/bin/env bash
set -eu

case " $* " in
  *" exec -T storefront "*)
    if [[ "${RELEASE_VERSION:-}" == "${FAKE_UNHEALTHY_RELEASE:-}" ]]; then
      exit 1
    fi
    printf '%s\n' '{"status":"UP"}'
    ;;
  *" logs "*)
    printf '%s\n' 'fake container logs'
    ;;
  *)
    exit 0
    ;;
esac
EOF

cat > "$TEST_BIN/sleep" <<'EOF'
#!/usr/bin/env sh
exit 0
EOF

cat > "$TEST_BIN/flock" <<'EOF'
#!/usr/bin/env sh
exit 0
EOF

chmod 700 "$TEST_APP/deploy.sh" "$TEST_BIN/docker" "$TEST_BIN/sleep" "$TEST_BIN/flock"
export PATH="$TEST_BIN:$PATH"

"$TEST_APP/deploy.sh" "$RELEASE_A" staging "$BACKEND_A" "$STOREFRONT_A" "$ADMIN_A" > /dev/null
[[ "$(cat "$TEST_APP/.release")" == "$RELEASE_A" ]]
grep -q "$BACKEND_A" "$TEST_APP/.release-state"
grep -q $'\tSUCCESS$' "$TEST_APP/deployment-history.tsv"

export FAKE_UNHEALTHY_RELEASE="$RELEASE_B"
if "$TEST_APP/deploy.sh" "$RELEASE_B" staging "$BACKEND_B" "$STOREFRONT_B" "$ADMIN_B" > /dev/null 2>&1; then
  echo "A failed deployment must leave the workflow in a failed state." >&2
  exit 1
fi

[[ "$(cat "$TEST_APP/.release")" == "$RELEASE_A" ]]
grep -q "$BACKEND_A" "$TEST_APP/.release-state"
grep -q $'\tFAILED$' "$TEST_APP/deployment-history.tsv"
grep -q $'\tROLLBACK$' "$TEST_APP/deployment-history.tsv"

if "$TEST_APP/deploy.sh" latest staging "$BACKEND_A" "$STOREFRONT_A" "$ADMIN_A" > /dev/null 2>&1; then
  echo "Mutable or malformed release tags must be rejected." >&2
  exit 1
fi

if "$TEST_APP/deploy.sh" "$RELEASE_A" qa "$BACKEND_A" "$STOREFRONT_A" "$ADMIN_A" > /dev/null 2>&1; then
  echo "Unknown deployment environments must be rejected." >&2
  exit 1
fi

if "$TEST_APP/deploy.sh" "$RELEASE_A" staging latest "$STOREFRONT_A" "$ADMIN_A" > /dev/null 2>&1; then
  echo "Mutable image references must be rejected." >&2
  exit 1
fi

echo "Deployment success, rollback and tag validation tests passed."
