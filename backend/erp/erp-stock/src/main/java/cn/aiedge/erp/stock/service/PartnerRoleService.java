package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerRoleService extends IService<PartnerRole> {
    List<PartnerRole> getByPartnerId(Long partnerId);
    boolean addRole(Long partnerId, String roleType, boolean isPrimary);
    boolean removeRole(Long id);
    boolean hasRole(Long partnerId, String roleType);
}
