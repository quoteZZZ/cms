package com.cms.RabbitMQ;

import com.cms.common.constant.MqConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 该工具类用于发送通知公告消息到 RabbitMQ，支持普通发送、优先级发送和延时发送。
 * 适用场景：适用于需要通过 RabbitMQ 发送不同类型的通知公告消息的场景。
 */
@Component
public class MQMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(MQMessagePublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送通知公告消息
     *
     * @param payload 通知公告对象（通常为 SysNotice 实体）
     */
    public void sendNoticeEvent(Object payload) {
        try {
            rabbitTemplate.convertAndSend(MqConstants.NOTICE_EXCHANGE, MqConstants.NOTICE_ROUTING_KEY, payload);
            logger.info("发送通知事件, payload: {}", payload);
        } catch (Exception e) {
            logger.error("发送通知事件失败, payload: {}", payload, e);
        }
    }

    /**
     * 发送高优先级通知公告消息
     *
     * @param payload     通知公告对象（通常为 SysNotice 实体）
     * @param priority    优先级（1-10，10为最高优先级）
     * @param delayMillis 延迟发送时间（毫秒）
     * @return
     */
    public boolean sendPriorityNotice(Object payload, int priority, long delayMillis) {
        try {
            MessagePostProcessor mpp = message -> {
                message.getMessageProperties().setPriority(priority);
                if (delayMillis > 0) {
                    message.getMessageProperties().setHeader("x-delay", delayMillis);
                }
                return message;
            };
            rabbitTemplate.convertAndSend(MqConstants.NOTICE_EXCHANGE, MqConstants.NOTICE_ROUTING_KEY, payload, mpp);
            logger.info("发送高优先级通知事件 (优先级: {}, 延迟: {} ms), payload: {}", priority, delayMillis, payload);
        } catch (Exception e) {
            logger.error("发送高优先级通知事件失败, payload: {}, 错误信息: {}", payload, e.getMessage());
        }
        return false;
    }

    /**
     * 发送延时消息
     *
     * @param payload 消息负载
     * @param delayMillis 延时时间（毫秒）
     * 使用场景：适用于延时任务、预热缓存或分散并发处理等场景。
     */
    public void sendDelayedMessage(Object payload, long delayMillis) {
        try {
            MessagePostProcessor mpp = message -> {
                message.getMessageProperties().setHeader("x-delay", delayMillis);
                return message;
            };
            rabbitTemplate.convertAndSend(MqConstants.NOTICE_EXCHANGE, MqConstants.NOTICE_DELAY_ROUTING_KEY, payload, mpp);
            logger.info("发送延时消息 ({} ms), payload: {}", delayMillis, payload);
        } catch (Exception e) {
            logger.error("发送延时消息失败, payload: {}", payload, e);
        }
    }
}