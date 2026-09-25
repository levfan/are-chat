package com.smart.chat.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理：所有错误都以 ApiResponse 形式返回，前端统一解析。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldIssue(String field, String message) {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        int status = e.getCode() >= 400 && e.getCode() < 600 ? e.getCode() : 400;
        return ResponseEntity.status(status).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldIssue>>> invalid(MethodArgumentNotValidException e) {
        List<FieldIssue> issues = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldIssue(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiResponse<>(400, "参数校验失败", issues));
    }

    /**
     * 请求体不是合法 JSON / 字段类型不对：属于客户端问题，返回 400 而不是 500，
     * 避免前端只看到「服务器开小差了」而不知道是请求格式写错了。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求内容格式不正确（应为合法 JSON）"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> badParameter(Exception e) {
        log.warn("请求参数不正确：{}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求参数不正确"));
    }

    /** 访问了不存在的接口：404 而不是 500 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "接口不存在：" + e.getResourcePath()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(ApiResponse.error(413, "文件太大了，服务器快抱不动了"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
        log.error("服务器开小差了", e);
        return ResponseEntity.status(500).body(ApiResponse.error(500, "服务器开小差了，稍后再试试"));
    }
}
