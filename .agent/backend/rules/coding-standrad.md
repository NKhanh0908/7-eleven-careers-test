---
trigger: always_on
applies_to: backend/**/*.java
---

# Backend Coding Standards

Mọi code Java/Spring Boot trong project này phải tuân thủ các quy tắc dưới đây.
Không có ngoại lệ. Nếu nghi ngờ, hỏi trước — đừng đoán.

---

## 1. Naming Conventions

```java
// ✅ Class: PascalCase, tên danh từ rõ nghĩa
public class ProductService {}
public class OrderItemRepository {}
public class JwtAuthFilter {}

// ✅ Method: camelCase, động từ mô tả hành động
public ProductResponse findById(UUID id) {}
public Page<ProductResponse> searchProducts(ProductSearchRequest request, Pageable pageable) {}
public void softDeleteProduct(UUID id) {}

// ✅ Variable: camelCase, đủ nghĩa
UUID productId = product.getId();
List<OrderItem> orderItems = order.getItems();
boolean isStockSufficient = product.getStockQuantity() >= requestedQuantity;

// ✅ Constant: SCREAMING_SNAKE_CASE
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String ROLE_ADMIN = "ROLE_ADMIN";
public static final long JWT_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

// ❌ Tuyệt đối không dùng
String d = "...";           // tên không có nghĩa
Object data = getUser();    // type quá chung
int flag = 1;               // magic number
```

---

## 2. Layer Rules — Không được vi phạm

### Controller
- Chỉ nhận request, gọi service, trả response
- Không chứa business logic
- Không query database trực tiếp
- Không gọi repository trực tiếp
- Luôn trả về `ResponseEntity<ApiResponse<T>>`

```java
// ✅ Đúng
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductRequest request) {
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Product created", 201));
}

// ❌ Sai — logic trong controller
@PostMapping
public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
    if (request.getPrice() <= 0) throw new RuntimeException("Bad price"); // logic!
    Product p = new Product();                                            // entity trực tiếp
    productRepository.save(p);                                           // gọi repo trực tiếp
    return ResponseEntity.ok(p);
}
```

### Service
- Chứa toàn bộ business logic
- `@Transactional` ở đây, không ở Controller hay Repository
- Không trả về Entity ra ngoài — luôn map sang DTO
- Constructor injection bắt buộc (không `@Autowired` field)

```java
// ✅ Đúng
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest req, Pageable pageable) {
        return productRepository.searchActive(req.getSearch(), req.getCategoryId(), pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public void softDeleteProduct(UUID id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.softDelete();
        // không cần save() nếu đã trong transaction
    }
}

// ❌ Sai
@Service
public class ProductService {
    @Autowired                              // field injection
    private ProductRepository repo;

    public Product getProduct(UUID id) {   // trả Entity ra ngoài
        return repo.findById(id).get();    // không xử lý Optional
    }
}
```

### Repository
- Chỉ chứa truy vấn dữ liệu
- Tên method phải mô tả rõ query: `findActiveById`, `searchByNameContaining`
- Custom query dùng JPQL, tránh native SQL khi có thể
- Native SQL chỉ khi JPQL không đủ (ví dụ: `pg_trgm`, window functions)

```java
// ✅ Đúng
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> searchActive(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    Optional<Product> findByIdAndIsActiveTrue(UUID id);
}
```

---

## 3. Exception Handling

Tất cả exception phải được xử lý tập trung tại `GlobalExceptionHandler`.
Không dùng `try-catch` để swallow exception.

```java
// ✅ Ném exception có nghĩa, để GlobalExceptionHandler bắt
throw new ResourceNotFoundException("Product", productId);
throw new BusinessException("INSUFFICIENT_STOCK", "Not enough stock: " + product.getName());
throw new BusinessException("INVALID_TOPPING", "Topping không thuộc category này");

// ❌ Không dùng
throw new RuntimeException("error");          // quá chung chung
throw new Exception("something went wrong");  // checked exception không cần thiết
try { ... } catch (Exception e) { }           // swallow exception
```

