package cn.aiedge.transaction.util;

import java.util.UUID;

/**
 * 事务ID生成器
 * 生成分布式事务所需的唯一ID
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class TransactionIdGenerator {

    /**
     * 生成全局事务ID
     * 
     * @return 全局事务ID
     */
    public static String generateGlobalTxId() {
        return "GTX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 16);
    }

    /**
     * 生成分支事务ID
     * 
     * @param globalTxId 全局事务ID
     * @return 分支事务ID
     */
    public static String generateBranchTxId(String globalTxId) {
        return globalTxId + "-BTX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 生成参与者ID
     * 
     * @param branchTxId 分支事务ID
     * @return 参与者ID
     */
    public static String generateParticipantId(String branchTxId) {
        return branchTxId + "-PTP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}