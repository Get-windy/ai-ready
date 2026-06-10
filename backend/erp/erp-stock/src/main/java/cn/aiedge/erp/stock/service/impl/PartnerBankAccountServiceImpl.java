package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerBankAccount;
import cn.aiedge.erp.stock.mapper.PartnerBankAccountMapper;
import cn.aiedge.erp.stock.service.PartnerBankAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerBankAccountServiceImpl extends ServiceImpl<PartnerBankAccountMapper, PartnerBankAccount>
        implements PartnerBankAccountService {

    @Override
    public List<PartnerBankAccount> getByPartnerId(Long partnerId) {
        return lambdaQuery()
                .eq(PartnerBankAccount::getPartnerId, partnerId)
                .eq(PartnerBankAccount::getDeleted, 0)
                .list();
    }
}
