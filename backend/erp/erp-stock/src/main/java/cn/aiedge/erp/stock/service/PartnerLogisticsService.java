package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerLogisticsExt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PartnerLogisticsService extends IService<PartnerLogisticsExt> {
    PartnerLogisticsExt getByPartnerId(Long partnerId);
    IPage<PartnerLogisticsExt> getPage(String keyword, Integer pageNum, Integer pageSize);
    boolean createLogisticsExt(PartnerLogisticsExt ext);
    boolean updateLogisticsExt(Long id, PartnerLogisticsExt ext);
}
