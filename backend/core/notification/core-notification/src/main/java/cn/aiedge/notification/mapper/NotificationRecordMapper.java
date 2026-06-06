package cn.aiedge.notification.mapper;

import cn.aiedge.notification.entity.NotificationRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知记录 Mapper
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {

    /**
     * 根据接收者ID查询通知列表
     */
    @Select("SELECT * FROM sys_notification_record WHERE receiver_id = #{receiverId} ORDER BY create_time DESC LIMIT #{limit}")
    List<NotificationRecord> findByReceiverIdOrderByCreateTimeDesc(@Param("receiverId") Long receiverId, @Param("limit") int limit);

    /**
     * 根据发送状态查询
     */
    @Select("SELECT * FROM sys_notification_record WHERE status = #{status}")
    List<NotificationRecord> findByStatus(@Param("status") Integer status);

    /**
     * 根据接收者ID和阅读状态查询通知列表（read_status 为非持久化字段，暂按 receiver_id 查询）
     */
    @Select("SELECT * FROM sys_notification_record WHERE receiver_id = #{receiverId} ORDER BY create_time DESC LIMIT #{limit}")
    List<NotificationRecord> findByReceiverIdAndReadStatusOrderByCreateTimeDesc(@Param("receiverId") Long receiverId, @Param("readStatus") Integer readStatus, @Param("limit") int limit);

    /**
     * 统计用户通知数量
     */
    @Select("SELECT COUNT(*) FROM sys_notification_record WHERE receiver_id = #{receiverId}")
    int countByReceiverIdAndReadStatus(@Param("receiverId") Long receiverId, @Param("readStatus") Integer readStatus);

    /**
     * 批量更新某用户的记录状态
     */
    @Update("UPDATE sys_notification_record SET send_time = #{readTime} WHERE receiver_id = #{receiverId}")
    int updateReadStatusByReceiverId(@Param("receiverId") Long receiverId, @Param("readStatus") Integer readStatus, @Param("readTime") LocalDateTime readTime);

    /**
     * 查询通知记录
     */
    @Select("SELECT * FROM sys_notification_record ORDER BY create_time DESC LIMIT #{limit}")
    List<NotificationRecord> findByTemplateCodeOrderByCreateTimeDesc(@Param("templateCode") String templateCode, @Param("limit") int limit);
}
