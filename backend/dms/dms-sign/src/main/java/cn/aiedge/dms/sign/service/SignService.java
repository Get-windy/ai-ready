package cn.aiedge.dms.sign.service;

import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.sign.entity.DmsSign;
import cn.aiedge.dms.sign.mapper.DmsSignMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 签收服务
 *
 * 处理签收提交、定位偏差计算、签收记录查询等业务逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {

    private final DmsSignMapper signMapper;
    private final DmsTaskMapper taskMapper;

    /**
     * 提交签收
     *
     * @param taskId           任务ID
     * @param signType         签收类型（1-正常 2-部分 3-拒收）
     * @param photoUrls        照片URL列表（JSON数组）
     * @param signatureUrl     手写签名图片URL
     * @param signLat          签收纬度
     * @param signLng          签收经度
     * @param customerLat      客户实际纬度
     * @param customerLng      客户实际经度
     * @param deviationThresh  偏差阈值（米），超过此值发出警告
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long taskId, Integer signType, String photoUrls, String signatureUrl,
                       BigDecimal signLat, BigDecimal signLng,
                       BigDecimal customerLat, BigDecimal customerLng,
                       double deviationThresh) {
        // 校验任务
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_DELIVERING) {
            throw new DmsBusinessException("任务状态不允许签收，当前状态: " + task.getStatus());
        }

        // 计算定位偏差
        double deviation = calculateDeviation(signLat, signLng, customerLat, customerLng);
        int locationWarning = deviation > deviationThresh ? 1 : 0;

        // 创建签收记录
        DmsSign sign = new DmsSign();
        sign.setTaskId(taskId);
        sign.setTenantId(task.getTenantId());
        sign.setSignType(signType);
        sign.setPhotoUrls(photoUrls);
        sign.setSignatureUrl(signatureUrl);
        sign.setSignLat(signLat);
        sign.setSignLng(signLng);
        sign.setCustomerLat(customerLat);
        sign.setCustomerLng(customerLng);
        sign.setLocationDeviation(BigDecimal.valueOf(deviation).setScale(2, RoundingMode.HALF_UP));
        sign.setLocationWarning(locationWarning);
        sign.setSignTime(LocalDateTime.now());
        sign.setAuditStatus(0);
        signMapper.insert(sign);

        // 更新任务状态为已签收
        task.setStatus(DmsConstants.TASK_SIGNED);
        taskMapper.updateById(task);

        if (locationWarning == 1) {
            log.warn("签收定位偏差超限: taskId={}, deviation={}米, 阈值={}米", taskId, deviation, deviationThresh);
        }

        log.info("签收提交成功: taskId={}, signType={}, deviation={}米", taskId, signType, deviation);
    }

    /**
     * 根据任务ID获取签收记录
     *
     * @param taskId 任务ID
     * @return 签收记录
     */
    public DmsSign getByTaskId(Long taskId) {
        return signMapper.selectOne(
                new LambdaQueryWrapper<DmsSign>()
                        .eq(DmsSign::getTaskId, taskId)
                        .orderByDesc(DmsSign::getCreateTime)
                        .last("LIMIT 1")
        );
    }

    // ========== 私有辅助方法 ==========

    /**
     * 计算两点间的距离（Haversine 公式）
     *
     * @param lat1 点1纬度
     * @param lng1 点1经度
     * @param lat2 点2纬度
     * @param lng2 点2经度
     * @return 距离（米）
     */
    private double calculateDeviation(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0;
        }

        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double radLng1 = Math.toRadians(lng1.doubleValue());
        double radLng2 = Math.toRadians(lng2.doubleValue());

        double dLat = radLat2 - radLat1;
        double dLng = radLng2 - radLng1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 地球平均半径 6371000 米
        return 6371000 * c;
    }
}
