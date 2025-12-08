-- 這是 H2 初始化資料，用於本地開發測試
-- 請確保欄位對應 schema.sql

INSERT INTO users (username, email) VALUES ('AdminUser', 'admin@example.com');
INSERT INTO users (username, email) VALUES ('TestUser1', 'user1@test.com');
INSERT INTO users (username, email) VALUES ('DemoUser', 'demo@demo.com');

-- 如果有其他關聯表資料，也可以在這裡插入
-- INSERT INTO orders (user_id, total) VALUES (1, 100);
