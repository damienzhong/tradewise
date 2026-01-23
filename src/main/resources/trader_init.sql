-- 初始化交易员配置表数据
-- 可以通过以下命令手动导入数据:
-- mysql -u root -p tradewise < src/main/resources/trader_init.sql

INSERT INTO trader_config (trader_id, portfolio_id, name, enabled) VALUES
('trader1', '4777921357644221697', '大夫', 1),
('trader2', '4840960552873724929', '明道华光', 1);

-- 可以根据需要添加更多交易员
-- INSERT INTO trader_config (trader_id, portfolio_id, name, enabled) VALUES ('trader3', '123456789', '新交易员', 1);