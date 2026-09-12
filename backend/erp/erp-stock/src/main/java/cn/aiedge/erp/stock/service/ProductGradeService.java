package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductGrade;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品等级Service接口
 *
 * <p>价格等级为全局标准槽位 GRADE_1..GRADE_8，昵称（gradeName）由用户自定义，
 * 读写均需忽略多租户过滤（种子数据 tenant_id=0）。</p>
 */
public interface ProductGradeService extends IService<ProductGrade> {

    /** 获取启用的价格等级（按 sortOrder/gradeLevel 升序，槽位顺序 = GRADE_1..8） */
    List<ProductGrade> getActiveGrades();

    /** 获取全部价格等级（含停用，忽略租户） */
    List<ProductGrade> getAllGrades();

    /** 更新价格等级（昵称/排序/状态等，忽略租户） */
    boolean updateGrade(ProductGrade grade);

    /** 删除价格等级（逻辑删除，忽略租户） */
    boolean deleteGrade(Long id);
}
