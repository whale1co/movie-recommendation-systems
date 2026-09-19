package com.movierec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.movierec.mapper")
public class MovieRecApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieRecApplication.class, args);
    }
}
