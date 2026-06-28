package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ProductCostStandard;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

public interface ICostService {

    Page<ProductCostStandard> pageCost(Integer pageNum, Integer pageSize, String productName, String batchNo);

    Map<String, Object> getStats();
}
