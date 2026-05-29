-- Root Categories
INSERT INTO categories (id, name, slug, parent_id, level, sort_order, is_active, created_at, updated_at) VALUES
(CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'Trà Sữa & Trà Trái Cây', 'tra-sua-tra-trai-cay', NULL, 0, 1, true, NOW(), NOW()),
(CAST('22222222-2222-2222-2222-222222222221' AS UUID), 'Cà phê', 'ca-phe', NULL, 0, 2, true, NOW(), NOW()),
(CAST('55555555-5555-5555-5555-555555555551' AS UUID), 'Thức Ăn', 'thuc-an', NULL, 0, 3, true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Sub Categories
INSERT INTO categories (id, name, slug, parent_id, level, sort_order, is_active, created_at, updated_at) VALUES
(CAST('11111111-1111-1111-1111-111111111112' AS UUID), 'Trà sữa 7-Eleven', 'tra-sua-7eleven', CAST('11111111-1111-1111-1111-111111111111' AS UUID), 1, 1, true, NOW(), NOW()),
(CAST('11111111-1111-1111-1111-111111111113' AS UUID), 'Trà trái cây', 'tra-trai-cay', CAST('11111111-1111-1111-1111-111111111111' AS UUID), 1, 2, true, NOW(), NOW()),
(CAST('22222222-2222-2222-2222-222222222222' AS UUID), 'Cà phê pha máy', 'ca-phe-pha-may', CAST('22222222-2222-2222-2222-222222222221' AS UUID), 1, 1, true, NOW(), NOW()),
(CAST('55555555-5555-5555-5555-555555555552' AS UUID), 'Bánh mì & Sandwich', 'banh-mi-sandwich', CAST('55555555-5555-5555-5555-555555555551' AS UUID), 1, 1, true, NOW(), NOW()),
(CAST('55555555-5555-5555-5555-555555555553' AS UUID), 'Mì & Lẩu Ly', 'mi-lau-ly', CAST('55555555-5555-5555-5555-555555555551' AS UUID), 1, 2, true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- Toppings
INSERT INTO toppings (id, name, extra_price, is_active, created_at) VALUES
(CAST('33333333-3333-3333-3333-333333333331' AS UUID), 'Trân châu đen', 5000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333332' AS UUID), 'Thạch dừa', 3000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333333' AS UUID), 'Kem cheese', 10000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333334' AS UUID), 'Trân châu trắng', 6000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333335' AS UUID), 'Patê thêm', 5000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333336' AS UUID), 'Trứng ốp la', 7000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333337' AS UUID), 'Chả lụa thêm', 8000, true, NOW()),
(CAST('33333333-3333-3333-3333-333333333338' AS UUID), 'Phô mai lát', 6000, true, NOW())
ON CONFLICT DO NOTHING;

-- Category Toppings (Mapping root categories to toppings)
INSERT INTO category_toppings (category_id, topping_id) VALUES
-- Trà Sữa & Trà Trái Cây -> Trân châu đen, Thạch dừa, Kem cheese, Trân châu trắng
(CAST('11111111-1111-1111-1111-111111111111' AS UUID), CAST('33333333-3333-3333-3333-333333333331' AS UUID)),
(CAST('11111111-1111-1111-1111-111111111111' AS UUID), CAST('33333333-3333-3333-3333-333333333332' AS UUID)),
(CAST('11111111-1111-1111-1111-111111111111' AS UUID), CAST('33333333-3333-3333-3333-333333333333' AS UUID)),
(CAST('11111111-1111-1111-1111-111111111111' AS UUID), CAST('33333333-3333-3333-3333-333333333334' AS UUID)),
-- Cà phê -> Trân châu đen, Kem cheese
(CAST('22222222-2222-2222-2222-222222222221' AS UUID), CAST('33333333-3333-3333-3333-333333333331' AS UUID)),
(CAST('22222222-2222-2222-2222-222222222221' AS UUID), CAST('33333333-3333-3333-3333-333333333333' AS UUID)),
-- Thức Ăn -> Patê thêm, Trứng ốp la, Chả lụa thêm, Phô mai lát
(CAST('55555555-5555-5555-5555-555555555551' AS UUID), CAST('33333333-3333-3333-3333-333333333335' AS UUID)),
(CAST('55555555-5555-5555-5555-555555555551' AS UUID), CAST('33333333-3333-3333-3333-333333333336' AS UUID)),
(CAST('55555555-5555-5555-5555-555555555551' AS UUID), CAST('33333333-3333-3333-3333-333333333337' AS UUID)),
(CAST('55555555-5555-5555-5555-555555555551' AS UUID), CAST('33333333-3333-3333-3333-333333333338' AS UUID))
ON CONFLICT DO NOTHING;

