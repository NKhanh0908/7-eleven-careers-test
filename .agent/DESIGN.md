# DESIGN.md — Thiết kế chi tiết

---

## 1. Database Schema

### 1.1 Tổng quan quan hệ

```
users ─────────────────────────────────┐
  │                                    │
  │ 1:N                                │ 1:N
  ▼                                    ▼
orders ──── 1:N ──── order_items     (created_by / managed_by)
                          │
                    N:1 ──┤── products ──── N:1 ──── categories (level=1, sub)
                          │                               │
                          │                         N:1 (parent_id)
                    N:N via                              │
              order_item_toppings              categories (level=0, root)
                          │
                     N:1 ──── toppings ──── N:N via ──── category_toppings
                                                               │
                                                          N:1 ──── categories (level=0)
```

### 1.2 Chi tiết từng bảng

---

#### `users`

| Cột           | Kiểu              | Ràng buộc                    | Ghi chú                   |
|---------------|-------------------|------------------------------|---------------------------|
| id            | UUID              | PK, default gen_random_uuid()| |
| email         | VARCHAR(255)      | NOT NULL, UNIQUE             | |
| password_hash | VARCHAR(255)      | NOT NULL                     | BCrypt                    |
| full_name     | VARCHAR(100)      | NOT NULL                     | |
| role          | VARCHAR(20)       | NOT NULL, default 'USER'     | 'ADMIN' hoặc 'USER'      |
| is_active     | BOOLEAN           | NOT NULL, default true       | |
| created_at    | TIMESTAMPTZ       | NOT NULL, default now()      | |
| updated_at    | TIMESTAMPTZ       | NOT NULL, default now()      | |

---

#### `categories`

| Cột        | Kiểu         | Ràng buộc                   | Ghi chú                             |
|------------|--------------|-----------------------------|-------------------------------------|
| id         | UUID         | PK                          | |
| name       | VARCHAR(100) | NOT NULL                    | Ví dụ: "Trà Sữa & Trà Trái Cây"   |
| slug       | VARCHAR(120) | NOT NULL, UNIQUE            | URL-friendly, ví dụ: "tra-sua"     |
| parent_id  | UUID         | FK → categories(id), NULL   | NULL = category gốc (level 0)      |
| level      | SMALLINT     | NOT NULL, default 0         | 0 = gốc, 1 = sub-category          |
| sort_order | SMALLINT     | NOT NULL, default 0         | Thứ tự hiển thị                    |
| is_active  | BOOLEAN      | NOT NULL, default true      | |
| created_at | TIMESTAMPTZ  | NOT NULL, default now()     | |
| updated_at | TIMESTAMPTZ  | NOT NULL, default now()     | |

**Index:** `idx_categories_parent_id` trên cột `parent_id`

---

#### `products`

| Cột            | Kiểu           | Ràng buộc                  | Ghi chú                             |
|----------------|----------------|----------------------------|-------------------------------------|
| id             | UUID           | PK                         | |
| name           | VARCHAR(200)   | NOT NULL                   | |
| description    | TEXT           |                            | |
| price          | NUMERIC(12,0)  | NOT NULL, CHECK > 0        | Đơn vị: VNĐ                        |
| image_url      | VARCHAR(500)   |                            | |
| stock_quantity | INTEGER        | NOT NULL, default 0        | |
| version        | BIGINT         | NOT NULL, default 0        | Optimistic lock (@Version JPA)     |
| category_id    | UUID           | FK → categories(id), NOT NULL | Trỏ đến sub-category (level=1)  |
| is_active      | BOOLEAN        | NOT NULL, default true     | Soft delete flag                   |
| deleted_at     | TIMESTAMPTZ    |                            | Soft delete timestamp              |
| created_at     | TIMESTAMPTZ    | NOT NULL, default now()    | |
| updated_at     | TIMESTAMPTZ    | NOT NULL, default now()    | |

**Index:**
- `idx_products_category_id` trên `category_id`
- `idx_products_name_trgm` trên `name` dùng `gin_trgm_ops` (tìm kiếm ILIKE)
- `idx_products_is_active` trên `is_active`

---

#### `toppings`

| Cột         | Kiểu          | Ràng buộc               | Ghi chú              |
|-------------|---------------|-------------------------|----------------------|
| id          | UUID          | PK                      | |
| name        | VARCHAR(100)  | NOT NULL                | Ví dụ: "Trân châu"  |
| extra_price | NUMERIC(12,0) | NOT NULL, default 0     | Phụ thu thêm        |
| is_active   | BOOLEAN       | NOT NULL, default true  | |
| created_at  | TIMESTAMPTZ   | NOT NULL, default now() | |

