# Security in System Design Interviews

## What it is / Why it matters in interviews
Security is tested as a cross-cutting concern at senior/staff level — interviewers don't ask a separate "security round," they ask "walk me through how you'd secure this." The ability to weave AuthN/AuthZ, secrets management, and threat mitigation into an HLD without derailing the design is the signal. Candidates who ignore security get dinged; candidates who over-rotate into security minutiae also get dinged.

---

## Core Concepts

### Authentication vs Authorization

**AuthN (Authentication) — Who are you?**
Establishes identity. Verifies that the caller is who they claim to be.
Examples: password check, JWT validation, API key lookup, mTLS certificate.

**AuthZ (Authorization) — What can you do?**
Grants or denies access to a resource after identity is confirmed.
Examples: RBAC role check, IAM policy evaluation, permission scope on a token.

```
Request → [AuthN layer]  → [AuthZ layer] → Resource
            "Is this       "Can user X
            user real?"     access /admin?"
```

---

### Session-based vs Token-based Auth

| | Session-based | Token-based (JWT) |
|---|---|---|
| Storage | Server stores session in DB/cache | No server state — token is self-contained |
| Scalability | Sticky sessions or shared store (Redis) | Stateless — any server can validate |
| Revocation | Instant (delete session from store) | Hard — must wait for expiry or use a blocklist |
| Size | Small session ID in cookie | JWT can be large (claims inflate it) |
| Best for | Monolith, web apps with server rendering | Microservices, mobile, SPAs |

---

### JWT Deep Dive

**Structure:** `header.payload.signature` (Base64URL encoded, dot-separated)

```
Header:    { "alg": "RS256", "typ": "JWT" }
Payload:   { "sub": "user123", "exp": 1700000000, "roles": ["admin"] }
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

**Validation:** Server verifies signature using secret (HMAC) or public key (RSA/ECDSA). If signature is valid, payload is trusted without a DB lookup — this is what makes JWTs stateless.

**Expiry + Refresh Token Pattern:**
```
Client                   Auth Server              Resource Server
  |                           |                          |
  |-- POST /login ----------->|                          |
  |<-- access_token (15min)   |                          |
  |    refresh_token (7d) ----|                          |
  |                           |                          |
  |-- GET /data (access_token)------------------------------>|
  |<-------- 200 OK ----------------------------------------|
  |                           |                          |
  [access_token expires]       |                          |
  |-- POST /refresh (refresh_token)-->|                   |
  |<-- new access_token (15min) ------|                   |
```

**Why short-lived access tokens?** If stolen, damage is time-limited. Refresh token is longer-lived but only sent to auth server over HTTPS, reducing exposure surface.

**Common JWT mistakes:**
- `"alg": "none"` attack — always validate the algorithm, never accept `none`
- Storing JWT in localStorage (vulnerable to XSS) — prefer httpOnly cookies
- Not validating `exp` and `iss` claims

---

### OAuth 2.0 Flows

OAuth 2.0 is an authorization framework — it lets users grant third-party apps access to their resources without sharing passwords.

```
Four main flows:

1. Authorization Code (Web Apps):
   User → App → Auth Server (login + consent) → Auth Server returns code
   App → Auth Server (exchanges code for token, server-side) → Access Token
   WHY: Code is short-lived; token exchange is server-to-server (secret never in browser)

2. Authorization Code + PKCE (Mobile/SPA):
   Same as above, but replaces client_secret with a code_verifier/code_challenge pair
   WHY: Mobile apps can't securely store a client_secret; PKCE proves it's the same client

3. Client Credentials (Service-to-Service):
   ServiceA → Auth Server (client_id + client_secret) → Access Token
   WHY: No user involved; machine-to-machine auth
   Example: Order service calling Inventory service

4. Implicit (DEPRECATED):
   Token returned directly in URL fragment — vulnerable to token leakage
   Replaced by Auth Code + PKCE for SPAs
```

**When to use what:**
- User logs into your web app: Authorization Code
- Your mobile app needs user auth: Authorization Code + PKCE
- Internal microservice calls: Client Credentials (or mTLS)
- Third-party integration (e.g., "Login with Google"): Authorization Code

---

### API Keys

| | API Keys | OAuth 2.0 |
|---|---|---|
| Best for | Server-to-server, developer APIs | User-delegated access |
| Granularity | Scoped at key creation | Scoped via OAuth scopes per token |
| Revocation | Delete the key | Revoke token or wait for expiry |
| Rotation | Manual or automated | Short-lived tokens auto-expire |
| Security risk | Long-lived credential — leaked key = full access | Access token is short-lived |

**Key rotation:** Rotate API keys periodically (90 days, or on suspected compromise). Use Secrets Manager to store and automate rotation. Never embed API keys in code or commit to git.

**Scoping:** Issue keys with minimum required permissions. A key for reading analytics shouldn't have write access to orders.

---

### Secrets Management

**The anti-pattern: hardcoded secrets**
```python
# NEVER DO THIS
DB_PASSWORD = "s3cr3t123"
API_KEY = "sk-abcdefg..."
```
Secrets in code end up in git history, build logs, container images.

**Environment variables** — better, but still risky. Visible in `ps aux`, leaked in crash dumps, no audit trail.

**Secrets Manager (AWS Secrets Manager / HashiCorp Vault):**
```
Service                  Secrets Manager
  |                           |
  |-- "Give me DB creds" ----->|
  |<-- { user: x, pass: y } --|  (fetched at runtime, not in code)
  |                           |
  [Later: auto-rotation]      |
  Secrets Manager rotates DB password, updates RDS, invalidates old creds
