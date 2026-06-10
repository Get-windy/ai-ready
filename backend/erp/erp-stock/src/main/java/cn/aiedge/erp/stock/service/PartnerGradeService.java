package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.PartnerGrade;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PartnerGradeService extends IService<PartnerGrade> {
    List<PartnerGrade> getByType(String gradeType);
}
