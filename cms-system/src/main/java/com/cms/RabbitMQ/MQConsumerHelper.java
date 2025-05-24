package com.cms.RabbitMQ;

import com.cms.common.core.domain.entity.SysNotice;
import com.cms.system.service.ISysNoticeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 该工具类用于消费 RabbitMQ 中的通知消息，将消息体转换为 SysNotice 对象，然后调用通知处理逻辑（例如将通知推送给目标用户，更新通知状态等）。
 * 适用场景：用于 RabbitMQ 消费者模块中，在接收到通知公告消息后，将其反序列化为 SysNotice 对象，再调用后续业务逻辑。支持手动确认和异常时拒绝消息的重试机制。
 */
@Component
public class MQConsumerHelper implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MQConsumerHelper.class);

    @Autowired
    private ISysNoticeService noticeService; // 注入通知服务

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("接收到消息内容: {}", body);

            SysNotice notice = objectMapper.readValue(body, SysNotice.class);

            Integer priority = message.getMessageProperties().getPriority();
            if (priority != null && priority > 5) {
                logger.info("处理高优先级消息, deliveryTag: {}", deliveryTag);
                processHighPriorityNotice(notice);
            } else {
                logger.info("处理普通优先级消息, deliveryTag: {}", deliveryTag);
                processNormalPriorityNotice(notice);
            }

            channel.basicAck(deliveryTag, false);
            logger.debug("消息已确认, deliveryTag: {}", deliveryTag);
        } catch (Exception e) {
            logger.error("处理消息时发生错误, deliveryTag: {}, 错误信息: {}", deliveryTag, e.getMessage());
            handleRetryLogic(message, channel, deliveryTag);
        }
    }

    private void processHighPriorityNotice(SysNotice notice) {
        try {
            // 调用通知服务保存通知并触发异步推送
            //noticeService.sendNotice(notice);
            logger.info("高优先级消息处理完成, 公告ID: {}", notice.getNoticeId());
        } catch (Exception e) {
            logger.error("处理高优先级消息失败, 公告ID: {}, 错误信息: {}", notice.getNoticeId(), e.getMessage());
            throw e; // 抛出异常以触发重试机制
        }
    }

    private void processNormalPriorityNotice(SysNotice notice) {
        try {
            // 调用通知服务保存通知并触发异步推送
           // noticeService.sendNotice(notice);
            logger.info("普通优先级消息处理完成, 公告ID: {}", notice.getNoticeId());
        } catch (Exception e) {
            logger.error("处理普通优先级消息失败, 公告ID: {}, 错误信息: {}", notice.getNoticeId(), e.getMessage());
            throw e; // 抛出异常以触发重试机制
        }
    }

    private void handleRetryLogic(Message message, Channel channel, long deliveryTag) throws Exception {
        Integer retryCount = message.getMessageProperties().getHeader("x-retry-count");
        retryCount = (retryCount == null) ? 0 : retryCount;

        if (retryCount < 3) {
            message.getMessageProperties().setHeader("x-retry-count", retryCount + 1);
            channel.basicNack(deliveryTag, false, true);
            logger.warn("消息重新入队, deliveryTag: {}, 新重试次数: {}", deliveryTag, retryCount + 1);
        } else {
            // 记录丢弃消息的详细信息
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.error("消息已丢弃, deliveryTag: {}, 消息内容: {}", deliveryTag, body);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}