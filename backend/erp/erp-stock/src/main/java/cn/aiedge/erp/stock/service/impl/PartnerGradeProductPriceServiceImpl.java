package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerGradeProductPrice;
import cn.aiedge.erp.stock.mapper.PartnerGradeProductPriceMapper;
import cn.aiedge.erp.stock.service.PartnerGradeProductPriceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartnerGradeProductPriceServiceImpl extends ServiceImpl<PartnerGradeProductPriceMapper, PartnerGradeProductPrice>
        implements PartnerGradeProductPriceService {

    @Override
    public List<PartnerGradeProductPrice> getByPartnerGradeId(Long partnerGradeId) {
        return lambdaQuery()
                .eq(PartnerGradeProductPrice::getPartnerGradeId, partnerGradeId)
                .eq(PartnerGradeProductPrice::getDeleted, 0)
                .eq(PartnerGradeProductPrice::getIsActive, 1)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(Long partnerGradeId, List<PartnerGradeProductPrice> list) {
        // 删除旧的
        lambdaUpdate()
                .eq(PartnerGradeProductPrice::getPartnerGradeId, partnerGradeId)
                .remove();
        // 插入新的
        if (!list.isEmpty()) {
            list.forEach(p -> p.setPartnerGradeId(partnerGradeId));
            return saveBatch(list);
        }
        return true;
    }
}
