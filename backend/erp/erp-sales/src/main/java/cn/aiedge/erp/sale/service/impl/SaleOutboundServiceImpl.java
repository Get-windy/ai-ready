package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.entity.SaleOutbound;
import cn.aiedge.erp.sale.mapper.SaleOutboundMapper;
import cn.aiedge.erp.sale.service.ISaleOutboundService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 销售出库单服务实现
 */
@Service
public class SaleOutboundServiceImpl extends ServiceImpl<SaleOutboundMapper, SaleOutbound> implements ISaleOutboundService {
}