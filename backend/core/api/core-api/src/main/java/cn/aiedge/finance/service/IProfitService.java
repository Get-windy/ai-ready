package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ProfitAnalysis;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

public interface IProfitService {

    Page<ProfitAnalysis> pageProfit(Integer pageNum, Integer pageSize, String productName, String startDate, String endDate);

    Map<String, Object> getStats();
}
