package com.example.tradewise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.example.tradewise", "com.damien.tradewise"})
@EnableScheduling
@MapperScan({"com.example.tradewise.mapper", "com.damien.tradewise.admin.mapper", "com.damien.tradewise.user.mapper", "com.damien.tradewise.common.mapper"})
public class TradeWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeWiseApplication.class, args);
    }

}
