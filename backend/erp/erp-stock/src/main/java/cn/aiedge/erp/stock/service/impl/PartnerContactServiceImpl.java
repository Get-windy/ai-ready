package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerContact;
import cn.aiedge.erp.stock.mapper.PartnerContactMapper;
import cn.aiedge.erp.stock.service.PartnerContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerContactServiceImpl extends ServiceImpl<PartnerContactMapper, PartnerContact>
        implements PartnerContactService {

    @Override
    public List<PartnerContact> getByPartnerId(Long partnerId) {
        return lambdaQuery()
                .eq(PartnerContact::getPartnerId, partnerId)
                .eq(PartnerContact::getDeleted, 0)
                .list();
    }
}
