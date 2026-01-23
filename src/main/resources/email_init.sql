-- 初始化邮箱配置表数据
-- 可以通过以下命令手动导入数据:
-- mysql -u root -p tradewise < src/main/resources/email_init.sql

INSERT INTO email_config (email_address, enabled) VALUES
('81165386@qq.com', 1),
('www.ccem@qq.com', 1),
('sjs1919@qq.com', 1);

-- 可以根据需要添加更多邮箱地址
-- INSERT INTO email_config (email_address, enabled) VALUES ('another@example.com', 1);