package com.aiready.notification.mapper;

import com.aiready.notification.entity.NotificationTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 消息模板Mapper接口
 */
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplate> {

    /**
     * 根据模板编码获取模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE template_code = #{templateCode} AND status = 1 AND deleted = 0")
    NotificationTemplate selectByCode(@Param("templateCode") String templateCode);
}
