# TradeWise 跟单订单监控系统

这是一个Spring Boot应用程序，用于监控币安跟单交易员的订单，并在有新订单时通过邮件通知。

## 功能特性

1. 每分钟定时调用币安API获取指定交易员的历史订单列表
2. 检测当天是否有新增订单
3. 如果有新增订单，则通过邮件发送通知
4. 支持同时监控多个交易员
5. 支持向多个邮箱地址发送通知
6. 邮件模板中显示中文交易术语
7. 基于技术指标（EMA、RSI、布林带等）自动分析市场行情并生成交易信号
8. 支持多种K线周期的行情分析（如15分钟、30分钟、1小时等）
9. 可配置的功能开关，支持独立控制跟单信号和行情信号功能
10. 多技术指标确认机制，提高信号准确性
11. 基于ATR的风险管理，提供止损止盈建议
12. 信号置信度评估，帮助用户判断信号质量

## 技术栈

- Spring Boot 2.7.18
- Java 8+
- MyBatis
- MySQL Database
- Spring Mail
- Thymeleaf (邮件模板)

## 配置要求

### 1. 邮箱配置

在 `application.yml` 文件中配置邮箱信息。以下是常见邮箱服务商的配置示例：

#### Gmail
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # 注意：使用应用专用密码，而非账户登录密码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### QQ邮箱
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-email@qq.com
    password: your-authorization-code  # QQ邮箱授权码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 163邮箱
```yaml
spring:
  mail:
    host: smtp.163.com
    port: 25
    username: your-email@163.com
    password: your-authorization-code  # 163邮箱授权码
    properties:
      mail:
        smtp:
          auth: true
```

### 2. 交易员配置

交易员配置现在存储在数据库中，支持动态添加和删除，无需重启服务。

#### 2.1 通过SQL初始化交易员配置

可以通过执行以下SQL语句来添加初始交易员配置：

```sql
INSERT INTO trader_config (trader_id, portfolio_id, name, enabled) VALUES
('trader1', '4777921357644221697', '大夫', 1),
('trader2', '4840960552873724929', '明道华光', 1);
```

#### 2.2 通过API管理交易员配置

项目提供了REST API来动态管理交易员配置：

- `GET /api/trader-config/list` - 获取所有启用的交易员配置
- `POST /api/trader-config/add` - 添加交易员配置
- `PUT /api/trader-config/update/{id}` - 更新交易员配置
- `DELETE /api/trader-config/delete/{traderId}` - 删除交易员配置

**示例：添加交易员配置**

```bash
curl -X POST http://localhost:8080/api/trader-config/add \
  -H "Content-Type: application/json" \
  -d '{"traderId":"trader3","portfolioId":"123456789","name":"新交易员","enabled":true}'
```

**示例：获取交易员配置列表**

```bash
curl http://localhost:8080/api/trader-config/list
```

系统运行后将完全使用数据库中的配置。配置文件中只需要保留基础配置：

```yaml
tradewise:
  # 邮箱配置
  email:
    from: ${spring.mail.username}  # 发件人邮箱，使用spring.mail.username的值
    # 收件人邮箱地址现在从数据库表 email_config 中读取
    
  # 交易员配置现在完全从数据库表 trader_config 中读取
  # 配置文件中的traders部分不再使用
```

### 3. 邮箱配置

邮箱地址现在存储在数据库中，支持动态添加和删除，无需重启服务。

#### 3.1 通过SQL初始化邮箱配置

可以通过执行以下SQL语句来添加初始邮箱配置：

```sql
INSERT INTO email_config (email_address, enabled) VALUES
('your-email@example.com', 1),
('another-email@example.com', 1);
```

也可以运行项目提供的初始化脚本：

```bash
mysql -u root -p tradewise < src/main/resources/email_init.sql
```

#### 3.2 通过API管理邮箱配置

项目提供了REST API来动态管理邮箱配置：

- `GET /api/email-config/list` - 获取所有启用的邮箱配置
- `POST /api/email-config/add` - 添加邮箱配置
- `PUT /api/email-config/update/{id}` - 更新邮箱配置
- `DELETE /api/email-config/delete/{id}` - 删除邮箱配置

