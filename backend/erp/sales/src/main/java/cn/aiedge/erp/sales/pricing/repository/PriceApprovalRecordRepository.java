package cn.aiedge.erp.sales.pricing.repository;

import cn.aiedge.erp.sales.pricing.entity.PriceApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 价格审批记录Repository接口
 */
@Repository
public interface PriceApprovalRecordRepository extends JpaRepository<PriceApprovalRecord, Long>,
                                                       JpaSpecificationExecutor<PriceApprovalRecord> {

    /**
     * 根据申请ID查询所有审批记录，按创建时间升序
     */
    List<PriceApprovalRecord> findByApprovalIdOrderByCreateTimeAsc(Long approvalId);
}