-- Products
INSERT INTO products (id, name, description, price, image_url, stock_quantity, category_id, is_active, version, created_at, updated_at) VALUES
-- Trà sữa 7-Eleven
(CAST('44444444-4444-4444-4444-444444444441' AS UUID), 'Trà Sữa 7-Eleven Truyền Thống', 'Trà sữa đậm vị đặc trưng, béo ngậy chuẩn vị 7E', 25000, '', 100, CAST('11111111-1111-1111-1111-111111111112' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444442' AS UUID), 'Trà Sữa Thái Xanh', 'Thơm mùi lá dứa, thanh mát giải nhiệt mùa hè', 28000, '', 50, CAST('11111111-1111-1111-1111-111111111112' AS UUID), true, 0, NOW(), NOW()),
-- Trà trái cây
(CAST('44444444-4444-4444-4444-444444444443' AS UUID), 'Trà Tắc sz L', 'Thanh mát giải nhiệt với tắc tươi và trà lài', 17000, '', 50, CAST('11111111-1111-1111-1111-111111111113' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444444' AS UUID), 'Trà Tắc Nhà Bảy Size XL', 'Trà tắc tươi mát khổng lồ giải khát sảng khoái', 23000, '', 100, CAST('11111111-1111-1111-1111-111111111113' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444445' AS UUID), 'Trà Đào Cam Sả', 'Ngọt ngào hương đào, thơm thoảng sả tươi và cam vàng', 35000, '', 40, CAST('11111111-1111-1111-1111-111111111113' AS UUID), true, 0, NOW(), NOW()),
-- Cà phê pha máy
(CAST('44444444-4444-4444-4444-444444444446' AS UUID), 'Espresso', 'Cà phê nguyên chất pha máy đậm đà', 25000, '', 100, CAST('22222222-2222-2222-2222-222222222222' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444447' AS UUID), 'Bạc Xỉu', 'Cà phê sữa đá ngọt ngào kiểu Sài Gòn', 29000, '', 80, CAST('22222222-2222-2222-2222-222222222222' AS UUID), true, 0, NOW(), NOW()),
-- Bánh mì & Sandwich
(CAST('44444444-4444-4444-4444-444444444448' AS UUID), 'Bánh Mì Que Hải Phòng', 'Bánh mì que giòn tan kèm pate béo ngậy đặc sản', 15000, '', 100, CAST('55555555-5555-5555-5555-555555555552' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444449' AS UUID), 'Bánh Mì Patê Chả Lụa', 'Bánh mì truyền thống Việt Nam với pate và chả lụa đậm đà', 25000, '', 80, CAST('55555555-5555-5555-5555-555555555552' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444450' AS UUID), 'Sandwich Tam Giác Kẹp Cá Ngừ', 'Sandwich tươi ngon, tiện lợi cho bữa sáng đầy năng lượng', 22000, '', 50, CAST('55555555-5555-5555-5555-555555555552' AS UUID), true, 0, NOW(), NOW()),
-- Mì & Lẩu Ly
(CAST('44444444-4444-4444-4444-444444444451' AS UUID), 'Mì Trộn Cay 7-Eleven', 'Mì trộn sốt cay đặc trưng chuẩn vị đường phố Hàn Quốc', 29000, '', 60, CAST('55555555-5555-5555-5555-555555555553' AS UUID), true, 0, NOW(), NOW()),
(CAST('44444444-4444-4444-4444-444444444452' AS UUID), 'Lẩu Ly Chua Cay', 'Lẩu ly ăn liền cay nồng ấm bụng, đầy đặn topping', 35000, '', 40, CAST('55555555-5555-5555-5555-555555555553' AS UUID), true, 0, NOW(), NOW())
ON CONFLICT DO NOTHING;
