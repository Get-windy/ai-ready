package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.LoyaltyProgram;
import cn.aiedge.erp.marketing.mapper.LoyaltyProgramMapper;
import cn.aiedge.erp.marketing.service.LoyaltyProgramService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LoyaltyProgramServiceImpl extends ServiceImpl<LoyaltyProgramMapper, LoyaltyProgram>
        implements LoyaltyProgramService {

    @Override
    public List<LoyaltyProgram> listByType(String programType) {
        return list(new LambdaQueryWrapper<LoyaltyProgram>()
                .eq(LoyaltyProgram::getProgramType, programType)
                .orderByAsc(LoyaltyProgram::getSortOrder));
    }

    @Override
    public List<LoyaltyProgram> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<LoyaltyProgram>()
                .eq(LoyaltyProgram::getIsActive, 1)
                .and(w -> w
                        .isNull(LoyaltyProgram::getStartDate)
                        .or().le(LoyaltyProgram::getStartDate, now))
                .and(w -> w
                        .isNull(LoyaltyProgram::getEndDate)
                        .or().ge(LoyaltyProgram::getEndDate, now))
                .apply("max_usage IS NULL OR usage_count < max_usage")
                .orderByAsc(LoyaltyProgram::getSortOrder));
    }

    @Override
    public void incrementUsage(Long programId) {
        LoyaltyProgram program = getById(programId);
        if (program != null) {
            Integer currentCount = program.getUsageCount() != null ? program.getUsageCount() : 0;
            program.setUsageCount(currentCount + 1);
            updateById(program);
        }
    }
}
