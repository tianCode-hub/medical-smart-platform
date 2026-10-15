package com.medical.utils.config;


import com.medical.model.common.dtos.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 公共全局异常处理器：统一捕获各业务服务的异常并返回标准 Result。
 * 服务只需在启动类扫描 com.medical.utils 即可生效。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 权限不足：@PreAuthorize 校验失败抛出（角色不匹配 / 未登录访问受保护接口）。
     * 按 HTTP 操作类型返回差异化提示，便于前端/用户理解具体受限的操作。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        log.warn("权限不足，拒绝访问：method={}, uri={}, msg={}", method, request.getRequestURI(), e.getMessage());
        String message;
        switch (method) {
            case "DELETE":
                message = "无权执行删除操作，删除功能仅管理员可用";
                break;
            case "PUT":
            case "PATCH":
                message = "无权执行修改操作，仅可修改本人信息，或由管理员操作";
                break;
            case "POST":
                message = "无权执行新增操作，新增功能仅管理员可用";
                break;
            default:
                message = "无权访问该资源";
                break;
        }
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("全局异常捕获", e);
        return Result.error(StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "操作失败");
    }
}
