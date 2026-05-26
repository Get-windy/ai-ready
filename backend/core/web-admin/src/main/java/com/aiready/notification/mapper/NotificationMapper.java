package com.aiready.notification.mapper;

import com.aiready.notification.entity.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息通知Mapper接口
 */
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 获取用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_notification WHERE user_id = #{userId} AND read_status = 0 AND deleted = 0")
    Long selectUnreadCount(@Param("userId") Long userId);

    /**
     * 标记消息为已读
     */
    @Update("UPDATE sys_notification SET read_status = 1, read_time = NOW() WHERE id = #{id}")
    int markAsRead(@Param("id") Long id);

    /**
     * 批量标记消息为已读
     */
    @Update("<script>UPDATE sys_notification SET read_status = 1, read_time = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int markAsReadBatch(@Param("ids") List<Long> ids);

    /**
     * 标记用户所有消息为已读
     */
    @Update("UPDATE sys_notification SET read_status = 1, read_time = NOW() WHERE user_id = #{userId} AND read_status = 0")
    int markAllAsRead(@Param("userId") Long userId);
}
