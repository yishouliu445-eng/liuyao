package com.yishou.liuyao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LiuyaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiuyaoApplication.class, args);
    }
}
