# Git Workflow for AI Assistant (App 7Eleven Project)

This document guides the AI (or any developer) on how to interact with Git for the **7Eleven MVP project**. The goal is to maintain a clean, professional, and traceable history.

## 1. Repository Information

| Item | Value |
|------|-------|
| **Remote URL** | `https://github.com/NKhanh0908/7-eleven-test.git` |
| **Default Branch** | `main` |
| **Integration Branch** | `develop` |
| **Project** | 7-Eleven Vietnam — Retail Management System |

## 2. Branching Strategy (Simplified Git Flow)

### 2.1. Core Branches

- **`main`**: Production-ready. Only code that is tested and ready for deployment is merged here. **No direct commits allowed.**
- **`develop`**: Main integration branch. All features, bug fixes, and chores merge here first. Always reflects the latest state of development.

### 2.2. Supporting Branches (Short-lived)

All work is done on short-lived branches that branch off from `develop`. Follow the naming conventions strictly.

#### Feature Branches (`feature/...`)
New features mapped to implementation phases (see `impl-plan.md`).

| Example | Phase |
|---------|-------|
| `feature/foundation-setup` | Phase 1 |
| `feature/database-migration` | Phase 2 |
| `feature/auth-jwt-cookie` | Phase 3 |
| `feature/category-crud` | Phase 4 |
| `feature/topping-crud` | Phase 5 |
| `feature/product-crud` | Phase 6 |
| `feature/order-create` | Phase 7 |
| `feature/swagger-dockerfile` | Phase 8 |

- **Source Branch:** `develop`
- **Target Branch:** `develop`

#### Bugfix Branches (`bugfix/...`)
Non-critical bugs found during development.

- `bugfix/topping-query-empty` — fix topping không lấy đúng root category
- `bugfix/order-code-collision` — fix order code trùng
- `bugfix/cors-cookie-blocked` — fix CORS khi gửi httpOnly cookie
- **Source Branch:** `develop` → **Target Branch:** `develop`

#### Chore Branches (`chore/...`)
Refactoring, dependency updates, config changes — không thay đổi business logic.

- `chore/update-bucket4j-version`
- `chore/refactor-exception-handler`
- `chore/optimize-dockerfile`
- **Source Branch:** `develop` → **Target Branch:** `develop`

#### Hotfix Branches (`hotfix/...`)
Urgent production fixes only.

- `hotfix/fix-jwt-expiry-crash`
- `hotfix/fix-stock-race-condition`
- **Source Branch:** `main` → **Target Branch:** Both `main` AND `develop`

## 3. The Definitive Workflow (Git Commands)

### 3.1. Starting a New Task

Always start by syncing your local `develop` branch.

```bash
# 1. Switch to develop
git checkout develop

# 2. Pull latest changes
git pull origin develop

# 3. Create a new branch (follow naming convention above)
git checkout -b feature/auth-jwt-cookie
```

### 3.2. While Working

Commit after each logical unit — correspond to impl-plan phases.

```bash
# Check staged files BEFORE committing (pre-commit checklist)
git status

# Stage files
git add .

# Commit with Conventional Commits format (see Section 5.2)
git commit -m "feat(auth): implement JWT httpOnly cookie login"

# Repeat per logical unit
```

### 3.3. Finishing a Task (Merging to develop)

```bash
# 1. Switch to develop
git checkout develop

# 2. Sync develop (someone else may have pushed)
git pull origin develop

# 3. Merge feature branch
git merge feature/auth-jwt-cookie

# 4. Push develop
git push origin develop

# 5. Delete local branch (clean up)
git branch -d feature/auth-jwt-cookie
```

### 3.4. Releasing to Production (Merging to main)

After a milestone is stable on `develop`:

```bash
# 1. Switch to main
git checkout main

# 2. Sync main
git pull origin main

# 3. Merge develop
git merge develop

# 4. Tag the release
git tag -a v1.0.0 -m "MVP release: product management + order flow"

# 5. Push main + tags
git push origin main
git push origin --tags
```

### 3.5. Hotfix Flow (Production Emergency)

```bash
# 1. Branch from main
git checkout main
git pull origin main
git checkout -b hotfix/fix-jwt-expiry-crash

# 2. Fix the issue
git add .
git commit -m "fix(auth): handle JWT expiry edge case on cookie read"

# 3. Merge into main
git checkout main
git merge hotfix/fix-jwt-expiry-crash
git push origin main

# 4. Merge back into develop (IMPORTANT — don't lose the fix)
git checkout develop
git merge hotfix/fix-jwt-expiry-crash
git push origin develop

# 5. Delete hotfix branch
git branch -d hotfix/fix-jwt-expiry-crash
```

