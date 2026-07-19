package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.LoyaltyProgram;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface LoyaltyProgramService extends IService<LoyaltyProgram> {
    List<LoyaltyProgram> listByType(String programType);
    List<LoyaltyProgram> listActive();
    void incrementUsage(Long programId);
}
