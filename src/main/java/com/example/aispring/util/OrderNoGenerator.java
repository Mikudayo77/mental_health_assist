package com.example.aispring.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;

/**
 * 业务单号生成器：前缀 + 毫秒时间戳 + 4位随机数（20位）
 * 数据库唯一索引兜底，重复概率可忽略
 */
public class OrderNoGenerator {

    public static String gen(String prefix) {
        return prefix
                + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
                + RandomUtil.randomNumbers(4);
    }

    private OrderNoGenerator() {
    }
}