## 4. Rules of the Road

| Rule | Detail |
|------|--------|
| 🚫 No direct commits to `main` | Always merge from `develop` or `hotfix/*` |
| 🚫 No force push | Never `git push --force` on `main` or `develop` |
| ✅ Keep branches short-lived | Delete after merging |
| ✅ Small, logical commits | 1 commit = 1 logical unit, not 1 giant dump |
| ✅ Pull before push | Always `git pull` target branch first |
| ✅ Conventional Commits format | See Section 5.2 |
| ✅ No secrets in commits | Check `.env` files before staging |

## 5. Branch & Commit Conventions

### 5.1. Branch Prefixes

| Type | Prefix | Example |
|------|--------|---------|
| Feature | `feature/` | `feature/product-search-pagination` |
| Bugfix | `bugfix/` | `bugfix/stock-update-optimistic-lock` |
| Chore | `chore/` | `chore/add-flyway-migration-v6` |
| Hotfix | `hotfix/` | `hotfix/fix-order-code-sequence` |

### 5.2. Commit Messages (Conventional Commits)

**Format:** `<type>(<scope>): <subject>`

#### Allowed Types

| Type | When to Use |
|------|-------------|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `chore` | Build, deps, config — no logic change |
| `refactor` | Code restructure — no feature/fix |
| `test` | Add or update tests |
| `security` | Security-related fix (JWT, CORS, rate limit) |
| `perf` | Performance improvement (query, index) |

#### Allowed Scopes — mapped to project domains

| Scope | What it covers |
|-------|----------------|
| `auth` | JWT cookie, login, logout, register, JwtAuthFilter |
| `category` | Category entity, tree query, CRUD |
| `topping` | Topping entity, CategoryTopping, CRUD |
| `product` | Product entity, search, soft delete, stock |
| `order` | Order, OrderItem, OrderItemTopping, status flow |
| `user` | User entity, ownership checks |
| `security` | SecurityConfig, CORS, RateLimitFilter, BCrypt |
| `db` | Flyway migrations (V1–V9), DB sequence |
| `common` | ApiResponse, PageResponse, GlobalExceptionHandler |
| `infra` | Docker Compose, Nginx, Dockerfile |
| `deps` | pom.xml dependency changes |
| `fe` | Frontend (React, Vite, Zustand, TanStack Query) |
| `fe-auth` | Frontend auth store, axios cookie interceptor |
| `fe-product` | Frontend product pages, components |
| `fe-order` | Frontend cart, order flow |
| `config` | application.yml, environment variables |

### 5.3. Good Commit Examples

```bash
# Phase 1 — Foundation
feat(common): add ApiResponse and PageResponse wrappers
feat(common): add ResourceNotFoundException and BusinessException
feat(security): add SecurityConfig skeleton with permit-all
chore(deps): add spring-boot-starter-security and bucket4j-core

# Phase 2 — Database
chore(db): add V1__create_users.sql migration
chore(db): add V2-V5 category, product, topping migrations
chore(db): add V6__create_orders.sql with order_daily_seq sequence
chore(db): add V9__seed_data.sql with admin and user accounts

# Phase 3 — Auth
feat(auth): add User entity and UserRepository
feat(auth): implement JwtTokenProvider with HS256 algorithm
feat(auth): add JwtAuthFilter reading httpOnly cookie + header fallback
feat(auth): implement AuthService login setting httpOnly cookie
feat(auth): implement logout endpoint clearing cookie (maxAge=0)
security(security): add RateLimitFilter 10 req/min on auth endpoints
security(security): configure CORS with specific origins and allowCredentials

# Phase 4–5 — Category & Topping
feat(category): add Category entity with self-referencing parent_id
feat(category): implement CategoryService returning tree structure
feat(topping): add Topping and CategoryTopping entities
feat(topping): implement topping query via root category parent_id

# Phase 6 — Product
feat(product): add Product entity with @Version for optimistic lock
feat(product): implement ProductRepository with JPQL search query
feat(product): implement soft delete setting isActive=false and deletedAt
feat(product): add AdminProductController CRUD with @PreAuthorize ADMIN
perf(product): add GIN trigram index on products.name for ILIKE search

# Phase 7 — Order
feat(order): add Order, OrderItem, OrderItemTopping entities
feat(order): implement order code generation using DB sequence order_daily_seq
feat(order): add stock decrease with @Version optimistic lock
feat(order): add topping validation against root category
feat(order): implement order status state machine (PENDING→CONFIRMED→READY→COMPLETED)
fix(order): handle OptimisticLockException returning HTTP 409

# Phase 8 — Polish
chore(infra): add multi-stage Dockerfile for backend and frontend
chore(infra): add docker-compose.yml with postgres, backend, frontend, nginx
chore(infra): add nginx.conf with /api proxy and security headers
feat(common): complete GlobalExceptionHandler covering all exception types

# Frontend
feat(fe-auth): add authStore with user info only, no token storage
feat(fe-auth): configure axios withCredentials:true for httpOnly cookie
feat(fe): add ErrorBoundary wrapping App in main.tsx
feat(fe-product): add ProductTable with loading/error/empty states
feat(fe-order): implement cart store with Zustand persist

# Tests
test(product): add ProductServiceTest covering CRUD and soft delete
test(order): add OrderServiceTest covering stock insufficient and invalid topping
test(auth): add AuthServiceTest verifying password not stored plain text
```

