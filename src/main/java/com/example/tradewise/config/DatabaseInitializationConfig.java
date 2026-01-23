package com.example.tradewise.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationConfig.class);

    // 数据库表结构初始化现在通过手动执行SQL脚本完成
    // 请在首次部署时手动执行 src/main/resources/db_init.sql 脚本
}