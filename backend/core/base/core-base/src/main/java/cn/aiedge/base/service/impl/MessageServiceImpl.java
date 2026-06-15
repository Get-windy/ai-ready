package cn.aiedge.base.service.impl;

import cn.aiedge.base.dto.UnreadCountVO;
import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.entity.SysMessageTemplate;
import cn.aiedge.base.mapper.SysMessageMapper;
import cn.aiedge.base.mapper.SysMessageTemplateMapper;
import cn.aiedge.base.service.MessageService;
import cn.aiedge.base.websocket.MessageWebSocketHandler;
import cn.aiedge.base.websocket.WebSocketResponse;
// import cn.dev33.satoken.stp.StpKit;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息通知服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements MessageService {

    private final SysMessageMapper messageMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final MessageWebSocketHandler webSocketHandler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSiteMessage(Long receiverId, String title, String content, 
                                String businessType, Long businessId) {
        SysMessage message = new SysMessage();
        message.setMsgType(2); // 站内信
        message.setTitle(title);
        message.setContent(content);
        message.setReceiverId(receiverId);
        message.setSendStatus(2); // 发送成功（站内信直接成功）
        message.setSendTime(LocalDateTime.now());
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        // message.setTenantId(StpKit.getTenantId());
        // 暂时使用固定租户ID
        message.setTenantId(1L);
        message.setRetryCount(0);

        messageMapper.insert(message);
        log.info("发送站内信: receiverId={}, title={}", receiverId, title);

        // WebSocket 实时推送
        try {
            String notifyType = businessType == null ? "SYSTEM"
                    : (businessType.contains("approval") ? "APPROVAL" : "BUSINESS");
            java.util.HashMap<String, Object> pushData = new java.util.HashMap<>();
            pushData.put("id", message.getId());
            pushData.put("type", notifyType);
            pushData.put("title", title);
            pushData.put("content", content);
            pushData.put("readStatus", 0);
            webSocketHandler.sendToUser(receiverId,
                    WebSocketResponse.success("NOTIFICATION", pushData));
        } catch (Exception e) {
            log.warn("WebSocket推送失败: receiverId={}", receiverId, e);
        }

        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSiteMessageByTemplate(Long receiverId, String templateCode, Map<String, Object> params,
                                          String businessType, Long businessId) {
        SysMessageTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new RuntimeException("消息模板不存在: " + templateCode);
        }

        String title = renderTemplate(template.getTitle(), params);
        String content = renderTemplate(template.getContent(), params);

        return sendSiteMessage(receiverId, title, content, businessType, businessId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendEmail(Long receiverId, String receiverEmail, String title, String content,
                          String businessType, Long businessId) {
        SysMessage message = new SysMessage();
        message.setMsgType(1); // 邮件
        message.setTitle(title);
        message.setContent(content);
        message.setReceiverId(receiverId);
        message.setReceiverContact(receiverEmail);
        message.setSendStatus(0); // 待发送
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        // message.setTenantId(StpKit.getTenantId());
        // 暂时使用固定租户ID
        message.setTenantId(1L);
        message.setRetryCount(0);

        messageMapper.insert(message);
        log.info("创建邮件消息: receiverId={}, email={}, title={}", receiverId, receiverEmail, title);
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendEmailByTemplate(Long receiverId, String receiverEmail, String templateCode,
                                    Map<String, Object> params, String businessType, Long businessId) {
        SysMessageTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new RuntimeException("消息模板不存在: " + templateCode);
        }

        String title = renderTemplate(template.getTitle(), params);
        String content = renderTemplate(template.getContent(), params);

        return sendEmail(receiverId, receiverEmail, title, content, businessType, businessId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSendSiteMessage(List<Long> receiverIds, String title, String content,
                                     String businessType, Long businessId) {
        for (Long receiverId : receiverIds) {
            try {
                sendSiteMessage(receiverId, title, content, businessType, businessId);
            } catch (Exception e) {
                log.error("批量发送消息失败: receiverId={}", receiverId, e);
            }
        }
    }

    @Override
    public List<SysMessage> getUnreadMessages(Long userId) {
        // return messageMapper.selectUnreadByUserId(userId, StpKit.getTenantId());
        // 暂时使用固定租户ID
        return messageMapper.selectUnreadByUserId(userId, 1L);
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        List<SysMessage> unreadList = lambdaQuery()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, 0)
                .eq(SysMessage::getMsgType, 2)
                .list();

        long total = unreadList.size();
        long system = 0, business = 0, approval = 0;

        for (SysMessage msg : unreadList) {
            String bt = msg.getBusinessType();
            if (bt == null) {
                system++;
            } else if (bt.contains("approval")) {
                approval++;
            } else {
                business++;
            }
        }

        return new UnreadCountVO(total, system, business, approval);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId) {
        messageMapper.markAsRead(messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageMapper.batchMarkAsRead(messageIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(SysMessageTemplate template) {
        // 检查模板编码是否已存在
        // SysMessageTemplate exist = templateMapper.selectByCode(template.getTemplateCode(), 
        //                                                             template.getTenantId());
        // 暂时使用固定租户ID
        SysMessageTemplate exist = templateMapper.selectByCode(template.getTemplateCode(), 1L);
        if (exist != null) {
            throw new RuntimeException("模板编码已存在: " + template.getTemplateCode());
        }

        template.setStatus(1);
        // template.setTenantId(StpKit.getTenantId());
        // 暂时使用固定租户ID
        template.setTenantId(1L);
        templateMapper.insert(template);
        log.info("创建消息模板: code={}, name={}", template.getTemplateCode(), template.getTemplateName());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(SysMessageTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        templateMapper.deleteById(templateId);
    }

    @Override
    public SysMessageTemplate getTemplateByCode(String templateCode) {
        // return templateMapper.selectByCode(templateCode, StpKit.getTenantId());
        // 暂时使用固定租户ID
        return templateMapper.selectByCode(templateCode, 1L);
    }

    @Override
    public String renderTemplate(String templateContent, Map<String, Object> params) {
        if (templateContent == null || params == null) {
            return templateContent;
        }

        String result = templateContent;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            if (value != null) {
                result = result.replace("${" + key + "}", value.toString());
            }
        }

        return result;
    }

    @Override
    public Page<SysMessage> pageMessages(Page<SysMessage> page, Long userId, Integer msgType,
                                         Integer sendStatus, String businessType) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getReceiverId, userId);
        
        if (msgType != null) {
            wrapper.eq(SysMessage::getMsgType, msgType);
        }
        if (sendStatus != null) {
            wrapper.eq(SysMessage::getSendStatus, sendStatus);
        }
        if (businessType != null) {
            wrapper.eq(SysMessage::getBusinessType, businessType);
        }
        
        // wrapper.eq(SysMessage::getTenantId, StpKit.getTenantId())
        // 暂时使用固定租户ID
        wrapper.eq(SysMessage::getTenantId, 1L)
               .orderByDesc(SysMessage::getCreateTime);

        return messageMapper.selectPage(page, wrapper);
    }
}
