package cn.aiedge.notification.template;

import cn.aiedge.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板仓库接口
 * 用于管理通知模板的存储和检索
 */
@Repository
public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    /**
     * 根据模板编码查找模板
     * @param templateCode 模板编码
     * @return 模板Optional
     */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
    
    /**
     * 根据模板编码查找启用的模板
     * @param templateCode 模板编码
     * @return 模板Optional
     */
    Optional<NotificationTemplate> findByTemplateCodeAndStatus(String templateCode, Integer status);
    
    /**
     * 查找所有启用的模板
     * @return 模板列表
     */
    List<NotificationTemplate> findByStatus(Integer status);
    
    /**
     * 根据模板类型查找模板
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<NotificationTemplate> findByTemplateType(String templateType);
    
    /**
     * 根据模板类型和状态查找模板
     * @param templateType 模板类型
     * @param status 状态
     * @return 模板列表
     */
    List<NotificationTemplate> findByTemplateTypeAndStatus(String templateType, Integer status);
}