**示例：添加邮箱配置**

```bash
curl -X POST http://localhost:8080/api/email-config/add \
  -H "Content-Type: application/json" \
  -d '{"emailAddress":"new-user@example.com","enabled":true}'
```

**示例：获取邮箱配置列表**

```bash
curl http://localhost:8080/api/email-config/list
```

## 如何运行

### 方法1：使用Maven (推荐)

```bash
# 克隆或下载项目后进入目录
cd tradewise

# 编译并运行
mvn spring-boot:run
```

### 方法2：使用IDE

1. 导入项目到IDE（如IntelliJ IDEA或Eclipse）
2. 确保Maven依赖正确下载
3. 运行 [TradeWiseApplication](file:///D:/workspace/tradewise/src/main/java/com/example/tradewise/TradeWiseApplication.java#L7-L15) 类

### 方法3：打包后运行

```bash
# 打包项目
mvn clean package

# 运行jar文件（需要先替换配置文件中的占位符）
java -jar target/tradewise-0.0.1-SNAPSHOT.jar
```

## 代理配置说明

如果遇到 `UnknownHostException: www.binance.com` 错误，在中国大陆等地区可能需要配置代理才能访问币安API。

### 启动时配置代理

```bash
# 无认证代理
java -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -jar target/tradewise-0.0.1-SNAPSHOT.jar

# 带认证代理
java -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyUser=username -Dhttp.proxyPassword=password -jar target/tradewise-0.0.1-SNAPSHOT.jar
```

### 在IDE中配置代理

如果您在IDE中运行，可在运行配置中添加以下JVM参数：
- `-Dhttp.proxyHost=127.0.0.1`
- `-Dhttp.proxyPort=7890`
- `-Dhttps.proxyHost=127.0.0.1`
- `-Dhttps.proxyPort=7890`

### 对于Clash用户

Clash默认代理端口为7890，因此可以使用上述参数，将7890替换为Clash的实际代理端口。

## API端点

### 订单监控相关
- `GET /api/orders/health` - 检查服务健康状态
- `GET /api/orders/check` - 手动触发订单检查（当前仅为占位符）

### 交易员配置相关
- `GET /api/trader-config/list` - 获取所有启用的交易员配置
- `POST /api/trader-config/add` - 添加交易员配置
- `PUT /api/trader-config/update/{id}` - 更新交易员配置
- `DELETE /api/trader-config/delete/{traderId}` - 删除交易员配置

### 邮箱配置相关
- `GET /api/email-config/list` - 获取所有启用的邮箱配置
- `POST /api/email-config/add` - 添加邮箱配置
- `PUT /api/email-config/update/{id}` - 更新邮箱配置
- `DELETE /api/email-config/delete/{id}` - 删除邮箱配置

### 市场分析相关
- `POST /api/market-analysis/analyze/{symbol}` - 手动触发特定交易对的市场分析
- `GET /api/market-analysis/health` - 检查市场分析服务健康状态

### 功能开关相关
- `GET /api/features/status` - 获取当前所有功能状态
- `POST /api/features/market-analysis/toggle?enabled=true` - 启用/禁用市场分析功能
- `POST /api/features/copy-trading/toggle?enabled=true` - 启用/禁用交易员跟单功能
- `GET /api/features/market-analysis/status` - 获取市场分析功能状态
- `GET /api/features/copy-trading/status` - 获取交易员跟单功能状态

## 数据库

项目使用MySQL数据库，配置如下：
- URL: jdbc:mysql://47.121.28.79:3306/tradewise
- 用户名: root
- 密码: damien2code.

### 数据库初始化

首次运行项目前，需要初始化数据库表结构和初始数据。有两种方式：

#### 方式1：使用完整初始化脚本（推荐）

```bash
mysql -u root -p tradewise < src/main/resources/db_init.sql
```

#### 方式2：分别初始化表结构和数据

```bash
# 首先创建表结构（只需执行一次）
mysql -u root -p tradewise < DB_SCHEMA.sql

# 然后添加初始数据
mysql -u root -p tradewise < src/main/resources/trader_init.sql
mysql -u root -p tradewise < src/main/resources/email_init.sql
```

## 邮件模板

### 跟单信号模板

跟单信号邮件模板位于 `src/main/resources/templates/order-notification-template.html`，使用中文显示交易术语，包括：

- **方向**: BUY → 买入, SELL → 卖出
- **类型**: LIMIT → 限价单, MARKET → 市价单
- **仓位**: LONG → 多仓, SHORT → 空仓

### 市场行情信号模板

市场行情信号邮件模板位于 `src/main/resources/templates/market-signal-template.html`，专门用于显示技术指标分析结果，包括：

- **信号类型**: BUY → 买入信号, SELL → 卖出信号
- **指标名称**: 显示触发信号的技术指标名称（如EMA金叉、RSI超卖等）
- **信号描述**: 详细的信号生成原因说明

您可以根据需要自定义样式和内容。

## 安全注意事项

1. 不要在代码中硬编码敏感信息如邮箱密码
2. 使用应用专用密码而不是账户登录密码
3. 在生产环境中使用更安全的数据库解决方案
4. 对于Gmail用户，需要开启"两步验证"并使用"应用专用密码"
5. 对于QQ邮箱用户，需要获取"授权码"而非账户密码

## 功能开关

系统提供了灵活的功能开关，允许您动态控制不同功能的启用/禁用状态：

### 1. 配置文件开关

在 `application.yml` 中可以通过以下配置控制功能：

```yaml
tradewise:
  # 市场分析功能开关
  market-analysis:
    enabled: true  # 设置为false可禁用市场分析功能
    symbols-to-monitor:  # 要监控的交易对
      - BTCUSDT
      - ETHUSDT
      - BNBUSDT
      - SOLUSDT
      - XRPUSDT
    interval: 1h  # K线周期
    limit: 200    # 获取K线的数量
    analysis-interval-minutes: 15  # 分析间隔（分钟），与定时器cron表达式保持一致
    # 技术指标参数
    ema-short-period: 12  # 短期EMA周期
    ema-long-period: 26   # 长期EMA周期
    rsi-period: 14        # RSI周期
    bb-period: 20         # 布林带周期
    bb-std-multiplier: 2.0  # 布林带标准差倍数
    atr-period: 14        # ATR周期
    min-volume-ratio: 1.2 # 最小成交量比率
    trend-strength-period: 50  # 趋势强度计算周期
    signal-confirmation-threshold: 2  # 信号确认阈值，至少需要多少个指标确认（1=宽松,2=适中,3=严格）
  # 交易员跟单功能开关
  copy-trading:
    enabled: true  # 设置为false可禁用交易员跟单功能
```

### 2. API开关

系统还提供了API端点来动态控制功能开关：

- `GET /api/features/status` - 获取当前所有功能状态
- `POST /api/features/market-analysis/toggle?enabled=true` - 启用/禁用市场分析功能
- `POST /api/features/copy-trading/toggle?enabled=true` - 启用/禁用交易员跟单功能
- `GET /api/features/market-analysis/status` - 获取市场分析功能状态
- `GET /api/features/copy-trading/status` - 获取交易员跟单功能状态

**示例：禁用市场分析功能**

```bash
curl -X POST http://localhost:8080/api/features/market-analysis/toggle?enabled=false
```

**示例：获取当前功能状态**

```bash
curl http://localhost:8080/api/features/status
```

## 维护和扩展

- 定时任务配置在 [OrderMonitoringScheduler](file:///D:/workspace/tradewise/src/main/java/com/example/tradewise/scheduler/OrderMonitoringScheduler.java#L9-L58) 类中
- API调用逻辑在 [OrderService](file:///D:/workspace/tradewise/src/main/java/com/example/tradewise/service/OrderService.java#L14-L201) 类中
- 邮件发送逻辑在 [EmailService](file:///D:/workspace/tradewise/src/main/java/com/example/tradewise/service/EmailService.java#L13-L70) 类中
- 功能开关控制在 [FeatureToggleController](file:///D:/workspace/tradewise/src/main/java/com/example/tradewise/controller/FeatureToggleController.java#L15-L86) 类中