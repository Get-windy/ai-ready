package cn.aiedge.erp.batchsn.service.impl;

import cn.aiedge.erp.batchsn.entity.SerialFlowRecord;
import cn.aiedge.erp.batchsn.entity.SerialNumber;
import cn.aiedge.erp.batchsn.enums.SnStatusEnum;
import cn.aiedge.erp.batchsn.mapper.SerialFlowRecordMapper;
import cn.aiedge.erp.batchsn.mapper.SerialNumberMapper;
import cn.aiedge.erp.batchsn.service.SerialNumberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 序列号服务实现
 *
 * @author team-member
 * @date 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialNumberServiceImpl extends ServiceImpl<SerialNumberMapper, SerialNumber> implements SerialNumberService {

    private final SerialNumberMapper serialNumberMapper;
    private final SerialFlowRecordMapper serialFlowRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber createSerial(SerialNumber serial) {
        if (serial.getSerialNo() == null || serial.getSerialNo().isEmpty()) {
            serial.setSerialNo(generateSerialNo(serial));
        }
        if (serial.getSnStatus() == null) {
            serial.setSnStatus(SnStatusEnum.AVAILABLE.getCode());
        }
        if (serial.getSnStage() == null) {
            serial.setSnStage("WAREHOUSE");
        }
        if (serial.getWarrantyStartDate() == null) {
            serial.setWarrantyStartDate(LocalDate.now());
        }
        if (serial.getWarrantyPeriod() != null && serial.getWarrantyEndDate() == null) {
            serial.setWarrantyEndDate(serial.getWarrantyStartDate().plusMonths(serial.getWarrantyPeriod()));
        }
        serial.setCreatedAt(LocalDateTime.now());
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.insert(serial);
        log.info("创建序列号成功: serialNo={}, productCode={}", serial.getSerialNo(), serial.getProductCode());
        return serial;
    }

    private String generateSerialNo(SerialNumber serial) {
        String prefix = "SN";
        String dateStr = LocalDate.now().toString().replace("-", "");
        String seq = String.format("%06d", System.currentTimeMillis() % 1000000);
        return prefix + dateStr + seq;
    }

    @Override
    public SerialNumber getSerialById(Long id) {
        return serialNumberMapper.selectById(id);
    }

    @Override
    public SerialNumber getSerialByNo(String serialNo) {
        return serialNumberMapper.selectBySerialNo(serialNo);
    }

    @Override
    public List<SerialNumber> listSerials(String serialNo, String productCode, String status,
                                          String batchNo, int page, int size) {
        LambdaQueryWrapper<SerialNumber> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialNumber::getIsDeleted, 0);
        if (StringUtils.hasText(serialNo)) {
            wrapper.like(SerialNumber::getSerialNo, serialNo);
        }
        if (StringUtils.hasText(productCode)) {
            wrapper.eq(SerialNumber::getProductCode, productCode);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SerialNumber::getSnStatus, status);
        }
        if (StringUtils.hasText(batchNo)) {
            wrapper.eq(SerialNumber::getBatchNo, batchNo);
        }
        wrapper.orderByDesc(SerialNumber::getCreatedAt);
        wrapper.last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        return serialNumberMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber updateSerial(Long id, SerialNumber serial) {
        SerialNumber existing = serialNumberMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("序列号不存在: id=" + id);
        }
        serial.setId(id);
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(serial);
        return serialNumberMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber inbound(SerialNumber serial, Long warehouseId, String warehouseName, Long locationId) {
        SerialNumber existing = serial.getId() != null ? serialNumberMapper.selectById(serial.getId()) : null;
        if (existing == null) {
            serial.setWarehouseId(warehouseId);
            serial.setLocationId(locationId);
            serial.setCurrentLocation(warehouseName);
            serial.setSnStatus(SnStatusEnum.AVAILABLE.getCode());
            serial.setSnStage("WAREHOUSE");
            return createSerial(serial);
        }
        existing.setWarehouseId(warehouseId);
        existing.setLocationId(locationId);
        existing.setCurrentLocation(warehouseName);
        existing.setSnStatus(SnStatusEnum.AVAILABLE.getCode());
        existing.setSnStage("WAREHOUSE");
        existing.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(existing);

        recordFlow(existing, "INBOUND", existing.getSnStatus(), existing.getSnStatus(),
            existing.getSnStage(), existing.getSnStage(), null, warehouseName);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber outbound(Long serialId, Long saleOrderId, String saleOrderNo,
                                  Long warehouseId, Long locationId) {
        SerialNumber serial = serialNumberMapper.selectById(serialId);
        if (serial == null) {
            throw new RuntimeException("序列号不存在: id=" + serialId);
        }
        String fromStatus = serial.getSnStatus();
        String fromStage = serial.getSnStage();
        serial.setSaleOrderId(saleOrderId);
        serial.setSaleOrderNo(saleOrderNo);
        serial.setSnStatus(SnStatusEnum.IN_USE.getCode());
        serial.setSnStage("EOF_CUSTOMER");
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(serial);

        recordFlow(serial, "OUTBOUND", fromStatus, serial.getSnStatus(),
            fromStage, serial.getSnStage(), serial.getCurrentLocation(), "Customer");
        return serial;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber updateStatus(Long id, String status, String stage) {
        SerialNumber serial = serialNumberMapper.selectById(id);
        if (serial == null) {
            throw new RuntimeException("序列号不存在: id=" + id);
        }
        String fromStatus = serial.getSnStatus();
        String fromStage = serial.getSnStage();
        serial.setSnStatus(status);
        if (stage != null) {
            serial.setSnStage(stage);
        }
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(serial);

        recordFlow(serial, "STATUS_CHANGE", fromStatus, status, fromStage,
            stage != null ? stage : fromStage, serial.getCurrentLocation(), serial.getCurrentLocation());
        return serial;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber addMaintenanceRecord(Long serialId, String remark, String operatorId) {
        SerialNumber serial = serialNumberMapper.selectById(serialId);
        if (serial == null) {
            throw new RuntimeException("序列号不存在: id=" + serialId);
        }
        String fromStatus = serial.getSnStatus();
        String fromStage = serial.getSnStage();
        serial.setMaintenanceCount(serial.getMaintenanceCount() != null ? serial.getMaintenanceCount() + 1 : 1);
        serial.setLastMaintenanceDate(LocalDateTime.now());
        serial.setSnStatus(SnStatusEnum.MAINTAINED.getCode());
        serial.setQualityStatus("UNDER_REPAIR");
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(serial);

        recordFlow(serial, "MAINTAIN", fromStatus, serial.getSnStatus(),
            fromStage, "IN_SERVICE", serial.getCurrentLocation(), serial.getCurrentLocation());
        return serial;
    }

    @Override
    public List<SerialNumber> getWarrantyExpiring(int warningDays) {
        return serialNumberMapper.selectWarrantyExpiring(warningDays, new Date());
    }

    @Override
    public List<SerialNumber> getWarrantyExpired() {
        return serialNumberMapper.selectWarrantyExpired(new Date());
    }

    @Override
    public List<SerialNumber> getFullHistory(Long serialId) {
        LambdaQueryWrapper<SerialFlowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialFlowRecord::getSerialId, serialId);
        wrapper.orderByDesc(SerialFlowRecord::getCreatedAt);
        return null;
    }

    @Override
    public List<SerialNumber> getMaintenanceHistory(Long serialId) {
        LambdaQueryWrapper<SerialFlowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialFlowRecord::getSerialId, serialId);
        wrapper.eq(SerialFlowRecord::getFlowType, "MAINTAIN");
        wrapper.orderByDesc(SerialFlowRecord::getCreatedAt);
        return null;
    }

    @Override
    public boolean serialNoExists(String serialNo) {
        return serialNumberMapper.selectBySerialNo(serialNo) != null;
    }

    @Override
    public boolean isWarrantyExpiring(SerialNumber serial, int warningDays) {
        if (serial == null || serial.getWarrantyEndDate() == null) {
            return false;
        }
        return serial.getWarrantyEndDate().isBefore(LocalDate.now().plusDays(warningDays));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSerialStatus(List<Long> serialIds, String status, String stage, String updaterId) {
        if (serialIds == null || serialIds.isEmpty()) {
            return 0;
        }
        return serialNumberMapper.updateSerialStatus(serialIds, status, stage, updaterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber scrapSerial(Long serialId, String remark, String operatorId) {
        SerialNumber serial = serialNumberMapper.selectById(serialId);
        if (serial == null) {
            throw new RuntimeException("序列号不存在: id=" + serialId);
        }
        String fromStatus = serial.getSnStatus();
        String fromStage = serial.getSnStage();
        serial.setSnStatus(SnStatusEnum.SCRAP.getCode());
        serial.setSnStage("SCRAPPED");
        serial.setUpdatedAt(LocalDateTime.now());
        serialNumberMapper.updateById(serial);

        recordFlow(serial, "SCRAP", fromStatus, serial.getSnStatus(),
            fromStage, serial.getSnStage(), serial.getCurrentLocation(), "Scrap Yard");
        return serial;
    }

    @Override
    public String getCurrentLocation(Long serialId) {
        SerialNumber serial = serialNumberMapper.selectById(serialId);
        return serial != null ? serial.getCurrentLocation() : null;
    }

    @Override
    public Long getCurrentWarehouseId(Long serialId) {
        SerialNumber serial = serialNumberMapper.selectById(serialId);
        return serial != null ? serial.getWarehouseId() : null;
    }

    private void recordFlow(SerialNumber serial, String flowType,
                            String fromStatus, String toStatus,
                            String fromStage, String toStage,
                            String fromLocation, String toLocation) {
        SerialFlowRecord record = new SerialFlowRecord();
        record.setSerialId(serial.getId());
        record.setSerialNo(serial.getSerialNo());
        record.setProductId(serial.getProductId());
        record.setProductCode(serial.getProductCode());
        record.setProductName(serial.getProductName());
        record.setFlowType(flowType);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setFromStage(fromStage);
        record.setToStage(toStage);
        record.setFromLocation(fromLocation);
        record.setToLocation(toLocation);
        record.setCreatedAt(LocalDateTime.now());
        serialFlowRecordMapper.insert(record);
    }
}