---

#### `category_toppings` (junction N:N)

| Cột         | Kiểu | Ràng buộc                           |
|-------------|------|-------------------------------------|
| category_id | UUID | FK → categories(id), NOT NULL       |
| topping_id  | UUID | FK → toppings(id), NOT NULL         |

**PK:** `(category_id, topping_id)` — composite primary key  
Logic: `category_id` trỏ đến **root category** (level=0). Tất cả sub-category con sẽ kế thừa topping qua parent.

---

#### `orders`

| Cột          | Kiểu          | Ràng buộc                    | Ghi chú                              |
|--------------|---------------|------------------------------|--------------------------------------|
| id           | UUID          | PK                           | |
| order_code   | VARCHAR(20)   | NOT NULL, UNIQUE             | Ví dụ: "7E-20240526-0001"           |
| user_id      | UUID          | FK → users(id), NOT NULL     | |
| status       | VARCHAR(30)   | NOT NULL, default 'PENDING'  | Xem Order Status bên dưới           |
| total_amount | NUMERIC(14,0) | NOT NULL                     | Tổng tiền tại thời điểm đặt         |
| note         | TEXT          |                              | Ghi chú của khách                   |
| created_at   | TIMESTAMPTZ   | NOT NULL, default now()      | |
| updated_at   | TIMESTAMPTZ   | NOT NULL, default now()      | |

**Index:** `idx_orders_user_id`, `idx_orders_status`, `idx_orders_created_at DESC`

---

#### `order_items`

| Cột          | Kiểu          | Ràng buộc                      | Ghi chú                            |
|--------------|---------------|--------------------------------|------------------------------------|
| id           | UUID          | PK                             | |
| order_id     | UUID          | FK → orders(id), NOT NULL      | |
| product_id   | UUID          | FK → products(id), NOT NULL    | |
| product_name | VARCHAR(200)  | NOT NULL                       | Snapshot tên lúc đặt               |
| unit_price   | NUMERIC(12,0) | NOT NULL                       | Snapshot giá lúc đặt               |
| quantity     | INTEGER       | NOT NULL, CHECK > 0            | |
| subtotal     | NUMERIC(14,0) | NOT NULL                       | unit_price × quantity + toppings   |

---

#### `order_item_toppings`

| Cột           | Kiểu          | Ràng buộc                         | Ghi chú                     |
|---------------|---------------|-----------------------------------|-----------------------------|
| id            | UUID          | PK                                | |
| order_item_id | UUID          | FK → order_items(id), NOT NULL    | |
| topping_id    | UUID          | FK → toppings(id), NOT NULL       | |
| topping_name  | VARCHAR(100)  | NOT NULL                          | Snapshot tên topping        |
| extra_price   | NUMERIC(12,0) | NOT NULL                          | Snapshot giá topping        |

---

### 1.3 Flyway migration order

```
V1__create_users.sql
V2__create_categories.sql
V3__create_products.sql              -- phụ thuộc categories
V4__create_toppings.sql
V5__create_category_toppings.sql     -- phụ thuộc categories, toppings
V6__create_orders.sql                -- phụ thuộc users
V7__create_order_items.sql           -- phụ thuộc orders, products
V8__create_order_item_toppings.sql   -- phụ thuộc order_items, toppings
V9__seed_data.sql                    -- insert test data
```

---

## 2. API Specifications

### 2.1 Cấu trúc Response chuẩn

**Success:**
```json
{
  "status": 200,
  "message": "Success",
  "data": { ... }
}
```

**Paginated:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "content": [ ... ],
    "totalElements": 42,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

**Error:**
```json
{
  "status": 404,
  "message": "Product not found",
  "data": null,
  "errors": { "field": "error detail" }
}
```

---

### 2.2 Auth API

#### `POST /api/v1/auth/login`
```json
// Request
{
  "email": "admin@7eleven.vn",
  "password": "Admin@123"
}

// Response 200
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "user": {
      "id": "uuid",
      "email": "admin@7eleven.vn",
      "fullName": "Admin",
      "role": "ADMIN"
    }
  }
}
```

#### `POST /api/v1/auth/register`
```json
// Request
{
  "email": "user@example.com",
  "password": "Pass@123",
  "fullName": "Nguyen Van A"
}

// Response 201
{
  "status": 201,
  "message": "Registration successful",
  "data": { "id": "uuid", "email": "...", "role": "USER" }
}
```

---

### 2.3 Category API

