package com.tgfc.som;

import org.springframework.boot.SpringApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SOM (Special Order Management) Application
 * 特殊訂單管理系統主程式入口
 */
@SpringBootApplication
@MapperScan("com.tgfc.som.mapper")
public class SomApplication {

    public static void main(String[] args) {
        SpringApplication.run(SomApplication.class, args);
    }
}
