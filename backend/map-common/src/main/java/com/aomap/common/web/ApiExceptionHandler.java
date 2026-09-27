package com.aomap.common.web;

import com.aomap.common.api.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> badRequest(IllegalArgumentException ex) {
        log.warn("请求参数或查询结果无法处理: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResult.fail(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<Void>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.fail(ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Void>> database(DataAccessException ex) {
        log.error("数据库操作失败", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("数据库操作失败，请确认 PostGIS 已初始化并且服务已连接 ai_open_map"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> other(Exception ex) {
        log.error("请求处理失败", ex);
        String message = ex.getMessage() == null ? "服务内部错误" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResult.fail(message));
    }
}
