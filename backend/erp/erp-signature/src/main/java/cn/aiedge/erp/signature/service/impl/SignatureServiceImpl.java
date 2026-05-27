package cn.aiedge.erp.signature.service.impl;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.SignatureRecord;
import cn.aiedge.erp.signature.enums.SignatureStatus;
import cn.aiedge.erp.signature.enums.SignatureType;
import cn.aiedge.erp.signature.mapper.SignatureRecordMapper;
import cn.aiedge.erp.signature.service.SignatureService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SignatureServiceImpl implements SignatureService {

    private final SignatureRecordMapper signatureMapper;

    @Override
    @Transactional
    public SignatureRecord photoSignature(PhotoSignatureRequest request, MultipartFile photo) {
        SignatureRecord record = new SignatureRecord();
        record.setSignatureCode("SIG" + IdUtil.fastSimpleUUID().substring(0, 8));
        record.setOrderNo(request.getOrderNo());
        record.setOrderId(request.getOrderId());
        record.setSignatureType(SignatureType.PHOTO.getCode());
        record.setSignerName(request.getSignerName());
        record.setSignerPhone(request.getSignerPhone());
        record.setLocation(request.getLocation());
        record.setLatitude(request.getLatitude());
        record.setLongitude(request.getLongitude());
        record.setDeviceId(request.getDeviceId());
        record.setDeliveryPersonId(request.getDeliveryPersonId());
        record.setDeliveryPersonName(request.getDeliveryPersonName());
        record.setSignTime(LocalDateTime.now());
        record.setStatus(1);
        record.setRemark(request.getRemark());

        if (photo != null && !photo.isEmpty()) {
            try {
                String photoHash = SecureUtil.md5(new String(photo.getBytes(), "UTF-8"));
                record.setPhotoPath(photoHash);
                record.setIntegrityHash(generateIntegrityHash(record, photoHash));
            } catch (IOException e) {
                throw new RuntimeException("照片处理失败: " + e.getMessage());
            }
        }

        record.setVerified(false);
        signatureMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public SignatureRecord electronicSignature(ElectronicSignatureRequest request) {
        if (request.getSignatureData() == null || request.getSignatureData().isEmpty()) {
            throw new RuntimeException("签名数据不能为空");
        }

        SignatureRecord record = new SignatureRecord();
        record.setSignatureCode("SIG" + IdUtil.fastSimpleUUID().substring(0, 8));
        record.setOrderNo(request.getOrderNo());
        record.setOrderId(request.getOrderId());
        record.setSignatureType(SignatureType.ELECTRONIC.getCode());
        record.setSignerName(request.getSignerName());
        record.setSignerPhone(request.getSignerPhone());
        record.setSignatureData(request.getSignatureData());
        record.setLocation(request.getLocation());
        record.setLatitude(request.getLatitude());
        record.setLongitude(request.getLongitude());
        record.setDeviceId(request.getDeviceId());
        record.setDeliveryPersonId(request.getDeliveryPersonId());
        record.setDeliveryPersonName(request.getDeliveryPersonName());
        record.setSignTime(LocalDateTime.now());
        record.setStatus(1);
        record.setRemark(request.getRemark());

        String dataHash = SecureUtil.md5(request.getSignatureData());
        record.setIntegrityHash(generateIntegrityHash(record, dataHash));
        record.setVerified(false);

        signatureMapper.insert(record);
        return record;
    }

    private String generateIntegrityHash(SignatureRecord record, String dataHash) {
        String hashSource = record.getOrderNo() + "|" +
                            record.getSignerName() + "|" +
                            record.getSignTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" +
                            dataHash;
        return SecureUtil.sha256(hashSource);
    }

    @Override
    public SignatureRecord getSignatureById(Long id) {
        return signatureMapper.selectById(id);
    }

    @Override
    public SignatureRecord getLatestByOrderNo(String orderNo) {
        return signatureMapper.selectLatestByOrderNo(orderNo);
    }

    @Override
    public Page<SignatureRecord> listSignatures(Integer page, Integer size, String orderNo, String signatureType) {
        Page<SignatureRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SignatureRecord> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.eq(SignatureRecord::getOrderNo, orderNo);
        }
        if (signatureType != null && !signatureType.isEmpty()) {
            wrapper.eq(SignatureRecord::getSignatureType, signatureType);
        }
        wrapper.orderByDesc(SignatureRecord::getSignTime);
        return signatureMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public IntegrityVerifyResult verifyIntegrity(Long id) {
        SignatureRecord record = signatureMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("签收记录不存在: " + id);
        }

        IntegrityVerifyResult result = new IntegrityVerifyResult();
        result.setSignatureId(id);
        result.setVerifyTime(LocalDateTime.now());
        result.setOriginalHash(record.getIntegrityHash());

        String currentDataHash;
        if (SignatureType.PHOTO.getCode().equals(record.getSignatureType())) {
            currentDataHash = record.getPhotoPath();
        } else {
            currentDataHash = SecureUtil.md5(record.getSignatureData());
        }

        String currentHash = generateIntegrityHash(record, currentDataHash);
        result.setCurrentHash(currentHash);

        boolean verified = record.getIntegrityHash().equals(currentHash);
        result.setVerified(verified);
        result.setVerifyMessage(verified ? "签收数据完整性验证通过" : "签收数据可能被篡改");

        record.setVerified(verified);
        record.setVerifyTime(LocalDateTime.now());
        if (verified) {
            record.setStatus(2);
        }
        signatureMapper.updateById(record);

        return result;
    }

    @Override
    public Page<SignatureRecord> listByDeliveryPerson(String deliveryPersonId, Integer page, Integer size) {
        Page<SignatureRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SignatureRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignatureRecord::getDeliveryPersonId, deliveryPersonId)
               .orderByDesc(SignatureRecord::getSignTime);
        return signatureMapper.selectPage(pageObj, wrapper);
    }
}