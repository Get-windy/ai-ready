package cn.aiedge.dms.vehicle.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.EnergyCardTypeEnum;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.vehicle.dto.EnergyCardCreateDTO;
import cn.aiedge.dms.vehicle.dto.EnergyCardQuery;
import cn.aiedge.dms.vehicle.dto.EnergyCardVO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyCard;
import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyLog;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleEnergyCardMapper;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleEnergyLogMapper;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 补能卡 / 套餐档案服务
 *
 * <p>「一卡一车一人」是本地档案的红线：卡必须绑定唯一主体，且补能时用卡主体必须与绑定主体一致
 * （在校验发生在 {@link VehicleEnergyLogService} 落流水时）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleEnergyCardService {

    private final DmsVehicleEnergyCardMapper cardMapper;
    private final DmsVehicleEnergyLogMapper energyLogMapper;
    private final DmsVehicleMapper vehicleMapper;
    private final DmsRiderMapper riderMapper;

    // ==================== 查询 ====================

    public IPage<EnergyCardVO> page(EnergyCardQuery query) {
        LambdaQueryWrapper<DmsVehicleEnergyCard> wrapper = new LambdaQueryWrapper<DmsVehicleEnergyCard>()
                .eq(query.getCardType() != null, DmsVehicleEnergyCard::getCardType, query.getCardType())
                .eq(query.getStatus() != null, DmsVehicleEnergyCard::getStatus, query.getStatus())
                .eq(query.getVehicleId() != null, DmsVehicleEnergyCard::getVehicleId, query.getVehicleId())
                .eq(query.getRiderId() != null, DmsVehicleEnergyCard::getRiderId, query.getRiderId())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(DmsVehicleEnergyCard::getCardNo, query.getKeyword())
                        .or().like(DmsVehicleEnergyCard::getCardName, query.getKeyword())
                        .or().like(DmsVehicleEnergyCard::getIssuer, query.getKeyword()))
                .orderByDesc(DmsVehicleEnergyCard::getStatus)
                .orderByAsc(DmsVehicleEnergyCard::getExpireDate)
                .orderByDesc(DmsVehicleEnergyCard::getId);

        IPage<DmsVehicleEnergyCard> raw = cardMapper.selectPage(
                new Page<>(query.getPage() == null ? 1 : query.getPage(),
                        query.getSize() == null ? 20 : query.getSize()), wrapper);
        return raw.convert(this::toVo);
    }

    public EnergyCardVO detail(Long id) {
        return toVo(requireCard(id));
    }

    /** 启用卡下拉（补能录入时选卡用） */
    public List<EnergyCardVO> options() {
        List<DmsVehicleEnergyCard> list = cardMapper.selectList(new LambdaQueryWrapper<DmsVehicleEnergyCard>()
                .eq(DmsVehicleEnergyCard::getStatus, 1)
                .orderByAsc(DmsVehicleEnergyCard::getCardNo));
        List<EnergyCardVO> result = new ArrayList<>();
        for (DmsVehicleEnergyCard card : list) {
            result.add(toVo(card));
        }
        return result;
    }

    /** 按卡号查启用卡（补能落流水时的稽核入口；不存在返回 null） */
    public DmsVehicleEnergyCard findByCardNo(String cardNo) {
        if (!StringUtils.hasText(cardNo)) {
            return null;
        }
        return cardMapper.selectOne(new LambdaQueryWrapper<DmsVehicleEnergyCard>()
                .eq(DmsVehicleEnergyCard::getCardNo, cardNo.trim())
                .last("LIMIT 1"));
    }

    // ==================== 新增 / 修改 / 删除 / 启停 ====================

    @Transactional(rollbackFor = Exception.class)
    public EnergyCardVO create(EnergyCardCreateDTO dto) {
        assertSubject(dto);
        assertCardNoUnique(dto.getCardNo(), null);
        DmsVehicleEnergyCard card = new DmsVehicleEnergyCard();
        applyDto(card, dto);
        card.setUsedQuota(BigDecimal.ZERO);
        cardMapper.insert(card);
        log.info("新增补能卡: id={}, cardNo={}, 主体={}", card.getId(), card.getCardNo(), subjectText(card));
        return toVo(card);
    }

    @Transactional(rollbackFor = Exception.class)
    public EnergyCardVO update(Long id, EnergyCardCreateDTO dto) {
        DmsVehicleEnergyCard card = requireCard(id);
        assertSubject(dto);
        assertCardNoUnique(dto.getCardNo(), id);
        applyDto(card, dto);
        cardMapper.updateById(card);
        return toVo(card);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCard(id);
        cardMapper.deleteById(id);
        log.info("删除补能卡: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsVehicleEnergyCard card = requireCard(id);
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态非法: " + status);
        }
        card.setStatus(status);
        cardMapper.updateById(card);
    }

    // ==================== 已用额度重算 ====================

    /**
     * 重算某卡号的已用额度：按「卡生效期内的补能流水数量」汇总（可重算，不维护增量）
     *
     * <p>由补能流水的增删改触发调用；卡不存在时静默跳过（补能允许只填卡号不建档）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void recomputeUsedQuota(String cardNo) {
        DmsVehicleEnergyCard card = findByCardNo(cardNo);
        if (card == null) {
            return;
        }
        card.setUsedQuota(sumUsedQuota(card, null));
        cardMapper.updateById(card);
    }

    /**
     * 汇总某卡在有效期内的已用额度（按流水数量求和）
     *
     * @param excludeLogId 排除的记录（更新场景排除自身，避免"把自己算两遍"）
     */
    public BigDecimal sumUsedQuota(DmsVehicleEnergyCard card, Long excludeLogId) {
        LambdaQueryWrapper<DmsVehicleEnergyLog> wrapper = new LambdaQueryWrapper<DmsVehicleEnergyLog>()
                .eq(DmsVehicleEnergyLog::getCardNo, card.getCardNo())
                .ne(excludeLogId != null, DmsVehicleEnergyLog::getId, excludeLogId);
        if (card.getStartDate() != null) {
            wrapper.ge(DmsVehicleEnergyLog::getOccurredAt, card.getStartDate().atStartOfDay());
        }
        if (card.getExpireDate() != null) {
            wrapper.le(DmsVehicleEnergyLog::getOccurredAt, card.getExpireDate().plusDays(1).atStartOfDay());
        }
        return energyLogMapper.selectList(wrapper).stream()
                .map(DmsVehicleEnergyLog::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== 内部方法 ====================

    private DmsVehicleEnergyCard requireCard(Long id) {
        DmsVehicleEnergyCard card = cardMapper.selectById(id);
        if (card == null) {
            throw BusinessException.notFound("补能卡不存在: " + id);
        }
        return card;
    }

    /** 一卡一车一人：绑定主体二选一且必须存在 */
    private void assertSubject(EnergyCardCreateDTO dto) {
        if (dto.getVehicleId() == null && dto.getRiderId() == null) {
            throw BusinessException.badRequest("请绑定持卡主体：四轮车 或 配送员（一卡一车一人）");
        }
        if (dto.getVehicleId() != null && dto.getRiderId() != null) {
            throw BusinessException.badRequest("一张卡只能绑定一个主体：四轮车 或 配送员");
        }
        if (dto.getVehicleId() != null && vehicleMapper.selectById(dto.getVehicleId()) == null) {
            throw BusinessException.notFound("车辆不存在: " + dto.getVehicleId());
        }
        if (dto.getRiderId() != null && riderMapper.selectById(dto.getRiderId()) == null) {
            throw BusinessException.notFound("配送员不存在: " + dto.getRiderId());
        }
    }

    private void assertCardNoUnique(String cardNo, Long excludeId) {
        Long exists = cardMapper.selectCount(new LambdaQueryWrapper<DmsVehicleEnergyCard>()
                .eq(DmsVehicleEnergyCard::getCardNo, cardNo.trim())
                .ne(excludeId != null, DmsVehicleEnergyCard::getId, excludeId));
        if (exists != null && exists > 0) {
            throw BusinessException.badRequest("卡号已存在: " + cardNo);
        }
    }

    private void applyDto(DmsVehicleEnergyCard card, EnergyCardCreateDTO dto) {
        card.setCardNo(dto.getCardNo().trim());
        card.setCardName(dto.getCardName());
        card.setCardType(dto.getCardType());
        card.setVehicleId(dto.getVehicleId());
        card.setRiderId(dto.getRiderId());
        card.setIssuer(dto.getIssuer());
        card.setMonthlyFee(dto.getMonthlyFee());
        card.setBalance(dto.getBalance());
        card.setQuota(dto.getQuota());
        card.setStartDate(dto.getStartDate());
        card.setExpireDate(dto.getExpireDate());
        card.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        card.setRemark(dto.getRemark());
    }

    private EnergyCardVO toVo(DmsVehicleEnergyCard card) {
        EnergyCardVO vo = new EnergyCardVO();
        BeanUtils.copyProperties(card, vo);
        vo.setCardTypeText(EnergyCardTypeEnum.textOf(card.getCardType()));
        vo.setStatusText(Integer.valueOf(1).equals(card.getStatus()) ? "启用" : "停用");
        if (card.getVehicleId() != null) {
            vo.setSubjectType("VEHICLE");
            DmsVehicle vehicle = vehicleMapper.selectById(card.getVehicleId());
            vo.setSubjectName(vehicle == null ? null : vehicle.getPlateNo());
        } else if (card.getRiderId() != null) {
            vo.setSubjectType("RIDER");
            DmsRider rider = riderMapper.selectById(card.getRiderId());
            vo.setSubjectName(rider == null ? null : rider.getRealName());
        }
        if (card.getExpireDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), card.getExpireDate());
            vo.setDaysToExpire(days);
            vo.setExpired(days < 0);
        } else {
            vo.setExpired(false);
        }
        if (card.getQuota() != null && card.getQuota().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal used = card.getUsedQuota() == null ? BigDecimal.ZERO : card.getUsedQuota();
            vo.setQuotaUsagePercent(used.multiply(BigDecimal.valueOf(100))
                    .divide(card.getQuota(), 0, RoundingMode.HALF_UP).intValue());
        }
        return vo;
    }

    private String subjectText(DmsVehicleEnergyCard card) {
        return card.getVehicleId() != null ? "vehicle:" + card.getVehicleId() : "rider:" + card.getRiderId();
    }
}