```

**How services authenticate to Secrets Manager:**
- **AWS:** EC2/Lambda use IAM roles. The instance has a role; the role has policy `secretsmanager:GetSecretValue`. No long-term credentials needed.
- **Vault:** Vault agent runs as a sidecar, authenticates via Kubernetes service account or AWS IAM, injects secrets into the app as env vars or files.

**Rotation:**
- AWS Secrets Manager can auto-rotate RDS/Redshift/ElastiCache credentials with a Lambda function.
- Applications should handle `GetSecretValue` errors gracefully and retry on rotation events.

---

### HTTPS / TLS

**Why TLS at the load balancer vs mTLS:**

```
Client → [TLS termination at LB] → [HTTP inside VPC] → Services
  Pros: Simple, LB manages certs, services don't need TLS config
  Cons: Traffic inside VPC is unencrypted (acceptable if VPC is trusted)

Client → [TLS at LB] → [mTLS between services] → Services
  Pros: End-to-end encryption, mutual authentication (both sides verify cert)
  Cons: Operational overhead (managing client certs), latency (handshake)
  When: High compliance requirements (PCI-DSS, HIPAA), zero-trust networking
```

**Certificate management:**
- **ACM (AWS Certificate Manager)** — free TLS certs for AWS services (ALB, CloudFront, API Gateway). Auto-renewal.
- **Let's Encrypt** — free certs for custom infrastructure. Use Certbot for auto-renewal. 90-day validity.
- **Certificate pinning** — app hardcodes expected cert or public key. Prevents MitM with a rogue cert. Risk: if cert rotates and app isn't updated, it breaks. Used in mobile banking apps.

---

### Common Attack Vectors (Architecture Level)

**SQL Injection**
```
Attack: SELECT * FROM users WHERE name = '' OR '1'='1'
Defense:
  - Parameterized queries (prepared statements) — never string-concatenate user input into SQL
  - ORM (Hibernate, SQLAlchemy) — uses parameterized queries by default
  - Least privilege: DB user has only SELECT/INSERT, not DROP TABLE
```

**XSS (Cross-Site Scripting)**
```
Attack: User submits <script>document.cookie</script> — gets stored and executed for other users
Defense:
  - Output encoding: escape HTML entities before rendering user input
  - Content Security Policy (CSP): HTTP header that restricts script sources
  - HttpOnly cookies: inaccessible to JavaScript even if XSS occurs
```

**CSRF (Cross-Site Request Forgery)**
```
Attack: Evil site makes victim's browser send forged request to your API using victim's cookies
Defense:
  - SameSite=Strict/Lax cookie attribute: browser won't send cookie on cross-origin requests
  - CSRF tokens: server issues a random token per session; form/AJAX must include it; server validates
  - Double-submit cookie pattern: set random cookie + send same value in header; server compares
```

**DDoS**
```
Attack: Flood of requests exhausts servers/bandwidth
Defense layers:
  - CDN (CloudFront, Cloudflare): absorbs traffic at edge, hides origin IP
  - Rate limiting: at API Gateway, NGINX, or app level (e.g., 100 req/min per IP)
  - WAF (Web Application Firewall): block known attack patterns, IP reputation lists
  - Scrubbing centers: AWS Shield Advanced, Cloudflare Magic Transit — inspect + clean traffic
  - Auto-scaling: absorb sudden but legitimate spikes
```

**SSRF (Server-Side Request Forgery)**
```
Attack: Attacker tricks your server into making requests to internal services
  e.g., user submits URL "http://169.254.169.254/latest/meta-data/" — EC2 metadata endpoint
Defense:
  - Validate and whitelist URLs before fetching
  - Deny outbound requests to private IP ranges (10.x.x.x, 172.16.x.x, 192.168.x.x, 169.254.x.x)
  - Use IMDSv2 (token-required metadata) to mitigate EC2 metadata abuse
  - Network-level egress controls on your Lambda/ECS task's security group
```

---

## Decision Framework

**Session vs JWT — when does it matter?**
```
Monolith / few servers?
  → Session-based (Redis-backed session store) is simpler, instant revocation

Microservices / horizontal scale?
  → JWT: stateless, no shared session store, services validate independently

