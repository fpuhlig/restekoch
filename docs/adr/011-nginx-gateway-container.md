# 011: nginx Gateway Container

## Status

Accepted

## Context

The frontend container originally served both static React files and proxied `/api/*` to the backend. This coupled two responsibilities into one container and exposed the backend port 8080 externally for direct access during development.

Three security and architecture concerns emerged:

1. No rate limiting on `/api/scan`, which calls Gemini Vision on every request. A malicious or buggy client could drain the Gemini budget.
2. Administrative endpoints (`POST /api/index`, `DELETE /api/cache`) were reachable from the public internet. Anyone could trigger full reindex or wipe the cache.
3. Port 8080 was exposed externally alongside port 80, giving two entry points to the backend instead of one.

## Decision

Introduce a separate `gateway` container running nginx as reverse proxy. This is the only container that exposes a port (80) to the host. Frontend, backend, and monitoring services are reachable only through the gateway (or through the firewall-open Grafana port 3000, which is addressed in a separate decision).

The gateway container:

- Routes `/` to frontend, `/api/*` to backend
- Applies rate limiting on `/api/scan`: 10 requests per minute per IP with burst of 5 (1 rate slot + 5 burst = 6 requests before 429)
- Restricts admin endpoints to Docker internal networks: `/api/index` entirely internal, `/api/cache` internal for DELETE/POST, public for GET
- Adds security headers: X-Content-Type-Options, X-Frame-Options, Referrer-Policy
- Returns custom JSON error pages for 429 and 502/503/504
- Exposes `/health` endpoint that does not depend on upstream containers
- Uses Docker DNS resolver (127.0.0.11 valid=30s) with direct `proxy_pass` instead of upstream blocks, so container restarts with new IPs are handled within 30 seconds

The frontend container is simplified to serve only static files with SPA routing.

Backend adds Quarkus proxy properties (`quarkus.http.proxy.proxy-address-forwarding`, `allow-x-forwarded`, `enable-forwarded-host`, `enable-forwarded-prefix`) to trust X-Forwarded-* headers from the gateway and log the real client IP instead of the gateway IP.

Terraform firewall closes port 8080 externally. Port 3000 stays open for Grafana access (Grafana subpath integration is out of scope for this change).

## Consequences

Pros:

- Single external entry point makes security policy enforceable in one place
- Rate limiting protects Gemini budget from abuse
- Admin endpoints cannot be called from outside the Docker network, even if the firewall is misconfigured
- Clean separation of concerns: frontend serves files, gateway routes traffic, backend handles logic
- Security headers applied consistently across all responses via `add_header_inherit on`
- Easier to add HTTPS later: only the gateway needs a certificate

Cons:

- One additional container to build, push, and deploy
- `$request_uri` must be appended to `proxy_pass` when using hostnames, otherwise nginx forwards only `/` to the backend
- `limit_except` blocks do not inherit `set` directives (nginx trac #1383), so `proxy_pass` cannot use variables inside those blocks
- Loss of keepalive connection pooling because direct `proxy_pass` with resolver cannot use upstream blocks. Acceptable for low-traffic uni project
- IPv6 pitfall: Docker healthchecks must use `127.0.0.1` instead of `localhost` because busybox wget prefers `::1` but nginx only listens on IPv4