#### `GET /api/v1/categories`
Trả về cây category đầy đủ (root + children).
```json
// Response 200
{
  "data": [
    {
      "id": "uuid-1",
      "name": "Trà Sữa & Trà Trái Cây",
      "slug": "tra-sua-tra-trai-cay",
      "level": 0,
      "children": [
        { "id": "uuid-2", "name": "Trà sữa 7-Eleven", "slug": "tra-sua-7eleven", "level": 1 },
        { "id": "uuid-3", "name": "Trà trái cây", "slug": "tra-trai-cay", "level": 1 }
      ]
    }
  ]
}
```

#### `GET /api/v1/categories/{id}/toppings`
Lấy danh sách topping của root category (dùng khi user chọn sản phẩm).

---

### 2.4 Product API (User)

#### `GET /api/v1/products`
```
Query params:
  - categoryId  (UUID, optional) — lọc theo sub-category
  - search      (string, optional) — tìm theo tên (ILIKE)
  - page        (int, default 0)
  - size        (int, default 20)
```
```json
// Response 200
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Trà Tắc sz L",
        "price": 17000,
        "imageUrl": "https://...",
        "stockQuantity": 50,
        "category": { "id": "uuid", "name": "Trà trái cây" }
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

#### `GET /api/v1/products/{id}`
Trả về chi tiết sản phẩm + topping khả dụng.
```json
{
  "data": {
    "id": "uuid",
    "name": "Trà Tắc sz L",
    "price": 17000,
    "description": "...",
    "imageUrl": "...",
    "stockQuantity": 50,
    "category": { "id": "...", "name": "Trà trái cây", "parentId": "..." },
    "availableToppings": [
      { "id": "uuid", "name": "Trân châu đen", "extraPrice": 5000 },
      { "id": "uuid", "name": "Thạch dừa", "extraPrice": 3000 }
    ]
  }
}
```

---

### 2.5 Product API (Admin) — yêu cầu role ADMIN

#### `GET /api/v1/admin/products`
```
Query params: categoryId, search, isActive (boolean), page, size
```

#### `POST /api/v1/admin/products`
```json
// Request
{
  "name": "Trà Tắc Nhà Bảy Size XL",
  "description": "Trà tắc tươi mát",
  "price": 23000,
  "imageUrl": "https://...",
  "stockQuantity": 100,
  "categoryId": "uuid-sub-category"
}

// Response 201
{ "status": 201, "message": "Product created", "data": { "id": "uuid", ... } }
```

#### `PUT /api/v1/admin/products/{id}`
Body tương tự POST, response 200.

#### `DELETE /api/v1/admin/products/{id}`
Soft delete: set `is_active=false`, `deleted_at=now()`.
```json
// Response 200
{ "status": 200, "message": "Product deleted", "data": null }
```

#### `PATCH /api/v1/admin/products/{id}/stock`
```json
// Request
{ "stockQuantity": 200 }
```

---

### 2.6 Order API (User)

#### `POST /api/v1/orders`
```json
// Request
{
  "note": "Ít đường",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2,
      "toppingIds": ["uuid-topping-1", "uuid-topping-2"]
    },
    {
      "productId": "uuid-2",
      "quantity": 1,
      "toppingIds": []
    }
  ]
}

// Response 201
{
  "status": 201,
  "message": "Order created successfully",
  "data": {
    "id": "uuid",
    "orderCode": "7E-20240526-0001",
    "status": "PENDING",
    "totalAmount": 58000,
    "createdAt": "2024-05-26T10:30:00Z"
  }
}
```

**Validation phía backend:**
- Mỗi `productId` phải tồn tại và `is_active=true`
- `stockQuantity` đủ sau khi trừ
- `toppingIds` phải thuộc root category của sản phẩm

#### `GET /api/v1/orders/my`
Lịch sử đơn hàng của user đang đăng nhập.
```
Query params: status, page, size
```

---

### 2.7 Order API (Admin)

#### `GET /api/v1/admin/orders`
```
Query params: status, userId, fromDate, toDate, page, size
```
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "orderCode": "7E-20240526-0001",
        "user": { "id": "uuid", "email": "user@7eleven.vn", "fullName": "..." },
        "status": "PENDING",
        "totalAmount": 58000,
        "itemCount": 3,
        "createdAt": "2024-05-26T10:30:00Z"
      }
    ],
    ...pagination
  }
}
```

