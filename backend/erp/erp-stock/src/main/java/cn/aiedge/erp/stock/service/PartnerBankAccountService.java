package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerBankAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerBankAccountService extends IService<PartnerBankAccount> {
    List<PartnerBankAccount> getByPartnerId(Long partnerId);
}
