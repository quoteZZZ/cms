package com.cms.common.constant;

/**
 * MqConstants 定义了 RabbitMQ 相关的交换机、队列和路由键常量，
 * 用于系统中所有涉及消息发送和消费的模块统一调用，避免硬编码风险。
 *
 * 使用场景与功能说明：
 * - 统一定义消息交换机名称、队列名称和路由键，
 *   保证系统中各模块使用相同配置，便于集中管理和维护。
 */
public class MqConstants {
    public static final String COMP_EXCHANGE = "comp.exchange";

    public static final String COMP_CREATE_ROUTING_KEY = "comp.create";
    public static final String COMP_UPDATE_ROUTING_KEY = "comp.update";
    public static final String COMP_DELETE_ROUTING_KEY = "comp.delete";
    public static final String COMP_STATUS_ROUTING_KEY = "comp.status";
    public static final String COMP_DELAY_ROUTING_KEY = "comp.delay";

    public static final String COMP_CREATE_QUEUE = "comp.create.queue";
    public static final String COMP_UPDATE_QUEUE = "comp.update.queue";
    public static final String COMP_DELETE_QUEUE = "comp.delete.queue";
    public static final String COMP_STATUS_QUEUE = "comp.status.queue";
    public static final String COMP_DELAY_QUEUE = "comp.delay.queue";

    // 补全通知公告相关的常量
    public static final String NOTICE_EXCHANGE = "notice.exchange";

    public static final String NOTICE_ROUTING_KEY = "notice.routing.key";
    public static final String NOTICE_DELAY_ROUTING_KEY = "notice.delay.routing.key";

    public static final String NOTICE_QUEUE = "notice.queue";
    public static final String NOTICE_DELAY_QUEUE = "notice.delay.queue";
}