#### `GET /api/v1/admin/orders/{id}`
Chi tiết đơn hàng đầy đủ bao gồm items và toppings.
```json
{
  "data": {
    "id": "uuid",
    "orderCode": "7E-20240526-0001",
    "user": { ... },
    "status": "PENDING",
    "totalAmount": 58000,
    "note": "Ít đường",
    "items": [
      {
        "id": "uuid",
        "productName": "Trà Tắc sz L",
        "unitPrice": 17000,
        "quantity": 2,
        "subtotal": 39000,
        "toppings": [
          { "toppingName": "Trân châu đen", "extraPrice": 5000 }
        ]
      }
    ],
    "createdAt": "2024-05-26T10:30:00Z"
  }
}
```

#### `PATCH /api/v1/admin/orders/{id}/status`
```json
// Request
{ "status": "CONFIRMED" }
```

---

## 3. Order Status Flow

```
                    ┌─────────┐
            tạo đơn │ PENDING │
            ────────►         │
                    └────┬────┘
                         │ Admin xác nhận
                    ┌────▼────┐
                    │CONFIRMED│
                    └────┬────┘
                         │ Chuẩn bị xong
                    ┌────▼────┐
                    │  READY  │
                    └────┬────┘
                         │ Giao/Lấy hàng
                    ┌────▼────────┐
                    │  COMPLETED  │
                    └─────────────┘

         Từ PENDING hoặc CONFIRMED:
                    ┌────────────┐
                    │ CANCELLED  │◄── Admin hoặc User huỷ
                    └────────────┘
```

**Quy tắc chuyển trạng thái:**
- `PENDING` → `CONFIRMED` hoặc `CANCELLED` (Admin)
- `CONFIRMED` → `READY` hoặc `CANCELLED` (Admin)
- `READY` → `COMPLETED` (Admin)
- `COMPLETED` và `CANCELLED` là **terminal state** — không thể thay đổi

---

## 4. Logic nghiệp vụ quan trọng

### 4.1 Tạo đơn hàng — xử lý race condition

Khi nhiều user đặt cùng 1 sản phẩm cùng lúc, dùng **Optimistic Locking**:

```java
// Product entity
@Version
private Long version;

// OrderService
@Transactional
public OrderResponse createOrder(CreateOrderRequest request, UUID userId) {
    for (OrderItemRequest item : request.getItems()) {
        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() < item.getQuantity()) {
            throw new BusinessException("Insufficient stock: " + product.getName());
        }

        // JPA sẽ throw OptimisticLockException nếu version conflict
        product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
        productRepository.save(product);
    }
    // ... tạo Order, OrderItems, OrderItemToppings
}
```

`GlobalExceptionHandler` bắt `OptimisticLockException` → trả về HTTP 409 Conflict.

### 4.2 Tính giá đơn hàng

```
subtotal của 1 item = (unit_price + sum(topping.extra_price)) × quantity
total_amount của order = sum(subtotal của tất cả items)
```

Giá **phải snapshot tại thời điểm đặt** vào `order_items.unit_price` và `order_item_toppings.extra_price`. Không query lại `products.price` sau khi đơn đã tạo.

### 4.3 Lấy topping cho sản phẩm

```sql
-- Lấy toppings khả dụng cho 1 sản phẩm
SELECT t.*
FROM toppings t
JOIN category_toppings ct ON ct.topping_id = t.id
JOIN categories sub ON sub.id = :categoryId          -- sub-category của sản phẩm
WHERE ct.category_id = sub.parent_id                 -- tìm root category
  AND t.is_active = true
```

### 4.4 Soft delete sản phẩm

Khi xóa sản phẩm (Admin):
1. Set `is_active = false`, `deleted_at = now()`
2. Sản phẩm không hiện trong danh sách user
3. Các `order_items` cũ vẫn hợp lệ vì đã snapshot `product_name` và `unit_price`
4. Admin vẫn có thể xem sản phẩm đã xóa với filter `isActive=false`

---

## 5. Error Codes

| HTTP Status | Code                  | Tình huống                                 |
|-------------|----------------------|--------------------------------------------|
| 400         | VALIDATION_ERROR     | Request body không hợp lệ                 |
| 401         | UNAUTHORIZED         | Không có hoặc token hết hạn               |
| 403         | FORBIDDEN            | Không đủ quyền (sai role)                 |
| 404         | RESOURCE_NOT_FOUND   | Entity không tồn tại                      |
| 409         | OPTIMISTIC_LOCK      | Race condition khi cập nhật stock         |
| 409         | INSUFFICIENT_STOCK   | Tồn kho không đủ khi tạo đơn             |
| 422         | BUSINESS_ERROR       | Vi phạm quy tắc nghiệp vụ                |
| 500         | INTERNAL_ERROR       | Lỗi hệ thống không xác định              |