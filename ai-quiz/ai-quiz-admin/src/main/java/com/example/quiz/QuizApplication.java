package com.example.quiz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.quiz",
    "com.example.quiz.sys",
    "com.example.quiz.ai",
    "com.example.quiz.dome"
})
@MapperScan(basePackages = {
    "com.example.quiz.sys.mapper",
    "com.example.quiz.ai.mapper",
    "com.example.quiz.dome.mapper"
})
public class QuizApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuizApplication.class, args);
    }
}
