#!/usr/bin/env bash
# Writes cloudflare-ips.caddy: the Caddy directive that trusts Cloudflare's
# edge ranges, so the backend sees each visitor's real IP (for rate limits).
# Cloudflare changes these rarely; run again if they announce new ranges.
set -euo pipefail
cd "$(dirname "$0")"

ranges=$( { curl -fsSL https://www.cloudflare.com/ips-v4; echo; curl -fsSL https://www.cloudflare.com/ips-v6; } \
  | grep -E '^[0-9a-fA-F:.]+/[0-9]+$' | tr '\n' ' ')

if [ -z "$ranges" ]; then
  echo "Could not download Cloudflare's IP ranges; keeping the existing file." >&2
  exit 1
fi

echo "trusted_proxies static $ranges" > cloudflare-ips.caddy
echo "    wrote cloudflare-ips.caddy ($(echo "$ranges" | wc -w) ranges)"
