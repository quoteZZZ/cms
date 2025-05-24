package com.cms.common.exception;

/**
 * 业务异常
 *
 * 该类用于在业务逻辑中抛出异常时提供统一的异常处理机制。
 * 它继承自 {@link RuntimeException}，并支持自定义错误码、详细消息和不可变设计。
 *
 * 使用场景：
 * - 在服务层方法中，当业务逻辑验证失败或发生错误时，可以抛出该异常。
 * - 示例代码：
 *
 *   if (!isValidInput) {
 *       throw new ServiceException("输入参数无效", 400, "详细错误信息");
 *   }
 *
 */
public final class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final String detailMessage;

    public ServiceException(String message) {
        super(message);
        this.code = 500; // 默认错误码
        this.detailMessage = null;
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
        this.detailMessage = null;
    }

    public ServiceException(String message, Integer code, String detailMessage) {
        super(message);
        this.code = code;
        this.detailMessage = detailMessage;
    }

    public Integer getCode() {
        return code;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
