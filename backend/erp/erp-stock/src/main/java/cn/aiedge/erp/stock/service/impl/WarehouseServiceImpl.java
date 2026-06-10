package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    @Override
    public List<Warehouse> getWarehouseList() {
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }
}
