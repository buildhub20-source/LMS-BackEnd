#!/usr/bin/env bash
# One-time preparation of a fresh Ubuntu 24.04 VM for GradusMind.
# Run as the admin user (with sudo) from this directory:  ./setup-vm.sh
# Safe to run again: every step checks whether it is already done.
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Docker Engine and Compose plugin (official Docker repository)"
if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"
  echo "    Docker installed. Log out and back in once so 'docker' works without sudo."
else
  echo "    already installed: $(docker --version)"
fi

echo "==> 2 GB swap file (a safety net, not working memory)"
if ! sudo swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
  # Prefer RAM; touch swap only under pressure.
  echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-gradusmind-swap.conf >/dev/null
  sudo sysctl -q --system
else
  echo "    already on"
fi

echo "==> Firewall: SSH, HTTP, HTTPS only"
# Azure's network security group should additionally limit 80/443 to
# Cloudflare's ranges, so nobody can bypass Cloudflare (see README).
sudo ufw allow OpenSSH >/dev/null
sudo ufw allow 80/tcp >/dev/null
sudo ufw allow 443/tcp >/dev/null
sudo ufw --force enable >/dev/null
sudo ufw status | sed 's/^/    /'

echo "==> Cloudflare IP ranges for Caddy (real visitor IPs)"
./refresh-cloudflare-ips.sh

echo "==> Local files the services need"
mkdir -p certs
for f in env/backend env/certificate env/chat env/notifications; do
  [ -f "$f.env" ] || { cp "$f.env.example" "$f.env"; chmod 600 "$f.env"; echo "    created $f.env (fill it in)"; }
done
[ -f .env ] || { cp .env.example .env; echo "    created .env"; }

echo
echo "Done. Next (see README.md):"
echo "  1. Put the Cloudflare Origin certificate in certs/origin.pem and certs/origin.key"
echo "  2. Fill in env/*.env"
echo "  3. Log in to the image registry, then ./deploy.sh"