## 6. Initial Repository Setup

If setting up from scratch:

```bash
# 1. Initialize repo
git init

# 2. Add remote (new URL)
git remote add origin https://github.com/NKhanh0908/7-eleven-test.git

# 3. Create branches
git checkout -b main
git checkout -b develop

# 4. Initial commit on develop
git add .
git commit -m "chore: initial project structure with Spring Boot 3.2 + React 18 + Vite"

# 5. Push both branches
git push -u origin develop
git push origin main
```

## 7. What NOT to Commit (.gitignore)

```gitignore
# Environment / Secrets — NEVER commit
.env
.env.dev
.env.prod
.env.local
*.env

# Java / Maven build outputs
target/
*.jar
*.war
*.ear
*.class

# Frontend build outputs
node_modules/
dist/
.next/
build/

# IDE files
.idea/
.vscode/
*.iml
*.suo
.DS_Store

# Logs
*.log
logs/

# Docker volumes / DB data
postgres_data/
backend/uploads/

# Test reports
surefire-reports/
```

> ⚠️ `JWT_SECRET`, `DB_PASSWORD`, `DB_URL` — phải lấy từ environment variable, **không bao giờ hardcode trong code hoặc commit vào git**.

## 8. AI Automation Rules

When AI performs Git operations, it **must** follow these rules:

1. **Never commit sensitive data** — scan staged files for `.env`, secrets, API keys, JWT secrets, DB passwords before every commit.
2. **Follow Conventional Commits strictly** — use types and scopes from Section 5.2.
3. **Commit in small logical chunks** — 1 commit per impl-plan item, not 1 giant commit per phase.
4. **Always pull before push** — run `git pull origin <branch>` before any push.
5. **Stop on merge conflict** — never attempt to resolve conflicts automatically; ask the developer.
6. **Never force push** — `git push --force` is forbidden on `main` and `develop`.
7. **Match branch to phase** — branch name must reflect the impl-plan phase being worked on.
8. **Verify cookie-related commits** — auth commits must not store JWT in localStorage or response body.

## 9. Example AI Commands

```
"AI, commit the current changes for the auth cookie implementation."
```
AI executes:
```bash
git add src/main/java/com/seveneleven/security/
git add src/main/java/com/seveneleven/domain/user/
git commit -m "feat(auth): implement JwtAuthFilter reading httpOnly cookie with Bearer fallback"
git push origin feature/auth-jwt-cookie
```

---

```
"AI, commit the order service with optimistic lock."
```
AI executes:
```bash
git add src/main/java/com/seveneleven/domain/order/
git commit -m "feat(order): add stock decrease with @Version optimistic lock and 409 handler"
git push origin feature/order-create
```

## 10. Pre-Commit Checklist

Before every commit, verify:

- [ ] `git status` — know exactly what is staged
- [ ] No `.env` or secret files accidentally staged?
- [ ] No JWT secret, DB password, or API key hardcoded in code?
- [ ] JWT stored in httpOnly cookie — not localStorage or response body?
- [ ] Commit message follows `type(scope): subject` format?
- [ ] Scope matches a domain from the allowed scopes list (Section 5.2)?
- [ ] `git pull origin <branch>` done before push?
- [ ] Code compiles? (`./mvnw compile` for backend, `npm run build` for frontend)
- [ ] Relevant checklist in `impl-plan.md` ticked for this domain?