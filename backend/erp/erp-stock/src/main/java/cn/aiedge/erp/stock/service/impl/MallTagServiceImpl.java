package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.MallTag;
import cn.aiedge.erp.stock.mapper.MallTagMapper;
import cn.aiedge.erp.stock.service.MallTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商城标签ServiceImpl
 */
@Slf4j
@Service
public class MallTagServiceImpl extends ServiceImpl<MallTagMapper, MallTag> implements MallTagService {

    @Override
    public List<MallTag> getByTenantId(Long tenantId) {
        return baseMapper.selectByTenantId(tenantId);
    }
}
