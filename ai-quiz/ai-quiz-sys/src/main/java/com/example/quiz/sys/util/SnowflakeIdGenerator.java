package com.example.quiz.sys.util;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;

public class SnowflakeIdGenerator {

    private static final DefaultIdentifierGenerator generator = new DefaultIdentifierGenerator();

    public static long nextId() {
        return generator.nextId(null).longValue();
    }

    /** 生成带前缀的编号，如 WD + 16位雪花ID */
    public static String nextNo(String prefix) {
        return prefix + String.valueOf(nextId()).substring(0, 16);
    }
}
