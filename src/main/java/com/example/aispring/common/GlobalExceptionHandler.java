package com.example.aispring.common;

import com.example.aispring.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 参数校验异常处理
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
    }

    // 业务异常处理
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        if (e.getCode() != null) {
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    /**
     * 唯一键冲突，例如同一用户同一天重复提交情绪日记
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMostSpecificCause().getMessage());
        return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "数据已存在，请勿重复提交", null);
    }

    /**
     * 数据库异常（表不存在、字段不匹配、SQL 语法错误等）。
     * 把真实原因带给前端，否则页面只会弹一句没有信息量的"请求失败"。
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<String> handleDataAccess(DataAccessException e) {
        String cause = e.getMostSpecificCause().getMessage();
        log.error("数据库操作失败：{}", cause);
        String message = "数据库操作失败：" + (cause == null ? "未知原因" : cause);
        if (cause != null && cause.contains("doesn't exist")) {
            message = message + "（数据表不存在，请重启后端自动建表，或手动执行 sql 目录下的脚本）";
        }
        if (message.length() > 300) {
            message = message.substring(0, 300) + "...";
        }
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    /**
     * 兜底：任何没被上面接住的异常，都把原因带回前端并打印堆栈。
     * 否则前端只能看到 Spring 默认的 {"status":500,"error":"Internal Server Error"}，什么信息都没有。
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleUnexpected(Exception e) {
        log.error("未预期的异常", e);
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (message.length() > 300) {
            message = message.substring(0, 300) + "...";
        }
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "服务异常：" + message, null);
    }
}
