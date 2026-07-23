package cn.aiedge.erp.sale.retail.service.impl;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailShift;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderMapper;
import cn.aiedge.erp.sale.retail.mapper.RetailShiftMapper;
import cn.aiedge.erp.sale.retail.service.IRetailShiftService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailShiftServiceImpl extends ServiceImpl<RetailShiftMapper, RetailShift> implements IRetailShiftService {

    private final RetailOrderMapper retailOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailShift openShift(Long cashierId, String cashierName, Long warehouseId,
                                 BigDecimal openingCash, String remark) {
        if (cashierId == null) throw new RuntimeException("收银员不能为空");
        // 校验该收银员无未交班班次
        Long openCount = count(new LambdaQueryWrapper<RetailShift>()
                .eq(RetailShift::getCashierId, cashierId)
                .eq(RetailShift::getStatus, 1)
                .eq(RetailShift::getDeleted, 0));
        if (openCount != null && openCount > 0) {
            throw new RuntimeException("该收银员存在未交班班次，请先交班");
        }

        RetailShift shift = new RetailShift();
        shift.setShiftNo(generateShiftNo());
        shift.setCashierId(cashierId);
        shift.setCashierName(cashierName);
        shift.setWarehouseId(warehouseId);
        shift.setOpenTime(LocalDateTime.now());
        shift.setOpeningCash(openingCash != null ? openingCash : BigDecimal.ZERO);
        shift.setStatus(1); // 营业中
        shift.setRemark(remark);
        save(shift);
        log.info("开班: shiftNo={}, cashierId={}, openingCash={}", shift.getShiftNo(), cashierId, shift.getOpeningCash());
        return shift;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailShift closeShift(Long shiftId, BigDecimal closingCash, String remark) {
        RetailShift shift = getById(shiftId);
        if (shift == null) throw new RuntimeException("班次不存在");
        if (shift.getStatus() != 1) throw new RuntimeException("该班次已交班");

        LocalDateTime closeTime = LocalDateTime.now();
        // 汇总该班次时段内已结算（status=1）零售单
        List<RetailOrder> orders = listShiftOrders(shift, closeTime);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal qrAmount = BigDecimal.ZERO;
        BigDecimal otherAmount = BigDecimal.ZERO;
        for (RetailOrder o : orders) {
            BigDecimal cash = nvl(o.getCashAmount());
            BigDecimal qr = nvl(o.getAlipayAmount()).add(nvl(o.getWechatAmount())).add(nvl(o.getAggregateAmount()));
            BigDecimal other = nvl(o.getCardAmount()).add(nvl(o.getAbcAmount())).add(nvl(o.getCcbAmount()))
                    .add(nvl(o.getJdAmount())).add(nvl(o.getPrepaidAmount())).add(nvl(o.getTransferAmount()));
            cashAmount = cashAmount.add(cash);
            qrAmount = qrAmount.add(qr);
            otherAmount = otherAmount.add(other);
            // 实收总额：以各支付方式汇总为准，退回单为负值时自然冲减
            totalAmount = totalAmount.add(cash).add(qr).add(other);
        }

        // 应收现金 = 期初现金 + 现金销售 - 现金退款（退款单现金为负，直接累加即冲减）
        BigDecimal openingCash = nvl(shift.getOpeningCash());
        BigDecimal expectedCash = openingCash.add(cashAmount);
        BigDecimal closing = closingCash != null ? closingCash : BigDecimal.ZERO;

        shift.setCloseTime(closeTime);
        shift.setClosingCash(closing);
        shift.setExpectedCash(expectedCash);
        shift.setDifference(closing.subtract(expectedCash));
        shift.setOrderCount(orders.size());
        shift.setTotalAmount(totalAmount);
        shift.setCashAmount(cashAmount);
        shift.setQrAmount(qrAmount);
        shift.setOtherAmount(otherAmount);
        shift.setStatus(2); // 已交班
        if (StringUtils.hasText(remark)) {
            shift.setRemark((shift.getRemark() != null ? shift.getRemark() + "; " : "") + remark);
        }
        updateById(shift);
        log.info("交班: shiftNo={}, orderCount={}, expectedCash={}, closingCash={}, difference={}",
                shift.getShiftNo(), orders.size(), expectedCash, closing, shift.getDifference());
        return shift;
    }

    @Override
    public RetailShift getCurrent(Long cashierId) {
        if (cashierId == null) return null;
        return getOne(new LambdaQueryWrapper<RetailShift>()
                .eq(RetailShift::getCashierId, cashierId)
                .eq(RetailShift::getStatus, 1)
                .eq(RetailShift::getDeleted, 0)
                .orderByDesc(RetailShift::getOpenTime)
                .last("LIMIT 1"));
    }

    @Override
    public IPage<RetailShift> pageShifts(Page<RetailShift> page, Long cashierId, Integer status,
                                         String dateStart, String dateEnd) {
        LambdaQueryWrapper<RetailShift> wrapper = new LambdaQueryWrapper<RetailShift>()
                .eq(RetailShift::getDeleted, 0)
                .eq(cashierId != null, RetailShift::getCashierId, cashierId)
                .eq(status != null, RetailShift::getStatus, status)
                .ge(StringUtils.hasText(dateStart), RetailShift::getOpenTime, parseDateTime(dateStart, false))
                .le(StringUtils.hasText(dateEnd), RetailShift::getOpenTime, parseDateTime(dateEnd, true))
                .orderByDesc(RetailShift::getOpenTime);
        return page(page, wrapper);
    }

    @Override
    public ShiftDetailVO getDetail(Long id) {
        RetailShift shift = getById(id);
        if (shift == null) return null;
        ShiftDetailVO vo = new ShiftDetailVO();
        vo.setShift(shift);
        // 汇总明细：班次时段内已结算零售单
        LocalDateTime end = shift.getCloseTime() != null ? shift.getCloseTime() : LocalDateTime.now();
        vo.setOrders(listShiftOrders(shift, end));
        return vo;
    }

    // ── 内部工具方法 ──

    /**
     * 查询班次时段内该收银员已结算（status=1）零售单，按完成时间归属班次
     */
    private List<RetailOrder> listShiftOrders(RetailShift shift, LocalDateTime endTime) {
        return retailOrderMapper.selectList(new LambdaQueryWrapper<RetailOrder>()
                .eq(RetailOrder::getDeleted, 0)
                .eq(RetailOrder::getStatus, 1)
                .eq(RetailOrder::getCashierId, shift.getCashierId())
                .ge(RetailOrder::getCompletedTime, shift.getOpenTime())
                .le(RetailOrder::getCompletedTime, endTime)
                .orderByAsc(RetailOrder::getCompletedTime));
    }

    private String generateShiftNo() {
        return "BC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private LocalDateTime parseDateTime(String dateString, boolean endOfDay) {
        try {
            LocalDate date = LocalDate.parse(dateString);
            return endOfDay ? date.plusDays(1).atStartOfDay().minusSeconds(1) : date.atStartOfDay();
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateString);
            return null;
        }
    }
}
