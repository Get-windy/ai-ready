package cn.aiedge.erp.stock.dto;

import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductRecommend;
import cn.aiedge.erp.stock.entity.ProductUnit;
import lombok.Data;

import java.util.List;

/**
 * 产品创建/编辑表单DTO
 * 包含产品主表 + 单位列表 + 推荐商品列表
 */
@Data
public class ProductFormDTO {

    private Product product;

    private List<ProductUnit> units;

    private List<ProductRecommend> recommends;
}
