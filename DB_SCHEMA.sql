-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_order_id VARCHAR(255) NOT NULL UNIQUE COMMENT '外部订单ID',
    trader_id VARCHAR(100) NOT NULL COMMENT '交易员ID',
    trader_name VARCHAR(255) NOT NULL COMMENT '交易员姓名',
    symbol VARCHAR(50) NOT NULL COMMENT '交易对',
    base_asset VARCHAR(50) NOT NULL COMMENT '基础资产',
    quote_asset VARCHAR(50) NOT NULL COMMENT '报价资产',
    side VARCHAR(20) NOT NULL COMMENT '买卖方向(BUY/SELL)',
    type VARCHAR(20) NOT NULL COMMENT '订单类型(LIMIT/MARKET)',
    position_side VARCHAR(20) NOT NULL COMMENT '持仓方向(LONG/SHORT)',
    executed_qty DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '成交数量',
    avg_price DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '平均价格',
    total_pnl DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '总盈亏',
    order_update_time BIGINT COMMENT '订单更新时间',
    order_time BIGINT NOT NULL COMMENT '订单时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_trader_id (trader_id),
    INDEX idx_order_time (order_time),
    INDEX idx_external_order_id (external_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 交易员配置表
CREATE TABLE IF NOT EXISTS trader_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trader_id VARCHAR(100) NOT NULL UNIQUE COMMENT '交易员ID',
    portfolio_id VARCHAR(255) NOT NULL COMMENT '投资组合ID',
    name VARCHAR(255) NOT NULL COMMENT '交易员姓名',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易员配置表';

-- 邮箱配置表
CREATE TABLE IF NOT EXISTS email_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱地址',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email_address (email_address),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱配置表';