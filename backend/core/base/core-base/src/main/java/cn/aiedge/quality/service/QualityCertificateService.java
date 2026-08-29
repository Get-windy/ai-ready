package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityCertificate;

/**
 * 质量证书服务接口
 */
public interface QualityCertificateService {

    /**
     * 创建质量证书
     */
    QualityCertificate create(QualityCertificate certificate);

    /**
     * 更新质量证书
     */
    QualityCertificate update(Long id, QualityCertificate certificate);

    /**
     * 删除质量证书
     */
    void delete(Long id);

    /**
     * 分页查询质量证书
     *
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param productName  产品名称(模糊)
     * @param batchNo      批次号(模糊)
     * @param result       检验结论
     * @param startDate    检验日期起始
     * @param endDate      检验日期结束
     */
    PageResult<QualityCertificate> page(Integer pageNum, Integer pageSize,
                                        String productName, String batchNo,
                                        String result, String startDate, String endDate);

    /**
     * 查询质量证书详情
     */
    QualityCertificate get(Long id);
}
