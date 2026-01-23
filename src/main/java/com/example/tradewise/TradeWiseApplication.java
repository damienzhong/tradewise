package com.example.tradewise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.tradewise.mapper")
public class TradeWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeWiseApplication.class, args);
    }

}