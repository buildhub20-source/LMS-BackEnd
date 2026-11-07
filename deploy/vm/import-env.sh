#!/usr/bin/env bash
# Fills env/backend.env, env/certificate.env, env/chat.env and env/notifications.env
# from a developer's local LMS-BackEnd/.env, so nothing has to be copied by hand.
#
#   ./import-env.sh ~/local-backend.env
#
# What it does:
#   - starts each file from its *.env.example (server addresses, production values)
#   - copies only the cloud settings the server needs: database, tenant encryption
#     key, Supabase management token, R2 files
#   - generates JWT_SECRET and SERVICE_KEY_SECRET (kept if already generated, so
#     running it again does not sign everyone out)
#   - leaves mail off (MAIL_ENABLED=false) unless --with-mail is given
#   - deletes the uploaded file afterwards
# Values are never printed; the summary lists key names only.
set -euo pipefail
cd "$(dirname "$0")"

with_mail=false
source_file=""
for arg in "$@"; do
  case "$arg" in
    --with-mail) with_mail=true ;;
    *) source_file="$arg" ;;
  esac
done
if [ -z "$source_file" ] || [ ! -f "$source_file" ]; then
  echo "Usage: ./import-env.sh <uploaded local .env> [--with-mail]" >&2
  exit 1
fi

# Reads KEY from the uploaded file: first '=' splits, Windows line endings dropped.
from_source() {
  awk -v key="$1" '
    { sub(/\r$/, "") }
    index($0, key "=") == 1 { print substr($0, length(key) + 2); found = 1; exit }
    END { if (!found) exit 1 }' "$source_file" || true
}

# Reads KEY from an existing target file (to keep generated secrets).
from_target() {
  [ -f "$2" ] || return 0
  awk -v key="$1" 'index($0, key "=") == 1 { print substr($0, length(key) + 2); exit }' "$2"
}

# Sets KEY=VALUE in FILE. The value travels through the environment, so no
# character in it (/, &, \, ^, spaces) is interpreted.
set_value() {
  local file="$1" key="$2"
  VALUE="$3" KEY="$key" awk '
    BEGIN { k = ENVIRON["KEY"]; v = ENVIRON["VALUE"] }
    index($0, k "=") == 1 { print k "=" v; done = 1; next }
    { print }
    END { if (!done) print k "=" v }' "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
  chmod 600 "$file"
}

copy_from_source() {
  local file="$1"; shift
  for key in "$@"; do
    local value
    value=$(from_source "$key")
    if [ -n "$value" ]; then set_value "$file" "$key" "$value"; fi
  done
}

# Keep secrets generated on an earlier run; otherwise make new ones.
jwt=$(from_target JWT_SECRET env/backend.env)
svc=$(from_target SERVICE_KEY_SECRET env/backend.env)
[ -n "$jwt" ] || jwt=$(openssl rand -base64 48 | tr -d '\n')
[ -n "$svc" ] || svc=$(openssl rand -base64 48 | tr -d '\n')

for name in backend certificate chat notifications; do
  cp "env/$name.env.example" "env/$name.env"
  chmod 600 "env/$name.env"
  set_value "env/$name.env" JWT_SECRET "$jwt"
  set_value "env/$name.env" SERVICE_KEY_SECRET "$svc"
done

db_keys="DB_URL DB_USERNAME DB_PASSWORD"
r2_keys="R2_ENDPOINT R2_ACCESS_KEY R2_SECRET_KEY R2_BUCKET R2_PUBLIC_URL"
mail_keys="MAIL_FROM SMTP_HOST SMTP_PORT SMTP_USERNAME SMTP_PASSWORD"

# Backend
copy_from_source env/backend.env $db_keys DB_SCHEMA TENANT_CREDENTIAL_ENCRYPTION_KEY \
  SUPABASE_ACCESS_TOKEN SUPABASE_ORGANIZATION_SLUG R2_ACCOUNT_ID $r2_keys
# Institutes are created in the same region as the existing Supabase projects.
region=$(from_source DB_URL | sed -n 's|.*://aws-[0-9]*-\([a-z0-9-]*\)\.pooler\.supabase\.com.*|\1|p')
[ -n "$region" ] && set_value env/backend.env SUPABASE_TENANT_REGION "$region"

# Certificate service: same database, key and files as the backend.
copy_from_source env/certificate.env $db_keys TENANT_CREDENTIAL_ENCRYPTION_KEY $r2_keys

# Chat attachments use the same R2 bucket.
copy_from_source env/chat.env R2_ACCOUNT_ID $r2_keys

if $with_mail; then
  copy_from_source env/backend.env $mail_keys
  copy_from_source env/certificate.env $mail_keys
else
  set_value env/backend.env MAIL_ENABLED false
  set_value env/certificate.env MAIL_ENABLED false
fi

# The uploaded copy has served its purpose; do not leave secrets lying around.
shred -u "$source_file" 2>/dev/null || rm -f "$source_file"

echo "Settings written: env/backend.env env/certificate.env env/chat.env env/notifications.env"
echo "Mail: $($with_mail && echo on || echo "off (invitation and reset links go to the backend log)")"
echo
echo "Still empty (names only; see README for which are fine):"
for f in env/*.env; do
  # grep finds nothing in a complete file; that is success, not an error.
  empty=$( { grep -E '^[A-Z_]+=$' "$f" || true; } | cut -d= -f1 | tr '\n' ' ')
  printf '  %-22s %s\n' "$f" "${empty:-none}"
done
