package cn.aiedge.erp.partner.service;

import cn.aiedge.erp.partner.entity.Partner;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPartnerService extends IService<Partner> {
    IPage<Partner> getPartnerPage(String keyword, String partnerType, String status, Long categoryId, Integer pageNum, Integer pageSize);
    Partner getPartnerDetail(Long id);
    boolean createPartner(Partner partner);
    boolean createPartnerWithRoles(Partner partner, List<String> roles);
    boolean updatePartner(Partner partner);
    List<Partner> search(String keyword, String partnerType);
    List<Partner> getPartnerList(String partnerType, String status, Integer pageSize);
    Integer getNextSeq(String prefix);

    // 客户匹配相关方法
    Partner findByTaxNo(String taxNo);
    List<Partner> findByName(String partnerName);
    List<Partner> findByContactPhone(String contactPhone);
    List<Partner> findByNameAndAddress(String partnerName, String address);
}