INSERT INTO users (id, email, password_hash, full_name, role, is_active)
VALUES 
(gen_random_uuid(), 'admin@7eleven.vn', '$2a$12$XcHhNi2uBeg5g6h5l4OSvef5f8DMuq2GH4woDBN.Cy1y5Iiy9VUz2', 'Admin', 'ADMIN', true),
(gen_random_uuid(), 'user@7eleven.vn', '$2a$12$UIR.GdTJ0KkrskF7eel6pe8jlMu8.b6zs09C68hRFn.IPLr1PVoQS', 'Nguyen Van A', 'USER', true)
ON CONFLICT (email) DO NOTHING;
