package com.securefile;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 安全文件传输系统启动类
 */
@SpringBootApplication
@MapperScan("com.securefile.mapper")
public class SecureFileApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureFileApplication.class, args);
    }
}