Custom exceptions:
```java
// ResourceNotFoundException → 404
// BusinessException         → 422
// UnauthorizedException     → 401 (hiếm khi cần — Spring Security lo)
// ForbiddenException        → 403
```

---

## 4. Validation

Dùng Bean Validation (`@Valid`) trên DTO, không validate thủ công trong Controller.

```java
// ✅ DTO
public record ProductRequest(
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200)
    String name,

    @NotNull
    @Positive(message = "Giá phải lớn hơn 0")
    BigDecimal price,

    @NotNull(message = "Category không được để trống")
    UUID categoryId,

    @Min(0)
    Integer stockQuantity
) {}

// ✅ Controller chỉ cần @Valid
public ResponseEntity<?> create(@Valid @RequestBody ProductRequest request) {}

// ❌ Không validate thủ công trong controller
if (request.getName() == null || request.getName().isBlank()) {
    throw new RuntimeException("Name required");
}
```

---

## 5. Security Rules

### 5.1 JWT — httpOnly Cookie (không localStorage)

```java
// ✅ Sau khi login thành công, set cookie — KHÔNG trả token trong body
ResponseCookie cookie = ResponseCookie.from("access_token", jwtToken)
        .httpOnly(true)          // JS không đọc được → chống XSS
        .secure(true)            // Chỉ gửi qua HTTPS (false ở dev)
        .sameSite("Strict")      // Chống CSRF
        .path("/")
        .maxAge(Duration.ofDays(7))
        .build();

response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

// Body chỉ trả thông tin user, không trả token
return ResponseEntity.ok(ApiResponse.success(AuthResponse.builder()
        .user(userDto)
        .build()));

// ❌ Tuyệt đối không làm
return ResponseEntity.ok(Map.of("token", jwtToken)); // token lộ ra body → lưu localStorage → XSS
```

### 5.2 JwtAuthFilter — đọc từ cookie

```java
// ✅ Đọc token từ httpOnly cookie
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    String token = null;

    if (request.getCookies() != null) {
        token = Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // Fallback: vẫn hỗ trợ Authorization header cho Swagger UI / mobile future
    if (token == null) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
    }

    if (token != null && jwtTokenProvider.validateToken(token)) {
        // set SecurityContext...
    }
}

// ❌ Chỉ đọc từ header — bỏ qua cookie
String header = request.getHeader("Authorization"); // thiếu cookie fallback
```

### 5.3 Logout — xóa cookie

```java
// ✅ Logout xóa cookie thay vì chỉ xóa localStorage
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
    ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)           // maxAge=0 → browser xóa ngay
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    return ResponseEntity.ok(ApiResponse.success(null, "Logged out", 200));
}

// ❌ Không trả về message "xóa token phía client" — server phải invalidate cookie
```

### 5.4 Password & secrets

```java
// ✅ JWT secret từ env, không hardcode
@Value("${jwt.secret}")
private String jwtSecret;

// ✅ Password hash với BCrypt (strength >= 12 cho production)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// ✅ Luôn kiểm tra ownership khi user truy cập resource của mình
@GetMapping("/orders/{id}")
public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserDetailsImpl currentUser) {
    OrderDetailResponse order = orderService.findByIdAndUser(id, currentUser.getId());
    return ResponseEntity.ok(ApiResponse.success(order));
}

// ❌ Tuyệt đối không hardcode secret
private String secret = "my-secret-key";

// ❌ Không lưu plain text password
user.setPassword(rawPassword);
```

---

## 6. Order Code Generation — Dùng DB Sequence (không Random)

