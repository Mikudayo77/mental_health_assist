package com.example.aispring.common;

import lombok.Data;

@Data
public class Result<T> {
    private String msg;
    private String code;
    private T data;

    public static <T> Result<T> ok() {
        Result<T> result = new Result<T>();
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setCode(ResultCode.SUCCESS.getCode());
        return result;
    }
    public static <T> Result<T> ok(T data) {
        Result<T> result = ok();
        result.setData(data);
        return result;
    }
    public static <T> Result<T> error() {
        Result<T> result = new Result<>();
        result.setMsg(ResultCode.ERROR.getMsg());
        result.setCode(ResultCode.ERROR.getCode());
        return result;
    }
    public static <T> Result<T> error(String code , String msg,T data) {
        Result<T> result = new Result<>();
        result.setMsg(msg);
        result.setCode(code);
        result.setData(data);
        return result;
    }
}
