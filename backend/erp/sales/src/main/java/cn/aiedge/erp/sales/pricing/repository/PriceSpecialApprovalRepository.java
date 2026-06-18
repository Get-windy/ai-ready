package cn.aiedge.erp.sales.pricing.repository;

import cn.aiedge.erp.sales.pricing.entity.PriceSpecialApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 价格特批申请Repository接口
 */
@Repository
public interface PriceSpecialApprovalRepository extends JpaRepository<PriceSpecialApproval, Long>,
                                                        JpaSpecificationExecutor<PriceSpecialApproval> {

    /**
     * 根据申请编号查询
     */
    Optional<PriceSpecialApproval> findByApprovalCode(String approvalCode);
}
