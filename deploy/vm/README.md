# GradusMind on one VM (testing phase)

Everything runs as containers on one 4 GB Azure VM (Central India). Cloudflare
sits in front for DNS and HTTPS. Redis, the AI service and a Judge0 server are
not part of this setup.

```
visitor ─► Cloudflare ─► VM :443 ─► Caddy ─┬─ <institute>.gradusmind.com /api/* ─► backend
                                           ├─ <institute>.gradusmind.com (pages) ─► frontend
                                           ├─ chat.gradusmind.com ──────────────► chat
                                           └─ notify.gradusmind.com ────────────► notifications
                                 backend ─► certificate (internal only)
```

| Container | Memory limit | Notes |
|---|---|---|
| caddy | 128 MB | the only container with public ports |
| frontend | 64 MB | built pages (nginx) |
| backend | 1152 MB | Java heap capped at 768 MB |
| certificate | 640 MB | Java heap capped at 384 MB |
| chat | 256 MB | |
| notifications | 256 MB | |

About 2.5 GB in total, leaving room for Ubuntu and Docker; a 2 GB swap file
is a safety net.

## 1. Create the VM (Azure portal)

Ubuntu Server 24.04 LTS, size B2s (2 vCPU, 4 GB), region Central India,
SSH key authentication (username `gradusmind`), 32 GB Standard SSD,
static public IP. Inbound: SSH only at first.

## 2. Prepare it

```bash
ssh -i gradusmind.pem gradusmind@<vm-ip>
git clone https://github.com/buildhub20-source/LMS-BackEnd.git
cd LMS-BackEnd/deploy/vm
./setup-vm.sh          # Docker, swap, firewall, Cloudflare ranges, empty env files
exit                   # log in again so docker works without sudo
```

## 3. DNS and certificate (Cloudflare)

1. Add `gradusmind.com` to Cloudflare and switch the registrar's nameservers.
2. DNS records, all **proxied** (orange cloud), pointing at the VM's IP:
   `gradusmind.com`, `*`, `chat`, `notify`.
3. SSL/TLS mode: **Full (strict)**.
4. SSL/TLS → Origin Server → **Create certificate** for `gradusmind.com` and
   `*.gradusmind.com`. Save it on the VM as `certs/origin.pem` (certificate)
   and `certs/origin.key` (private key, `chmod 600`).
5. Azure → VM → Networking: allow 80 and 443 **only from Cloudflare's
   ranges** (https://www.cloudflare.com/ips/), so nobody can bypass it.

## 4. Settings

Fill in `env/backend.env`, `env/certificate.env`, `env/chat.env` and
`env/notifications.env`. Secrets marked SAME must match across files;
generate them on the VM:

```bash
openssl rand -base64 48   # JWT_SECRET, SERVICE_KEY_SECRET
openssl rand -base64 32   # TENANT_CREDENTIAL_ENCRYPTION_KEY
```

No comments after a value in these files: Docker reads them as part of it.

## 5. First deploy

The images are in GitHub's registry. Create a GitHub token with only
`read:packages`, then on the VM:

```bash
echo <token> | docker login ghcr.io -u <github-user> --password-stdin
./deploy.sh
docker compose logs -f backend      # watch it start
```

First administrators: set `PLATFORM_ADMIN_ENABLED=true` (and email/password)
in `env/backend.env`, run `./deploy.sh backend`, sign in, then set it back to
`false` and deploy again.

## Everyday use

| Task | Command (in this folder) |
|---|---|
| Update everything | `./deploy.sh` |
| Update one service | `./deploy.sh backend` |
| Roll back one service | set `BACKEND_TAG=<commit sha>` in `.env`, `./deploy.sh backend` |
| Logs | `docker compose logs -f --tail 200 backend` |
| Memory per container | `docker stats --no-stream` |
| Restart one | `docker compose restart chat` |

Each repository's deploy workflow runs `./deploy.sh <service>` over SSH after
publishing a new image, using the repository secrets `DEPLOY_HOST`,
`DEPLOY_USER`, `DEPLOY_SSH_KEY` and `DEPLOY_KNOWN_HOSTS`.
