package cn.aiedge.webhook.mapper;

import cn.aiedge.webhook.entity.WebhookLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookLogMapper extends BaseMapper<WebhookLog> {

    @Select("SELECT * FROM sys_webhook_log WHERE webhook_id = #{webhookId} ORDER BY trigger_time DESC LIMIT #{limit}")
    List<WebhookLog> selectByWebhook(@Param("webhookId") Long webhookId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_webhook_log WHERE webhook_id = #{webhookId} AND success = true")
    int countSuccessByWebhook(@Param("webhookId") Long webhookId);

    @Select("SELECT COUNT(*) FROM sys_webhook_log WHERE webhook_id = #{webhookId} AND success = false")
    int countFailedByWebhook(@Param("webhookId") Long webhookId);

    @Select("SELECT * FROM sys_webhook_log WHERE success = false AND retry_attempt < 3 ORDER BY trigger_time ASC LIMIT #{limit}")
    List<WebhookLog> selectPendingRetries(@Param("limit") int limit);
}