package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerCategoryService extends IService<PartnerCategory> {
    List<PartnerCategory> getTree(String categoryType);
}
