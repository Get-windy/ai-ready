package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerContact;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerContactService extends IService<PartnerContact> {
    List<PartnerContact> getByPartnerId(Long partnerId);
}
