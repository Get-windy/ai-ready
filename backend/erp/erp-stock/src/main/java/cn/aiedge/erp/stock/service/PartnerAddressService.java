package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerAddressService extends IService<PartnerAddress> {
    List<PartnerAddress> getByPartnerId(Long partnerId);
    boolean setDefault(Long id, Long partnerId);
}
