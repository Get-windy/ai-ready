package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.LoyaltyCard;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface LoyaltyCardService extends IService<LoyaltyCard> {
    List<LoyaltyCard> listByMember(Long memberId);
    void addPoints(Long cardId, Integer points);
    void deductPoints(Long cardId, Integer points);
}
