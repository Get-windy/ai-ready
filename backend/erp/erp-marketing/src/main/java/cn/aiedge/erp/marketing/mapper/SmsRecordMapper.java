package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.SmsHistoryRow;
import cn.aiedge.erp.marketing.entity.SmsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SmsRecordMapper extends BaseMapper<SmsRecord> {

    /**
     * 短信历史（对标 7 列）。投递状态实时取 sys_message（单一真源）：
     * sys_message.send_status: 0 待发送 / 1 发送成功 / 2 发送失败（平台消息底座口径）
     */
    @Select("""
            <script>
            SELECT r.id,
                   r.receiver_name                        AS receiver_name,
                   r.mobile                               AS mobile,
                   COALESCE(m.send_time, r.create_time)   AS send_time,
                   r.handler_name                         AS handler_name,
                   CASE WHEN r.sign_name IS NULL OR r.sign_name = '' THEN r.content
                        ELSE '【' || r.sign_name || '】' || r.content END AS content,
                   CASE m.send_status WHEN 1 THEN '发送成功' WHEN 2 THEN '发送失败' ELSE '待发送' END AS send_status,
                   COALESCE(m.fail_reason, '')            AS fail_reason,
                   r.batch_no                             AS batch_no,
                   r.sms_type                             AS sms_type
            FROM mkt_sms_record r
            LEFT JOIN sys_message m ON m.id = r.message_id
            WHERE r.deleted = 0
              AND r.tenant_id = #{tenantId}
              <if test="receiver != null and receiver != ''"> AND r.receiver_name LIKE CONCAT('%', #{receiver}, '%') </if>
              <if test="mobile != null and mobile != ''"> AND r.mobile LIKE CONCAT('%', #{mobile}, '%') </if>
              <if test="smsType != null and smsType != ''"> AND r.sms_type = #{smsType} </if>
              <if test="sendStatus != null and sendStatus != ''">
                AND (CASE m.send_status WHEN 1 THEN 'SENT' WHEN 2 THEN 'FAILED' ELSE 'PENDING' END) = #{sendStatus}
              </if>
            ORDER BY r.create_time DESC NULLS LAST, r.id DESC
            </script>
            """)
    IPage<SmsHistoryRow> selectHistoryPage(Page<SmsHistoryRow> page,
                                           @Param("tenantId") Long tenantId,
                                           @Param("receiver") String receiver,
                                           @Param("mobile") String mobile,
                                           @Param("smsType") String smsType,
                                           @Param("sendStatus") String sendStatus);
}
