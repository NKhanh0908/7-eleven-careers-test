# ARCHITECTURE.md — Kiến trúc hệ thống

---

## 1. Mô hình kiến trúc

Hệ thống áp dụng **Layered Architecture (3-Tier)** kết hợp nguyên tắc của **Clean Architecture** ở tầng backend:

```
┌─────────────────────────────────────────────┐
│              Presentation Layer             │
│         React 18 + Vite (Frontend)          │
└───────────────────┬─────────────────────────┘
                    │  REST API / JSON (HTTP)
┌───────────────────▼─────────────────────────┐
│              Application Layer              │
│           Spring Boot 3.2 (Backend)         │
│  Controller → Service → Repository          │
└───────────────────┬─────────────────────────┘
                    │  JPA / Hibernate
┌───────────────────▼─────────────────────────┐
│               Data Layer                    │
│             PostgreSQL 16                   │
└─────────────────────────────────────────────┘
```

**Nguyên tắc chính:**
- Mỗi layer chỉ phụ thuộc vào layer ngay bên dưới, không bao giờ skip.
- Business logic tập trung ở `Service`, không để logic ở `Controller` hay `Repository`.
- `Controller` chỉ nhận request, validate input, gọi `Service`, trả response.
- `Repository` chỉ chứa các truy vấn dữ liệu thuần túy.

---

## 2. Tech Stack & Versions

### Backend

| Thành phần        | Công nghệ & Version              | Ghi chú                              |
|-------------------|----------------------------------|--------------------------------------|
| Ngôn ngữ          | Java 21 (LTS)                    | Virtual threads sẵn sàng             |
| Framework         | Spring Boot 3.2.x                | Auto-configuration, embedded Tomcat  |
| Build tool        | Maven 3.9.x                      |                                      |
| ORM               | Spring Data JPA + Hibernate 6.x  |                                      |
| Database          | PostgreSQL 16                    | Production DB duy nhất               |
| Auth              | Spring Security 6 + JWT          | httpOnly cookie, 7 ngày, không refresh |
| Migration         | Flyway 9.x                       | Versioned SQL migrations             |
| Validation        | Bean Validation (Hibernate)      | `@Valid`, `@NotBlank`, v.v.          |
| Rate Limiting     | Bucket4j (in-memory)             | Bảo vệ auth endpoints                |
| API Docs          | SpringDoc OpenAPI 3 (Swagger UI) |                                      |
| Test              | JUnit 5, Mockito                 |                                      |

### Frontend

| Thành phần      | Công nghệ & Version        | Ghi chú                           |
|-----------------|----------------------------|-----------------------------------|
| Framework       | React 18.3.x               | Concurrent features               |
| Build tool      | Vite 5.x                   | HMR, fast build                   |
| Ngôn ngữ        | TypeScript 5.x             | Strict mode                       |
| Styling         | Tailwind CSS 3.4.x         | Utility-first                     |
| UI Components   | shadcn/ui                  | Headless, accessible              |
| State (client)  | Zustand 4.x                | Giỏ hàng, auth state              |
| State (server)  | TanStack Query v5           | Fetch, cache, invalidate          |
| HTTP Client     | Axios 1.x                  | Interceptor gắn JWT tự động       |
| Form            | React Hook Form + Zod      | Validation phía client            |
| Router          | React Router v6            |                                   |

### Infrastructure

| Thành phần   | Công nghệ & Version  | Ghi chú                        |
|--------------|----------------------|--------------------------------|
| Container    | Docker 24.x          |                                |
| Orchestrator | Docker Compose 2.x   | Single-node, không Kubernetes  |
| Reverse proxy| Nginx 1.25           | Serve FE static, proxy API     |
| Port FE      | 3000                 |                                |
| Port BE      | 8080                 |                                |
| Port DB      | 5432                 | Không expose ra ngoài prod     |

---

## 3. Luồng dữ liệu (Data Flow)

### 3.1 Luồng Request thông thường

```
Browser
  │
  │  HTTP Request (JWT trong httpOnly cookie — không phải header)
  ▼
Nginx :80
  │  /api/*  → proxy_pass backend:8080
  │  /*       → serve frontend/dist/index.html
  ▼
Spring Boot :8080
  │
  ├─ CorsFilter                ← Kiểm tra Origin header
  ├─ RateLimitFilter           ← Chặn brute force (auth endpoints)
  ├─ JwtAuthFilter             ← Đọc cookie, validate token, set SecurityContext
  │
  ▼
Controller (@RestController)
  │  Nhận @RequestBody, @PathVariable, @RequestParam
  │  Gọi @Valid để validate
  ▼
Service (@Service)
  │  Business logic
  │  Transaction management (@Transactional)
  ▼
Repository (JpaRepository / @Query)
  │  JPQL / Native SQL
  ▼
PostgreSQL
  │
  ▼
Service  →  DTO (manual mapping)
  ▼
Controller  →  ResponseEntity<ApiResponse<T>>
  ▼
Browser nhận JSON
```

