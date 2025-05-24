package com.cms.common.exception;

/**
 * 工具类异常：：
 *
 * 该类用于在工具类中抛出异常时提供统一的异常处理机制。
 * 它继承自 {@link RuntimeException}，并支持自定义错误码和消息。
 *
 * 使用场景：
 * - 在工具类方法中，当发生不可恢复的错误时，可以抛出该异常。
 * - 示例代码：
 *   if (someCondition) {
 *       throw new UtilException("工具类操作失败", 500);
 *   }
 */
public class UtilException extends RuntimeException {
    private static final long serialVersionUID = 8247610319171014183L;

    private Integer code;

    public UtilException(Throwable e) {
        super(e.getMessage(), e);
        this.code = 500; // 默认错误码
    }

    public UtilException(String message) {
        super(message);
        this.code = 500; // 默认错误码
    }

    public UtilException(String message, Throwable throwable) {
        super(message, throwable);
        this.code = 500; // 默认错误码
    }

    public UtilException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
