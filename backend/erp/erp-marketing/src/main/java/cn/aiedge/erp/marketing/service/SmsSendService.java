package cn.aiedge.erp.marketing.service;

import java.util.List;
import java.util.Map;

/**
 * 营销短信群发（**单一实现**：手工群发与营销自动化共用，合规校验不重复实现）。
 *
 * <p>合规前置（顺序固定）：① 全局发送时段 → ② 逐收件人退订名单 → ③ 频控；
 * 通过后入平台消息底座（`sys_message`），写营销台账 `mkt_sms_record` 与同意留痕 `mkt_sms_consent`。</p>
 */
public interface SmsSendService {

    /**
     * 群发
     *
     * @param tenantId   租户（自动化场景无 Sa-Token 会话，故由调用方显式传入）
     * @param partnerIds 收件客户（往来单位）id
     * @param content    短信内容（不含签名）
     * @param signName   公司签名
     * @param smsType    NOTICE 通知短信 / MARKETING 营销短信
     * @param handlerName 经手人（手工群发为当前登录人；自动化为「系统自动化」）
     * @param source     来源标记，写入台账备注（如 手工群发 / 自动化:规则名）
     * @return {batchNo, sentCount, skippedCount, skippedByCompliance, remainQuota}
     */
    Map<String, Object> sendBatch(Long tenantId, List<Long> partnerIds, String content, String signName,
                                  String smsType, String handlerName, String source);
}
