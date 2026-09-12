package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.CloudProduct;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 云商品库 Service
 */
public interface CloudProductService extends IService<CloudProduct> {

    IPage<CloudProduct> getCloudPage(String keyword, String industryCategory, Integer pageNum, Integer pageSize);

    /**
     * 云导入：把云商品库条目导入为本地商品（按 categoryId 归类，单位按 unit 生成基本单位）
     *
     * @return { imported: 导入条数, skipped: 已存在跳过条数 }
     */
    Map<String, Object> cloudImport(List<Long> cloudIds, Long categoryId);
}
