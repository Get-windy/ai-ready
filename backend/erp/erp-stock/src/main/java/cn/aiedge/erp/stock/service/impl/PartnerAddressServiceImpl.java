package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerAddress;
import cn.aiedge.erp.stock.mapper.PartnerAddressMapper;
import cn.aiedge.erp.stock.service.PartnerAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartnerAddressServiceImpl extends ServiceImpl<PartnerAddressMapper, PartnerAddress>
        implements PartnerAddressService {

    @Override
    public List<PartnerAddress> getByPartnerId(Long partnerId) {
        return lambdaQuery()
                .eq(PartnerAddress::getPartnerId, partnerId)
                .eq(PartnerAddress::getDeleted, 0)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id, Long partnerId) {
        // 清除其他的默认
        lambdaUpdate()
                .eq(PartnerAddress::getPartnerId, partnerId)
                .eq(PartnerAddress::getIsDefault, 1)
                .set(PartnerAddress::getIsDefault, 0)
                .update();
        // 设置当前为默认
        return lambdaUpdate()
                .eq(PartnerAddress::getId, id)
                .set(PartnerAddress::getIsDefault, 1)
                .update();
    }
}