### 3.2 Luồng xác thực (Auth Flow)

```
POST /api/v1/auth/login
  ▼
AuthController → AuthService
  │  1. Load UserDetails từ DB
  │  2. PasswordEncoder.matches()
  │  3. Tạo JWT (subject=userId, claim: role, email)
  │  4. Set JWT vào httpOnly cookie (Secure, SameSite=Strict)
  │  5. Trả về { user } — KHÔNG trả token trong body
  ▼
Browser lưu cookie tự động (httpOnly — JS không đọc được)
  ▼
Mọi request sau: browser tự gửi cookie
  ▼
JwtAuthFilter đọc cookie → parse token → set Authentication vào SecurityContext
  ▼
@PreAuthorize("hasRole('ADMIN')") hoặc hasRole('USER') kiểm tra
```

> ⚠️ **MVP Trade-off**: Nếu sau này cần mobile app (không dùng cookie), chuyển sang
> Authorization header + refresh token rotation. Với web-only MVP này, httpOnly cookie
> là lựa chọn an toàn hơn localStorage (chống XSS).

### 3.3 Luồng tạo đơn hàng

```
User chọn sản phẩm + topping → Zustand cart state
  ▼
POST /api/v1/orders  (gửi lên CartDTO)
  ▼
OrderService
  │  1. Validate sản phẩm còn active, còn hàng
  │  2. Optimistic lock (@Version) khi trừ stock_quantity
  │  3. Lưu Order (status=PENDING) + OrderItems + OrderItemToppings
  │  4. Generate order_code từ DB sequence (không random — tránh collision)
  │  5. Trả về OrderResponse với order_code
  ▼
Frontend hiển thị xác nhận đơn hàng
```

---

## 4. Cấu trúc thư mục source code

