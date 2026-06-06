package cn.aiedge.base.service;

import cn.aiedge.base.dto.UnreadCountVO;
import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.entity.SysMessageTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 消息通知服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface MessageService extends IService<SysMessage> {

    /**
     * 发送站内信
     */
    Long sendSiteMessage(Long receiverId, String title, String content, String businessType, Long businessId);

    /**
     * 使用模板发送站内信
     */
    Long sendSiteMessageByTemplate(Long receiverId, String templateCode, Map<String, Object> params,
                                    String businessType, Long businessId);

    /**
     * 发送邮件
     */
    Long sendEmail(Long receiverId, String receiverEmail, String title, String content,
                   String businessType, Long businessId);

    /**
     * 使用模板发送邮件
     */
    Long sendEmailByTemplate(Long receiverId, String receiverEmail, String templateCode,
                             Map<String, Object> params, String businessType, Long businessId);

    /**
     * 批量发送消息
     */
    void batchSendSiteMessage(List<Long> receiverIds, String title, String content,
                              String businessType, Long businessId);

    /**
     * 获取用户的未读消息
     */
    List<SysMessage> getUnreadMessages(Long userId);

    /**
     * 获取未读数量统计（按通知类型分类）
     */
    UnreadCountVO getUnreadCount(Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long messageId);

    /**
     * 批量标记已读
     */
    void batchMarkAsRead(List<Long> messageIds);

    /**
     * 创建消息模板
     */
    Long createTemplate(SysMessageTemplate template);

    /**
     * 更新消息模板
     */
    void updateTemplate(SysMessageTemplate template);

    /**
     * 删除消息模板
     */
    void deleteTemplate(Long templateId);

    /**
     * 根据编码获取模板
     */
    SysMessageTemplate getTemplateByCode(String templateCode);

    /**
     * 渲染模板内容
     */
    String renderTemplate(String templateContent, Map<String, Object> params);

    /**
     * 分页查询消息
     */
    Page<SysMessage> pageMessages(Page<SysMessage> page, Long userId, Integer msgType,
                                  Integer sendStatus, String businessType);
}
