package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerRole;
import cn.aiedge.erp.stock.mapper.PartnerRoleMapper;
import cn.aiedge.erp.stock.service.PartnerRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerRoleServiceImpl extends ServiceImpl<PartnerRoleMapper, PartnerRole> implements PartnerRoleService {

    @Override
    public List<PartnerRole> getByPartnerId(Long partnerId) {
        return list(new LambdaQueryWrapper<PartnerRole>()
                .eq(PartnerRole::getPartnerId, partnerId)
                .eq(PartnerRole::getDeleted, 0)
                .eq(PartnerRole::getStatus, "ENABLED"));
    }

    @Override
    public boolean addRole(Long partnerId, String roleType, boolean isPrimary) {
        // Check if role already exists
        if (hasRole(partnerId, roleType)) {
            return true;
        }
        PartnerRole role = new PartnerRole();
        role.setPartnerId(partnerId);
        role.setRoleType(roleType);
        role.setIsPrimary(isPrimary ? 1 : 0);
        role.setStatus("ENABLED");
        return save(role);
    }

    @Override
    public boolean removeRole(Long id) {
        return removeById(id);
    }

    @Override
    public boolean hasRole(Long partnerId, String roleType) {
        return count(new LambdaQueryWrapper<PartnerRole>()
                .eq(PartnerRole::getPartnerId, partnerId)
                .eq(PartnerRole::getRoleType, roleType)
                .eq(PartnerRole::getDeleted, 0)) > 0;
    }
}
