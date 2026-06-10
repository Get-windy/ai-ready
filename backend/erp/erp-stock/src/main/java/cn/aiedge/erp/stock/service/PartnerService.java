package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Partner;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerService extends IService<Partner> {
    IPage<Partner> getPartnerPage(String keyword, String partnerType, String status, Long categoryId, Integer pageNum, Integer pageSize);
    Partner getPartnerDetail(Long id);
    boolean createPartner(Partner partner);
    boolean updatePartner(Partner partner);
    List<Partner> search(String keyword, String partnerType);
}
