package cn.aiedge.notification.mapper;

import cn.aiedge.notification.entity.NotificationTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板 Mapper
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Mapper
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplate> {

    /**
     * 根据模板编码查找模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE template_code = #{templateCode}")
    Optional<NotificationTemplate> findByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 根据模板编码查找启用的模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE template_code = #{templateCode} AND enabled = #{status}")
    Optional<NotificationTemplate> findByTemplateCodeAndStatus(@Param("templateCode") String templateCode, @Param("status") Integer status);

    /**
     * 查找所有启用的模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE enabled = #{status}")
    List<NotificationTemplate> findByStatus(@Param("status") Integer status);

    /**
     * 根据模板类型查找模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE template_type = #{templateType}")
    List<NotificationTemplate> findByTemplateType(@Param("templateType") String templateType);

    /**
     * 根据模板类型和状态查找模板
     */
    @Select("SELECT * FROM sys_notification_template WHERE template_type = #{templateType} AND status = #{status}")
    List<NotificationTemplate> findByTemplateTypeAndStatus(@Param("templateType") String templateType, @Param("status") Integer status);
}
