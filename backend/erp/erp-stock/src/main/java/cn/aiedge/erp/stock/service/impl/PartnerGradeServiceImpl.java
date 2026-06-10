package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerGrade;
import cn.aiedge.erp.stock.mapper.PartnerGradeMapper;
import cn.aiedge.erp.stock.service.PartnerGradeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class PartnerGradeServiceImpl extends ServiceImpl<PartnerGradeMapper, PartnerGrade>
        implements PartnerGradeService {

    @Override
    public List<PartnerGrade> getByType(String gradeType) {
        return lambdaQuery()
                .eq(PartnerGrade::getGradeType, gradeType)
                .eq(PartnerGrade::getDeleted, 0)
                .orderByAsc(PartnerGrade::getSortOrder)
                .list();
    }
}
