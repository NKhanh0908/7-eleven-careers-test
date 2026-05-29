---
trigger: on_task_start
applies_to: backend/src/test/**
---

# Backend Testing Standards

Mọi unit test và integration test trong project phải tuân thủ các quy tắc dưới đây.
AI agent phải viết test sau khi hoàn thành mỗi Service class — không bỏ qua.

---

## 1. Triết lý test

- **Test behavior, không test implementation**: test xem method làm đúng không, không test nó gọi method nào bao nhiêu lần.
- **1 test = 1 behavior**: mỗi `@Test` chỉ kiểm tra đúng 1 điều.
- **AAA pattern**: Arrange → Act → Assert — luôn theo thứ tự này.
- **Test trước khi commit**: không có test = không được merge.

---

## 2. Naming Convention

```java
// ✅ Format: methodName_scenario_expectedResult
@Test
void createProduct_whenCategoryNotFound_throwsResourceNotFoundException() {}

@Test
void createOrder_whenStockInsufficient_throwsBusinessException() {}

@Test
void findById_whenProductIsActive_returnsProductResponse() {}

@Test
void softDelete_whenProductExists_setsIsActiveFalse() {}

// ❌ Không đặt tên chung chung
@Test
void testCreate() {}

@Test
void test1() {}
```

---

## 3. Cấu trúc test file

```java
// ✅ Unit test cho Service
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // 1. Mock dependencies — không dùng @Autowired
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    // 2. Class under test — @InjectMocks
    @InjectMocks
    private ProductService productService;

    // 3. Test data constants — dùng lại, không tạo lặp
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    // 4. Factory method cho test data
    private Product buildActiveProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Trà Tắc sz L");
        product.setPrice(new BigDecimal("17000"));
        product.setStockQuantity(50);
        product.setIsActive(true);
        return product;
    }

    private Category buildSubCategory() {
        Category root = new Category();
        root.setId(UUID.randomUUID());
        root.setLevel(0);

        Category sub = new Category();
        sub.setId(CATEGORY_ID);
        sub.setLevel(1);
        sub.setParent(root);
        return sub;
    }

    // 5. Tests nhóm theo method
    @Nested
    class FindById {
        @Test
        void findById_whenProductExists_returnsProductResponse() {
            // Arrange
            Product product = buildActiveProduct();
            when(productRepository.findByIdAndIsActiveTrue(PRODUCT_ID))
                    .thenReturn(Optional.of(product));

            // Act
            ProductResponse result = productService.findById(PRODUCT_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(PRODUCT_ID);
            assertThat(result.name()).isEqualTo("Trà Tắc sz L");
        }

        @Test
        void findById_whenProductNotFound_throwsResourceNotFoundException() {
            // Arrange
            when(productRepository.findByIdAndIsActiveTrue(PRODUCT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.findById(PRODUCT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }
}
```

---

## 4. Service Tests — Phải viết cho mỗi domain

### ProductService — test bắt buộc

```java
// createProduct
void createProduct_whenValidRequest_returnsProductResponse()
void createProduct_whenCategoryNotFound_throwsResourceNotFoundException()
void createProduct_whenSubCategoryIsRoot_throwsBusinessException()  // level=0 không hợp lệ

// updateProduct
void updateProduct_whenProductExists_updatesAndReturnsResponse()
void updateProduct_whenProductNotFound_throwsResourceNotFoundException()

// softDeleteProduct
void softDeleteProduct_whenProductExists_setsIsActiveFalseAndDeletedAt()
void softDeleteProduct_whenProductNotFound_throwsResourceNotFoundException()

// searchProducts
void searchProducts_withNullFilters_returnsAllActiveProducts()
void searchProducts_withCategoryFilter_returnsFilteredProducts()
```

### OrderService — test bắt buộc

```java
// createOrder
void createOrder_whenValidRequest_createsOrderAndReturnsResponse()
void createOrder_whenProductNotFound_throwsResourceNotFoundException()
void createOrder_whenProductInactive_throwsResourceNotFoundException()
void createOrder_whenStockInsufficient_throwsBusinessException()
void createOrder_whenInvalidTopping_throwsBusinessException()  // topping không thuộc root category
void createOrder_decreasesStockQuantityForEachItem()
void createOrder_snapshotsProductNameAndPrice()   // giá phải là snapshot, không reference

// findByIdAndUser — ownership check
void findByIdAndUser_whenOrderBelongsToUser_returnsOrderDetail()
void findByIdAndUser_whenOrderBelongsToDifferentUser_throwsForbiddenException()
```

### AuthService — test bắt buộc

```java
void login_whenValidCredentials_returnsAuthResponse()
void login_whenUserNotFound_throwsUnauthorizedException()
void login_whenWrongPassword_throwsUnauthorizedException()
void login_whenUserInactive_throwsUnauthorizedException()
void register_whenEmailAlreadyExists_throwsBusinessException()
void register_whenValidRequest_encodesPasswordBeforeSave()  // password không được lưu plain text
```

---

## 5. Assert patterns — Dùng AssertJ (không JUnit assertEquals)

```java
// ✅ AssertJ — fluent, rõ ràng hơn
import static org.assertj.core.api.Assertions.*;

// Kiểm tra giá trị
assertThat(result.getName()).isEqualTo("Trà Tắc");
assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("17000"));
assertThat(result.getStockQuantity()).isPositive();
assertThat(result.getIsActive()).isTrue();

// Kiểm tra collection
assertThat(result.getItems()).hasSize(2);
assertThat(result.getItems()).extracting("productName")
        .containsExactlyInAnyOrder("Trà Tắc sz L", "Cà Phê Sữa");

// Kiểm tra exception
assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product");

// Kiểm tra exception với custom assertion
assertThatThrownBy(() -> service.createOrder(request, userId))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> {
            BusinessException be = (BusinessException) ex;
            assertThat(be.getErrorCode()).isEqualTo("INSUFFICIENT_STOCK");
        });

// Kiểm tra method được gọi (dùng hạn chế — chỉ khi side effect quan trọng)
verify(productRepository, times(1)).save(any(Product.class));
verify(productRepository, never()).delete(any());

// ❌ Không dùng JUnit assertEquals trực tiếp
assertEquals("Trà Tắc", result.getName()); // kém readable hơn
```

---

## 6. Mock patterns

```java
// ✅ Mock return value
when(productRepository.findByIdAndIsActiveTrue(PRODUCT_ID))
        .thenReturn(Optional.of(buildActiveProduct()));

// ✅ Mock void method
doNothing().when(productRepository).delete(any());

// ✅ Mock exception
when(productRepository.findById(any()))
        .thenThrow(new RuntimeException("DB error"));

// ✅ ArgumentCaptor — bắt argument được truyền vào mock
ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
verify(productRepository).save(productCaptor.capture());
Product savedProduct = productCaptor.getValue();
assertThat(savedProduct.getIsActive()).isFalse();   // verify soft delete đúng
assertThat(savedProduct.getDeletedAt()).isNotNull();

// ❌ Không mock quá nhiều — nếu cần mock > 5 dependencies thì cân nhắc refactor service
```

---

## 7. Test data builders

Tạo class helper để tránh lặp lại code tạo test data:

```java
// test/java/com/seveneleven/helper/TestDataBuilder.java
public class TestDataBuilder {

    public static Product activeProduct() {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName("Test Product");
        p.setPrice(new BigDecimal("20000"));
        p.setStockQuantity(100);
        p.setIsActive(true);
        p.setVersion(0L);
        return p;
    }

    public static Product activeProduct(UUID id, String name, int stock) {
        Product p = activeProduct();
        p.setId(id);
        p.setName(name);
        p.setStockQuantity(stock);
        return p;
    }

    public static Category rootCategory() {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName("Trà Sữa & Trà Trái Cây");
        c.setLevel(0);
        return c;
    }

    public static Category subCategory(Category parent) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName("Trà trái cây");
        c.setLevel(1);
        c.setParent(parent);
        return c;
    }

    public static CreateOrderRequest validOrderRequest(UUID productId) {
        OrderItemRequest item = new OrderItemRequest(productId, 2, List.of());
        return new CreateOrderRequest("Ít đường", List.of(item));
    }
}
```

---

## 8. Những gì KHÔNG cần test

| Không cần test | Lý do |
|---|---|
| Repository methods tên `findBy*` (Spring Data auto-generate) | Spring tự test, không phải code của bạn |
| Getter/Setter (Lombok) | Không có logic |
| Entity constructors | Không có logic |
| `@Configuration` class đơn giản | Không có business logic |
| Controller layer (unit) | Test qua integration test hoặc Swagger manual |

---

## 9. Checklist trước khi commit

- [ ] Mỗi Service class có file `*ServiceTest.java` tương ứng?
- [ ] Coverage các happy path (thành công)?
- [ ] Coverage các error path (exception cases)?
- [ ] Không có test nào tên `test1`, `test2`, `testSomething` chung chung?
- [ ] Không mock repository trả `null` thay vì `Optional.empty()`?
- [ ] Test có thể chạy độc lập (không phụ thuộc thứ tự chạy)?
- [ ] Không có `Thread.sleep()` trong unit test?
- [ ] Tất cả test pass với `./mvnw test`?
