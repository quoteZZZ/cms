package com.cms.common.exception;

/**
 * 全局异常
 *
 * 该类用于在请求处理过程中封装异常信息，支持动态设置消息、错误码和详细消息。
 * 它继承自 {@link RuntimeException}，并提供了链式调用方法。
 *
 * 使用场景：
 * - 在全局异常处理器中，捕获所有异常并封装为 {@link GlobalException}。
 * - 示例代码：
 *
 *   try {
 *       // 执行业务逻辑
 *   } catch (Exception e) {
 *       throw new GlobalException("请求处理失败", 500).setDetailMessage(e.getMessage());
 *   }
 *
 */
public class GlobalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;
    private String detailMessage;
    private Integer code;

    public GlobalException() {
    }

    public GlobalException(String message) {
        this.message = message;
        this.code = 500; // 默认错误码
    }

    public GlobalException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public GlobalException(String message, Integer code, String detailMessage) {
        this.message = message;
        this.code = code;
        this.detailMessage = detailMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public GlobalException setMessage(String message) {
        this.message = message;
        return this;
    }

    public GlobalException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    public GlobalException setCode(Integer code) {
        this.code = code;
        return this;
    }
}