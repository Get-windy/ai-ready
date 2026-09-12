package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.PartyAttachment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface PartyAttachmentService extends IService<PartyAttachment> {

    /** 查询某往来单位的附件列表 */
    List<PartyAttachment> listByPartnerId(Long partnerId);

    /** 批量统计附件数量：partnerId → count */
    Map<Long, Integer> countByPartnerIds(List<Long> partnerIds);
}
