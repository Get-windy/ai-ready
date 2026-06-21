package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerLogisticsExt;
import cn.aiedge.erp.stock.mapper.PartnerLogisticsExtMapper;
import cn.aiedge.erp.stock.service.PartnerLogisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerLogisticsServiceImpl extends ServiceImpl<PartnerLogisticsExtMapper, PartnerLogisticsExt> implements PartnerLogisticsService {

    @Override
    public PartnerLogisticsExt getByPartnerId(Long partnerId) {
        return getOne(new LambdaQueryWrapper<PartnerLogisticsExt>()
                .eq(PartnerLogisticsExt::getPartnerId, partnerId)
                .eq(PartnerLogisticsExt::getDeleted, 0));
    }

    @Override
    public IPage<PartnerLogisticsExt> getPage(String keyword, Integer pageNum, Integer pageSize) {
        Page<PartnerLogisticsExt> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<PartnerLogisticsExt> wrapper = new LambdaQueryWrapper<PartnerLogisticsExt>()
                .eq(PartnerLogisticsExt::getDeleted, 0)
                .orderByDesc(PartnerLogisticsExt::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public boolean createLogisticsExt(PartnerLogisticsExt ext) {
        if (ext.getColdChain() == null) ext.setColdChain(0);
        if (ext.getHazardous() == null) ext.setHazardous(0);
        if (ext.getVehicleCount() == null) ext.setVehicleCount(0);
        return save(ext);
    }

    @Override
    public boolean updateLogisticsExt(Long id, PartnerLogisticsExt ext) {
        ext.setId(id);
        return updateById(ext);
    }
}
