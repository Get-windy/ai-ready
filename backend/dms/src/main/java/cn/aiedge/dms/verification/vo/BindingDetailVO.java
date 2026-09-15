package cn.aiedge.dms.verification.vo;

import cn.aiedge.dms.verification.entity.DmsPositionVerification;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVehicleInspection;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import lombok.Data;

import java.util.List;

/**
 * 人车绑定详情（绑定信息 + 核验历史 + 关联巡检 + 关联预警）
 *
 * <p>补齐原本不存在的 {@code GET /binding/{id}}：绑定详情页此前取不到数。</p>
 */
@Data
public class BindingDetailVO {

    /** 绑定信息 */
    private DmsRiderVehicleBinding binding;

    /** 位置核验历史 */
    private List<DmsPositionVerification> verifications;

    /** 该绑定关联的巡检记录 */
    private List<DmsVehicleInspection> inspections;

    /** 该绑定产生的预警 */
    private List<DmsVerificationAlert> alerts;

    /** 核验次数 */
    private Integer verifyCount;

    /** 异常核验次数 */
    private Integer abnormalCount;
}