```java
// ❌ KHÔNG DÙNG — RandomStringUtils có thể trùng khi tải cao
String orderCode = "7E-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                 + "-" + RandomStringUtils.randomNumeric(4);
// Với 10,000 đơn/ngày và 4 chữ số → collision probability ~40% (Birthday Problem)

// ✅ ĐÚNG — Dùng PostgreSQL sequence, đảm bảo unique tuyệt đối
// Migration V6__create_orders.sql:
// CREATE SEQUENCE order_daily_seq START 1 INCREMENT 1;

// OrderService.java
@PersistenceContext
private EntityManager entityManager;

private String generateOrderCode() {
    Long seq = (Long) entityManager
            .createNativeQuery("SELECT nextval('order_daily_seq')")
            .getSingleResult();
    String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    return String.format("7E-%s-%04d", date, seq % 10000); // 7E-20240526-0042
}

// ✅ Hoặc đơn giản hơn: dùng UUID rút gọn (8 chars hex) — vẫn tốt hơn random numeric
// Chấp nhận được nếu order code không cần sequential
private String generateOrderCodeSimple() {
    return "7E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

> 📌 **Lý do**: `RandomStringUtils.randomNumeric(4)` chỉ có 10,000 giá trị.
> Với Birthday Problem, chỉ cần ~130 đơn/ngày là có 50% xác suất trùng.
> DB sequence là atomic, không bao giờ trùng dù có nhiều request đồng thời.

---

## 7. Optimistic Locking

Bắt buộc dùng `@Version` trên bảng `products` khi cập nhật `stockQuantity`.

```java
// ✅ Entity
@Entity
public class Product {
    @Version
    private Long version;  // JPA tự xử lý

    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new BusinessException("INSUFFICIENT_STOCK", "...");
        }
        this.stockQuantity -= quantity;
    }
}

// ✅ GlobalExceptionHandler bắt conflict
@ExceptionHandler(OptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("OPTIMISTIC_LOCK", "Dữ liệu đã thay đổi, vui lòng thử lại", 409));
}
```

---

## 8. Response Format

Mọi response đi qua `ApiResponse<T>`. Không trả bare object hay bare list.

```java
// Cấu trúc
ApiResponse.success(data)                    // 200
ApiResponse.success(data, "message", 201)    // 201
ApiResponse.error("CODE", "message", 404)    // error

// ✅ Đúng
return ResponseEntity.ok(ApiResponse.success(productResponse));
return ResponseEntity.status(201).body(ApiResponse.success(response, "Created", 201));

// ❌ Sai
return ResponseEntity.ok(product);           // bare entity
return ResponseEntity.ok(List.of(...));      // bare list
```

---

## 9. Code Hygiene

```java
// ✅ Prefer records cho DTO (immutable, compact)
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}

// ✅ Use Optional correctly
productRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));

// ❌ Không gọi .get() trực tiếp
productRepository.findById(id).get(); // NullPointerException khi không tìm thấy

// ✅ Prefer text blocks cho multiline strings/queries
String query = """
    SELECT p FROM Product p
    WHERE p.isActive = true
    """;

// ✅ Không return null — dùng Optional hoặc throw exception
// ❌ return null; từ service method
```

---

## 10. Những lỗi hay gặp — tránh từ đầu

| Lỗi | Cách tránh |
|-----|-----------|
| `LazyInitializationException` | Dùng `@Transactional(readOnly=true)` trên service, hoặc fetch join trong query |
| N+1 query | Dùng `JOIN FETCH` trong JPQL khi biết sẽ cần related data |
| `OptimisticLockException` không bắt được | Thêm handler vào `GlobalExceptionHandler` |
| Circular dependency | Không inject service vào nhau — tách logic ra class thứ 3 |
| Entity thay đổi nhưng không save | Trong `@Transactional`, JPA auto-flush khi commit — nhưng nếu không trong transaction thì phải gọi `save()` |
| Swagger không hiển thị endpoint bảo mật | Thêm `SecurityScheme` vào `OpenApiConfig` |
| Order code trùng | Dùng DB sequence, không dùng `RandomStringUtils` |
| Token bị XSS đánh cắp | Lưu JWT trong httpOnly cookie, không localStorage |
| CORS block khi có cookie | `allowCredentials(true)` + liệt kê origin cụ thể, không dùng `"*"` |
| Brute force login | Rate limiting trên `/api/v1/auth/**` với Bucket4j |
