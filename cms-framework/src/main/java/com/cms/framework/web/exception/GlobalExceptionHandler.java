package com.cms.framework.web.exception;

import com.cms.common.constant.HttpStatus;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.text.Convert;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.html.EscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * <p>
 * 用于捕获并处理Spring MVC框架中抛出的各种异常，提供统一的错误响应格式。
 * </p>
 * <p>
 * 主要功能：
 * - 捕获并处理权限校验异常、HTTP方法不支持异常、业务异常等。
 * - 返回统一格式的错误响应，便于前端解析。
 * </p>
 */
@RestControllerAdvice // 注解：定义一个控制器增强器，用于全局处理异常
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理权限校验异常
     * <p>
     * 当用户尝试访问无权限的资源时触发。
     * 返回状态码403（Forbidden），并附带错误信息。
     * </p>
     *
     * @param e       权限校验异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(AccessDeniedException.class) // 注解：指定该方法用于处理AccessDeniedException异常
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    /**
     * 处理HTTP方法不支持异常
     * <p>
     * 当客户端使用了不支持的HTTP方法时触发。
     * 返回状态码405（Method Not Allowed），并附带错误信息。
     * </p>
     *
     * @param e       HTTP方法不支持异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 处理业务异常
     * <p>
     * 当服务层抛出 {@link ServiceException} 时触发。
     * 返回状态码根据异常中的code字段决定，默认为500。
     * </p>
     *
     * @param e       业务异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(ServiceException.class) // 注解：指定该方法用于处理ServiceException异常
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    /**
     * 处理缺少必需的路径变量异常
     * <p>
     * 当请求路径中缺少必需的路径变量时触发。
     * 返回状态码400（Bad Request），并附带错误信息。
     * </p>
     *
     * @param e       缺少必需的路径变量异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 处理请求参数类型不匹配异常
     * <p>
     * 当请求参数类型与方法签名不匹配时触发。
     * 返回状态码400（Bad Request），并附带详细的错误信息。
     * </p>
     *
     * @param e       请求参数类型不匹配异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value)) {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    /**
     * 拦截未知的运行时异常
     * <p>
     * 当捕获到未预期的运行时异常时触发。
     * 返回状态码500（Internal Server Error），并附带错误信息。
     * </p>
     *
     * @param e       运行时异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(RuntimeException.class) // 注解：指定该方法用于处理RuntimeException及其子类异常
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 处理系统异常
     * <p>
     * 当捕获到任何未处理的异常时触发。
     * 返回状态码500（Internal Server Error），并附带错误信息。
     * </p>
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 处理Spring Validation校验失败异常
     * <p>
     * 当Spring Validation校验失败时触发。
     * 返回状态码400（Bad Request），并附带第一个错误信息。
     * </p>
     *
     * @param e 绑定异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 处理Spring Validation校验失败异常
     * <p>
     * 当Spring Validation校验失败时触发。
     * 返回状态码400（Bad Request），并附带第一个错误信息。
     * </p>
     *
     * @param e 方法参数验证异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }
}