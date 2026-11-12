package com.example.aispring.common;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResult<T> {
    private final long total;
    private final List<T> records;

    private PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}
