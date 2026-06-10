package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductGrade;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品等级Service接口
 */
public interface ProductGradeService extends IService<ProductGrade> {

    /**
     * 获取全部等级列表(按等级数值降序)
     */
    List<ProductGrade> getActiveGrades();
}
