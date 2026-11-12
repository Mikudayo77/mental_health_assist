-- ============================================================
-- 心理咨询套餐种子数据(可选, 手动执行)
-- ============================================================

INSERT INTO counseling_package (name, description, duration_minutes, price, status, created_at, updated_at) VALUES
('初次咨询(30分钟)', '适合初步了解与情绪疏导', 30, 99.00, 1, NOW(), NOW()),
('深度咨询(60分钟)', '适合深入探讨心理困扰', 60, 199.00, 1, NOW(), NOW()),
('危机干预(90分钟)', '适合紧急情况下的深度干预', 90, 299.00, 1, NOW(), NOW());

-- 管理员账号创建步骤:
-- 1. 调用 POST /api/user/add 注册一个普通账号(如 username=admin)
-- 2. 执行: UPDATE `user` SET user_type = 2 WHERE username = 'admin';
-- 3. 重新登录获取新 token(角色写在 JWT 中, 旧 token 不会自动升级)
