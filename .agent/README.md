# 7-Eleven Vietnam — Retail Management System

> Hệ thống quản lý bán lẻ nội bộ cho chuỗi cửa hàng tiện lợi 7-Eleven Vietnam.  
> Bao gồm quản lý sản phẩm, tạo đơn hàng và theo dõi đơn hàng theo thời gian thực.

---

## Bối cảnh & Bài toán

7-Eleven Vietnam vận hành chuỗi cửa hàng tiện lợi 24/7 với danh mục sản phẩm đa dạng — từ đồ uống pha chế, món ăn chế biến sẵn đến hàng tiêu dùng nhanh. Hệ thống hiện tại gặp các vấn đề:

- **Thiếu công cụ quản lý sản phẩm tập trung**: Admin phải cập nhật thủ công, dễ sai lệch tồn kho.
- **Trải nghiệm đặt hàng rời rạc**: Nhân viên hoặc khách hàng không có giao diện thống nhất để tạo đơn theo danh mục.
- **Không có lịch sử đơn hàng**: Khó theo dõi và kiểm soát doanh thu theo thời gian.

Dự án này xây dựng một **MVP (Minimum Viable Product)** giải quyết 3 bài toán trên trong một hệ thống web duy nhất, có thể mở rộng theo hướng production sau này.

---

## Tính năng chính

### Dành cho Admin
- Quản lý danh mục sản phẩm (2 cấp: category gốc → sub-category)
- CRUD sản phẩm: thêm, sửa, xóa mềm (soft delete), xem chi tiết
- Tìm kiếm & lọc sản phẩm theo tên, danh mục, trạng thái
- Phân trang danh sách sản phẩm
- Xem danh sách đơn hàng và chi tiết từng đơn
- Quản lý topping theo danh mục gốc

### Dành cho User
- Duyệt sản phẩm theo danh mục có phân cấp
- Thêm sản phẩm vào giỏ hàng, chọn topping tùy chọn
- Tạo đơn hàng từ giỏ hàng
- Xem lịch sử đơn hàng cá nhân

### Hệ thống
- Xác thực JWT, phân quyền theo role (`ADMIN` / `USER`)
- Optimistic locking tránh race condition khi cập nhật tồn kho
- Response chuẩn hóa toàn bộ API
- Chạy toàn bộ stack bằng một lệnh Docker Compose

---

## Tech Stack (tóm tắt)

| Layer      | Công nghệ                                     |
|------------|-----------------------------------------------|
| Frontend   | React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui |
| Backend    | Java 21, Spring Boot 3.2, Spring Security, JPA |
| Database   | PostgreSQL 16                                 |
| Auth       | JWT (access token, 7 ngày)                   |
| Infra      | Docker Compose, Nginx                         |

> Chi tiết phiên bản và kiến trúc xem tại [ARCHITECTURE.md](./ARCHITECTURE.md)  
> Thiết kế database và API xem tại [DESIGN.md](./DESIGN.md)

---

## Quick Start

### Yêu cầu
- Docker Desktop >= 24.x
- Docker Compose >= 2.x

### Chạy toàn bộ stack

```bash
git clone https://github.com/Hoangjunss/app_7eleven.git
cd app_7eleven
docker compose up --build
```

| Service   | URL                        |
|-----------|----------------------------|
| Frontend  | http://localhost:3000       |
| Backend   | http://localhost:8080       |
| API Docs  | http://localhost:8080/swagger-ui.html |

### Tài khoản mặc định (seed data)

| Role  | Email                  | Password   |
|-------|------------------------|------------|
| Admin | admin@7eleven.vn       | Admin@123  |
| User  | user@7eleven.vn        | User@123   |

### Chạy từng service riêng (development)

```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

---

## Cấu trúc thư mục gốc

```
app_7eleven/
├── backend/          # Spring Boot application
├── frontend/         # React + Vite application
├── .agent/           # AI agent rules & skills
│   ├── backend/rules/
│   ├── frontend/rules/
│   ├── review/
│   └── unit-test/
├── docker-compose.yml
├── README.md
├── ARCHITECTURE.md
└── DESIGN.md
```

---

## Git Workflow

Dự án tuân theo **Simplified Git Flow**:

- `main` — production-ready, không commit trực tiếp
- `develop` — integration branch chính
- `feature/*` — tính năng mới, tách từ `develop`
- `bugfix/*` — sửa lỗi, tách từ `develop`
- `hotfix/*` — sửa khẩn cấp trên `main`, merge về cả `main` và `develop`

---

## Giới hạn MVP (Out of scope)

Những tính năng dưới đây **không** nằm trong phạm vi MVP, tránh over-engineering:

- Kafka / RabbitMQ / message queue
- WebSocket / SSE real-time push
- OAuth2, refresh token, token blacklist
- Multiple backend instances / horizontal scaling
- Gửi email xác nhận đơn hàng
- Payment gateway tích hợp thật