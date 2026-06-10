package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerGradeProductPrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerGradeProductPriceService extends IService<PartnerGradeProductPrice> {
    List<PartnerGradeProductPrice> getByPartnerGradeId(Long partnerGradeId);
    boolean batchSave(Long partnerGradeId, List<PartnerGradeProductPrice> list);
}
