---
trigger: on_task_start
applies_to: backend/**
---

# Backend Implementation Plan

Đây là thứ tự và checklist để implement backend từ zero.
Đọc file này trước khi bắt đầu viết bất kỳ dòng code nào.

---

## Nguyên tắc khi implement

1. **Minimal change first** — chỉ viết code task yêu cầu, không thêm feature "phòng khi cần"
2. **Layer by layer** — không skip, không mix layer
3. **Test as you go** — mỗi endpoint xong phải test ngay với curl/Swagger
4. **Schema trước, code sau** — Flyway migration phải chạy được trước khi viết Java

---

## Thứ tự implement (quan trọng — không đảo)

```
Phase 1: Foundation
  ├── 1.1  pom.xml — dependencies (thêm bucket4j-core)
  ├── 1.2  application.yml — config
  ├── 1.3  Common classes (ApiResponse, PageResponse, exceptions)
  └── 1.4  SecurityConfig skeleton (permit all tạm thời) + CorsConfig

Phase 2: Database
  ├── 2.1  V1__create_users.sql
  ├── 2.2  V2__create_categories.sql
  ├── 2.3  V3__create_products.sql
  ├── 2.4  V4__create_toppings.sql
  ├── 2.5  V5__create_category_toppings.sql
  ├── 2.6  V6__create_orders.sql          ← Bao gồm CREATE SEQUENCE order_daily_seq
  ├── 2.7  V7__create_order_items.sql
  ├── 2.8  V8__create_order_item_toppings.sql
  └── 2.9  V9__seed_data.sql

Phase 3: Auth
  ├── 3.1  User entity + UserRepository
  ├── 3.2  JwtTokenProvider
  ├── 3.3  JwtAuthFilter                  ← Đọc từ httpOnly cookie + fallback header
  ├── 3.4  UserDetailsServiceImpl
  ├── 3.5  SecurityConfig (đầy đủ) + RateLimitFilter
  ├── 3.6  AuthService (login → set cookie, logout → xóa cookie, register)
  └── 3.7  AuthController — test POST /api/v1/auth/login (verify cookie được set)

Phase 4: Category
  ├── 4.1  Category entity (self-referencing, level 0/1)
  ├── 4.2  CategoryRepository
  ├── 4.3  CategoryService
  └── 4.4  CategoryController — test GET /api/v1/categories (tree)

Phase 5: Topping
  ├── 5.1  Topping entity + CategoryTopping entity
  ├── 5.2  ToppingRepository
  ├── 5.3  ToppingService
  └── 5.4  ToppingController
         ⚠️  Đọc kỹ mục "Category–Topping Logic" bên dưới trước khi implement

Phase 6: Product
  ├── 6.1  Product entity (@Version cho stock)
  ├── 6.2  ProductRepository (search query)
  ├── 6.3  ProductService (CRUD + soft delete)
  ├── 6.4  AdminProductController — test CRUD
  └── 6.5  ProductController (user view) — test search + pagination

Phase 7: Order
  ├── 7.1  Order + OrderItem + OrderItemTopping entities
  ├── 7.2  OrderRepository
  ├── 7.3  OrderService (create order + optimistic lock + sequence order code)
  ├── 7.4  OrderController (user)
  └── 7.5  AdminOrderController — test list + status update

Phase 8: Polish
  ├── 8.1  GlobalExceptionHandler — cover tất cả exception types
  ├── 8.2  Swagger/OpenAPI annotations + SecurityScheme cho cookie auth
  └── 8.3  Dockerfile multi-stage
```

---

## ⚠️ Category–Topping Logic (đọc kỹ — dễ implement sai)

### Cấu trúc category 2 cấp

```
Level 0 (Root Category):  "Trà Sữa & Trà Trái Cây"   ← Topping gắn ở đây
  └── Level 1 (Sub-category): "Trà sữa 7-Eleven"      ← Product gắn ở đây
  └── Level 1 (Sub-category): "Trà trái cây"           ← Product gắn ở đây
```

**Quy tắc**: Topping được định nghĩa tại **root category (level=0)**.
Tất cả sản phẩm thuộc các sub-category con đều **kế thừa** topping từ parent.

### Ví dụ cụ thể

```
"Trân châu đen"  →  gắn với  →  "Trà Sữa & Trà Trái Cây" (root, level=0)
                                      ↓ kế thừa
"Trà Tắc sz L"   →  thuộc   →  "Trà trái cây" (sub, level=1)
                                      ↓ parent_id trỏ về root
Vậy "Trà Tắc sz L" có thể thêm "Trân châu đen" ✅
```

### Query lấy topping cho 1 sản phẩm

```java
// Cách đúng: đi từ sub-category của sản phẩm → lên parent (root) → lấy toppings
@Query("""
    SELECT t FROM Topping t
    JOIN CategoryTopping ct ON ct.topping.id = t.id
    JOIN Category sub ON sub.id = :subCategoryId
    WHERE ct.category.id = sub.parent.id
      AND t.isActive = true
    """)
List<Topping> findAvailableBySubCategory(@Param("subCategoryId") UUID subCategoryId);
```

### Validate topping khi tạo order

```java
// ✅ Kiểm tra topping phải thuộc root category của sản phẩm
private void validateToppings(Product product, List<UUID> toppingIds) {
    UUID rootCategoryId = product.getCategory().getParent().getId();
    List<UUID> validToppingIds = categoryToppingRepository
            .findToppingIdsByRootCategoryId(rootCategoryId);

    List<UUID> invalidIds = toppingIds.stream()
            .filter(id -> !validToppingIds.contains(id))
            .toList();

    if (!invalidIds.isEmpty()) {
        throw new BusinessException("INVALID_TOPPING",
                "Topping không thuộc danh mục này: " + invalidIds);
    }
}
```

> 🔴 **Lỗi thường gặp**: Query topping theo `sub-category` thay vì `root category`.
> Kết quả sẽ luôn rỗng vì `category_toppings` chỉ lưu `root category id`.
> Luôn dùng `product.getCategory().getParent().getId()` để lấy root.

---

## Dependencies cần có trong pom.xml

```xml
<!-- Core -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-validation</dependency>

<!-- Database -->
<dependency>postgresql (runtime)</dependency>
<dependency>flyway-core</dependency>
<dependency>flyway-database-postgresql</dependency>

<!-- JWT -->
<dependency>jjwt-api (0.12.x)</dependency>
<dependency>jjwt-impl (runtime)</dependency>
<dependency>jjwt-jackson (runtime)</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.x</version>
</dependency>

<!-- Dev tools -->
<dependency>lombok</dependency>
<dependency>spring-boot-devtools (optional)</dependency>

<!-- API Docs -->
<dependency>springdoc-openapi-starter-webmvc-ui (2.x)</dependency>

<!-- Test -->
<dependency>spring-boot-starter-test (test)</dependency>
```

---

## application.yml cấu trúc

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/seveneleven}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway lo schema, JPA chỉ validate
    show-sql: false             # true khi debug, false khi commit
    properties:
      hibernate.format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}         # bắt buộc từ env, min 64 chars
  expiration-ms: 604800000      # 7 ngày
  cookie-name: access_token     # tên cookie nhất quán

server:
  port: 8080

# Cookie config
cookie:
  secure: ${COOKIE_SECURE:false}    # false ở dev (HTTP), true ở prod (HTTPS)
  same-site: Strict
```

---

## Checklist mỗi domain (lặp lại cho mỗi Phase 4–7)

Trước khi đánh dấu xong 1 domain:

- [ ] Entity có đủ fields theo DESIGN.md không?
- [ ] Entity có `created_at`, `updated_at` (dùng `@CreationTimestamp`, `@UpdateTimestamp`)?
- [ ] Repository có method đúng tên, đúng JPQL?
- [ ] Service dùng constructor injection (không `@Autowired` field)?
- [ ] Service method `@Transactional` đúng chỗ?
- [ ] Service không trả Entity ra ngoài (luôn map sang DTO)?
- [ ] Controller trả `ResponseEntity<ApiResponse<T>>`?
- [ ] Controller có `@Valid` trên `@RequestBody`?
- [ ] Admin endpoints có `@PreAuthorize("hasRole('ADMIN')")`?
- [ ] User endpoints kiểm tra ownership (user chỉ xem data của mình)?
- [ ] Đã test thủ công endpoint vừa viết chưa?

### Checklist bổ sung cho Phase 3 (Auth)

- [ ] JWT được set vào httpOnly cookie (không trả trong response body)?
- [ ] Cookie có `Secure=true` ở prod, `HttpOnly=true`, `SameSite=Strict`?
- [ ] Logout xóa cookie (maxAge=0) không chỉ trả 200?
- [ ] JwtAuthFilter đọc từ cookie trước, fallback sang Authorization header?
- [ ] RateLimitFilter đã apply cho `/api/v1/auth/**`?
- [ ] CORS đã config `allowedOrigins` cụ thể + `allowCredentials(true)`?

### Checklist bổ sung cho Phase 7 (Order)

- [ ] Order code dùng DB sequence không dùng `RandomStringUtils`?
- [ ] Validate toppingIds thuộc đúng root category của sản phẩm?
- [ ] Snapshot `product_name`, `unit_price`, `topping_name`, `extra_price` vào order items?
- [ ] `OptimisticLockException` đã được handle trong `GlobalExceptionHandler`?

---

## Các pattern hay dùng

### Entity base class (dùng chung)
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

### Soft delete trên Product
```java
// Entity
private Boolean isActive = true;
private Instant deletedAt;

public void softDelete() {
    this.isActive = false;
    this.deletedAt = Instant.now();
}

// Repository — chỉ lấy active records
Optional<Product> findByIdAndIsActiveTrue(UUID id);
```

### Order code generation — dùng DB sequence
```java
// V6__create_orders.sql
// CREATE SEQUENCE IF NOT EXISTS order_daily_seq START 1 INCREMENT 1;

// OrderService.java
@PersistenceContext
private EntityManager entityManager;

private String generateOrderCode() {
    Long seq = (Long) entityManager
            .createNativeQuery("SELECT nextval('order_daily_seq')")
            .getSingleResult();
    String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    return String.format("7E-%s-%04d", date, seq % 10000);
}
```

### Lấy toppings theo product (đúng cách)
```java
// ToppingRepository — query từ sub-category lên root
@Query("""
    SELECT t FROM Topping t
    JOIN CategoryTopping ct ON ct.topping.id = t.id
    JOIN Category sub ON sub.id = :subCategoryId
    WHERE ct.category.id = sub.parent.id
      AND t.isActive = true
    """)
List<Topping> findAvailableBySubCategory(@Param("subCategoryId") UUID subCategoryId);
```

---

## Những lỗi hay gặp — tránh từ đầu

| Lỗi | Cách tránh |
|-----|-----------| 
| `LazyInitializationException` | Dùng `@Transactional(readOnly=true)` trên service, hoặc fetch join trong query |
| N+1 query | Dùng `JOIN FETCH` trong JPQL khi biết sẽ cần related data |
| `OptimisticLockException` không bắt được | Thêm handler vào `GlobalExceptionHandler` |
| Circular dependency | Không inject service vào nhau — tách logic ra class thứ 3 |
| Entity thay đổi nhưng không save | Trong `@Transactional`, JPA auto-flush khi commit — nhưng nếu không trong transaction thì phải gọi `save()` |
| Swagger không hiển thị endpoint bảo mật | Thêm `SecurityScheme` vào `OpenApiConfig` |
| Order code trùng lặp | Dùng DB sequence thay vì `RandomStringUtils.randomNumeric()` |
| Topping query rỗng | Query phải đi qua `parent.id` (root category), không phải `sub.id` |
| Token bị XSS đánh cắp | JWT trong httpOnly cookie, không localStorage |
| CORS lỗi khi gửi cookie | `allowCredentials(true)` + origin cụ thể, không dùng `"*"` |
