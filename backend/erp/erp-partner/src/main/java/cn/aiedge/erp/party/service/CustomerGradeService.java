package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.CustomerGrade;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerGradeService extends IService<CustomerGrade> {

    CustomerGrade getByGradeCode(String gradeCode);

    CustomerGrade getByGradeLevel(Integer gradeLevel);

    List<CustomerGrade> listActiveGrades();

    CustomerGrade getGradeByAmount(BigDecimal amount);

    CustomerGrade getNextGrade(Long currentGradeId);

    BigDecimal calculateAmountToNextGrade(Long currentGradeId, BigDecimal currentAmount);

    boolean checkGradeCodeExists(String gradeCode);

    boolean checkGradeCodeExists(String gradeCode, Long excludeId);

    /** 正在使用该级别名称的往来单位数（删除前的引用保护） */
    long countUsedByGradeName(String gradeName);

    boolean enableGrade(Long id);

    boolean disableGrade(Long id);

    CustomerGrade getDefaultGrade();

    boolean setDefaultGrade(Long id);

    BigDecimal getDiscountRate(Long gradeId);

    BigDecimal getPointRate(Long gradeId);

    BigDecimal getCreditLimit(Long gradeId);
}