### 4.1 Backend (`/backend`)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/seveneleven/
│   │   │   ├── SevenElevenApplication.java       # Entry point
│   │   │   │
│   │   │   ├── config/                           # Cấu hình Spring
│   │   │   │   ├── SecurityConfig.java           # Security filter chain + CORS
│   │   │   │   ├── JwtConfig.java                # JWT properties
│   │   │   │   ├── RateLimitConfig.java          # Bucket4j rate limiting
│   │   │   │   └── OpenApiConfig.java            # Swagger config
│   │   │   │
│   │   │   ├── common/                           # Shared utilities
│   │   │   │   ├── ApiResponse.java              # Response wrapper { data, message, status }
│   │   │   │   ├── PageResponse.java             # Pagination wrapper
│   │   │   │   ├── GlobalExceptionHandler.java   # @ControllerAdvice
│   │   │   │   └── exception/                    # Custom exceptions
│   │   │   │       ├── ResourceNotFoundException.java
│   │   │   │       └── BusinessException.java
│   │   │   │
│   │   │   ├── security/                         # Auth & JWT
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthFilter.java            # Đọc từ httpOnly cookie
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── user/
│   │   │   │   ├── category/
│   │   │   │   ├── product/
│   │   │   │   ├── topping/
│   │   │   │   └── order/
│   │   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           ├── V1__create_users.sql
│   │           ├── V2__create_categories.sql
│   │           ├── V3__create_products.sql
│   │           ├── V4__create_toppings.sql
│   │           ├── V5__create_category_toppings.sql
│   │           ├── V6__create_orders.sql          # Bao gồm sequence order_code_seq
│   │           ├── V7__create_order_items.sql
│   │           ├── V8__create_order_item_toppings.sql
│   │           └── V9__seed_data.sql
│   │
│   └── test/
│       └── java/com/seveneleven/
│           ├── domain/product/service/ProductServiceTest.java
│           └── domain/order/service/OrderServiceTest.java
│
├── Dockerfile
└── pom.xml
```

### 4.2 Frontend (`/frontend`)

```
frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   │
│   ├── lib/
│   │   ├── axios.ts                     # Axios instance — withCredentials: true (cookie)
│   │   ├── queryClient.ts
│   │   └── utils.ts
│   │
│   ├── store/
│   │   ├── authStore.ts                 # Zustand: user (không lưu token — cookie lo)
│   │   └── cartStore.ts
│   │
│   ├── components/
│   │   ├── error/
│   │   │   ├── ErrorBoundary.tsx        # React Error Boundary — bắt lỗi render
│   │   │   └── GlobalErrorFallback.tsx  # UI hiển thị khi crash
│   │   ├── ui/
│   │   ├── layout/
│   │   ├── product/
│   │   ├── cart/
│   │   └── shared/
│   │       ├── ConfirmDialog.tsx
│   │       ├── Pagination.tsx
│   │       └── SearchInput.tsx
│   │
│   └── pages/
│       ├── auth/
│       ├── admin/
│       └── user/
│
├── Dockerfile
├── nginx.conf
├── vite.config.ts
├── tailwind.config.ts
└── package.json
```

---

## 5. Docker Compose topology

```
docker-compose.yml
│
├── postgres          (image: postgres:16-alpine)
│   └── port 5432, volume: postgres_data
│
├── backend           (build: ./backend/Dockerfile)
│   ├── port 8080
│   └── depends_on: postgres
│
├── frontend          (build: ./frontend/Dockerfile)
│   └── port 3000, built static files
│
└── nginx             (image: nginx:1.25-alpine)
    ├── port 80 → expose ra ngoài
    ├── /api/*  → proxy backend:8080
    └── /*      → serve frontend static
```

**Nginx — cấu hình bảo mật quan trọng:**
```nginx
# Thêm security headers
add_header X-Frame-Options "SAMEORIGIN";
add_header X-Content-Type-Options "nosniff";
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "strict-origin-when-cross-origin";
```

**Multi-stage Dockerfile — backend:**
```
Stage 1 (builder): maven:3.9-eclipse-temurin-21 → ./mvnw package
Stage 2 (runtime): eclipse-temurin:21-jre-alpine → copy JAR, EXPOSE 8080
```

**Multi-stage Dockerfile — frontend:**
```
Stage 1 (builder): node:20-alpine → npm ci && npm run build
Stage 2 (runtime): nginx:1.25-alpine → copy dist/, EXPOSE 80
```

---

## 6. CORS Configuration

```java
// SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // ✅ Chỉ cho phép origin cụ thể — KHÔNG dùng "*" khi có credentials
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",       // dev
        "https://app.7eleven.vn"       // prod
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization"));

    // ✅ Bắt buộc true để browser gửi cookie
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

> ⚠️ **Quan trọng**: `allowCredentials(true)` và `allowedOrigins("*")` không thể dùng cùng nhau.
> Phải liệt kê origin cụ thể.

---

## 7. Rate Limiting (Bucket4j)

Bảo vệ các endpoint nhạy cảm khỏi brute force:

```java
// RateLimitConfig.java — cấu hình giới hạn
// Auth endpoints: tối đa 10 requests/phút per IP
// API chung: tối đa 100 requests/phút per IP

// Áp dụng cho:
// POST /api/v1/auth/login     → 10 req/phút/IP
// POST /api/v1/auth/register  → 5 req/phút/IP
// Các API khác               → 100 req/phút/IP
```

```java
// RateLimitFilter.java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip, boolean isAuthEndpoint) {
        return buckets.computeIfAbsent(ip + ":" + isAuthEndpoint, k -> {
            long capacity = isAuthEndpoint ? 10 : 100;
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(capacity,
                            Refill.greedy(capacity, Duration.ofMinutes(1))))
                    .build();
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        boolean isAuth = request.getRequestURI().startsWith("/api/v1/auth/");
        Bucket bucket = resolveBucket(ip, isAuth);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"status\":429,\"message\":\"Quá nhiều yêu cầu, thử lại sau\"}");
        }
    }
}
```

---

## 8. Quy tắc & Ràng buộc kiến trúc

| Quy tắc | Lý do |
|---------|-------|
| Không dùng `@Autowired` field injection — chỉ constructor injection | Testability, immutability |
| Không trả `Entity` trực tiếp từ `Controller` — luôn qua DTO | Tránh lộ cấu trúc DB, kiểm soát response |
| Mọi response phải qua `ApiResponse<T>` wrapper | Frontend parse nhất quán |
| `@Transactional` chỉ ở tầng `Service` | Tránh transaction scope quá rộng |
| Soft delete với `deleted_at` trên bảng `products` | Giữ lịch sử order items hợp lệ |
| `@Version` trên `product.stock_quantity` | Tránh race condition khi nhiều user đặt cùng lúc |
| JWT lưu trong httpOnly cookie, không localStorage | Chống XSS — JS không đọc được cookie |
| CORS chỉ cho phép origin cụ thể | `allowCredentials=true` không cho phép wildcard |
| Order code dùng DB SEQUENCE, không Random | Tránh trùng lặp khi tải cao |
| Rate limiting trên auth endpoints | Chống brute force password |
| JWT không có refresh token | Đơn giản hóa MVP, access token 7 ngày |
| Chỉ 1 backend instance | Không cần session replication hay distributed lock |
