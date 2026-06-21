package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerAttachment;
import cn.aiedge.erp.stock.mapper.PartnerAttachmentMapper;
import cn.aiedge.erp.stock.service.PartnerAttachmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartnerAttachmentServiceImpl extends ServiceImpl<PartnerAttachmentMapper, PartnerAttachment>
        implements PartnerAttachmentService {

    @Override
    public List<PartnerAttachment> getByPartnerId(Long partnerId) {
        return list(new LambdaQueryWrapper<PartnerAttachment>()
                .eq(PartnerAttachment::getPartnerId, partnerId)
                .orderByAsc(PartnerAttachment::getSortOrder)
                .orderByDesc(PartnerAttachment::getCreateTime));
    }
}
