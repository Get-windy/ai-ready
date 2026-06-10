package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerTag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerTagService extends IService<PartnerTag> {
    List<Long> getTagIdsByPartner(Long partnerId);
    boolean attachTags(Long partnerId, List<Long> tagIds);
}