Need instant revocation (e.g., "log out all sessions after breach")?
  → Session-based OR JWT + token blocklist (adds state, but selective)

Mobile / SPA?
  → JWT (store in memory or httpOnly cookie, not localStorage)
```

**OAuth vs API key?**
```
User grants your app permission to access THEIR data elsewhere (e.g., Google Drive)?
  → OAuth 2.0 Authorization Code

Internal microservice A calls microservice B?
  → Client Credentials OAuth OR mTLS (depends on security posture)

External developer consuming your public API?
  → API key (simple, easy to scope, easy to revoke per key)
```

**TLS termination at LB vs mTLS everywhere?**
```
Standard production system?
  → TLS at LB, HTTP inside VPC (pragmatic, low ops overhead)

PCI-DSS / HIPAA / zero-trust requirement?
  → mTLS between services (end-to-end encryption + mutual auth)
```

---

## Numbers to Know

| Item | Value |
|---|---|
| JWT access token typical lifetime | 15 minutes |
| Refresh token typical lifetime | 7–30 days |
| OAuth authorization code lifetime | 10 minutes max |
| TLS handshake overhead | ~1–2 RTTs (TLS 1.3 = 1-RTT or 0-RTT) |
| bcrypt cost factor for passwords | 12 (balances security vs login latency) |
| OWASP recommended min password entropy | 15+ chars or equivalent entropy |
| Let's Encrypt cert validity | 90 days |
| AWS ACM cert validity | 13 months (auto-renews) |
| Rate limit (typical API) | 100–1,000 req/min per user/API key |

---

## Security in System Design

**Where to add AuthN/AuthZ in a layered architecture:**
```
Internet
    |
    v
[WAF] ← DDoS protection, IP blocking, OWASP rules
    |
    v
[API Gateway / Load Balancer]
    ← TLS termination
    ← Rate limiting
    ← API key validation (for public APIs)
    |
    v
[Auth Service / JWT validation middleware]
    ← Validate JWT signature + expiry
    ← Extract user identity + roles
    |
    v
[Service Layer]
    ← AuthZ check: "Does this user have permission for THIS resource?"
    ← Row-level security: user X can only read their own data
    |
    v
[Data Layer]
    ← Encryption at rest (AES-256, RDS encryption, DynamoDB encryption)
    ← DB user has minimum permissions (no DROP, no schema changes)
```

**Encryption at rest vs in transit:**
| | In Transit | At Rest |
|---|---|---|
| What | Data moving over network | Data stored on disk/DB |
| How | TLS/HTTPS, mTLS | AES-256, KMS, transparent disk encryption |
| Required for PCI-DSS? | Yes | Yes |
| Required for HIPAA? | Yes | Yes |
| Default in AWS? | HTTPS endpoints provided | Opt-in (enable RDS encryption, S3 SSE) |

**Data classification drives encryption decisions:**
- PII (name, email, SSN) → encrypt at rest + in transit
- Financial data → encrypt at rest + in transit, consider field-level encryption
- Internal config data → at least in transit
- Public static assets → TLS is sufficient

---

## Interview Tips

**What "describe the security of your design" means at senior level:**

Interviewers want to see you think in layers (defense-in-depth), not just add "HTTPS" and move on. Hit these layers in 2–3 minutes:

1. **Perimeter:** WAF, DDoS protection, rate limiting at API Gateway
2. **Auth:** JWT/OAuth at the API layer, mTLS for service-to-service if sensitive
3. **AuthZ:** RBAC or ABAC check at the service layer, not just at the gateway
4. **Data:** Encryption in transit (TLS), at rest (KMS + disk encryption), secrets in Secrets Manager
5. **Network:** VPC private subnets for DB/services, security groups, no public IPs on internal services
6. **Audit:** Logging all auth events (who accessed what, when), CloudTrail for AWS operations

**Example answer snippet:**
> "For AuthN, I'd use OAuth 2.0 Authorization Code flow for user logins, issuing short-lived JWTs (15 min) with refresh tokens. Service-to-service calls use Client Credentials. For AuthZ, the order service validates whether the requesting user owns the order before returning data — not just at the API Gateway. Secrets like DB credentials live in AWS Secrets Manager and are fetched at startup via IAM role — no hardcoded creds. The DB sits in a private subnet, TLS at the ALB, and I'd enable CloudTrail and GuardDuty from day one for audit and anomaly detection."

**Common mistakes:**
- Saying "we'll use HTTPS" and stopping there
- Confusing AuthN with AuthZ (e.g., "the JWT handles authorization" — no, it carries claims, but you still check them)
- Not mentioning refresh token rotation
- Storing JWTs in localStorage instead of httpOnly cookies
- Forgetting that OAuth 2.0 itself is not authentication — OpenID Connect (OIDC) adds the identity layer on top
- No secrets rotation strategy
- Treating the VPC as a trust boundary with no internal AuthZ (lateral movement risk)
