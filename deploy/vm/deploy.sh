#!/usr/bin/env bash
# Pulls the latest images and (re)starts the containers that changed.
#   ./deploy.sh                 every service
#   ./deploy.sh backend chat    only these
# Run on the VM. The GitHub deploy workflows call it over SSH.
set -euo pipefail
cd "$(dirname "$0")"

for f in .env env/backend.env env/certificate.env env/chat.env env/notifications.env \
         certs/origin.pem certs/origin.key cloudflare-ips.caddy; do
  [ -f "$f" ] || { echo "Missing $f; run ./setup-vm.sh and fill it in first (README.md)." >&2; exit 1; }
done

docker compose pull "$@"
docker compose up -d --remove-orphans "$@"

# Old image layers otherwise fill the 32 GB disk within a few weeks.
docker image prune -f >/dev/null

docker compose ps
