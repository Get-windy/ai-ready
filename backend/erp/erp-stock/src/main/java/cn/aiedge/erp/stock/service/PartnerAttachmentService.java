package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerAttachment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerAttachmentService extends IService<PartnerAttachment> {
    List<PartnerAttachment> getByPartnerId(Long partnerId);
}
