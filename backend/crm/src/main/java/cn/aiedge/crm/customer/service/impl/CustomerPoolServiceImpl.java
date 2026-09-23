package cn.aiedge.crm.customer.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.entity.CustomerPool;
import cn.aiedge.crm.customer.enums.PoolReason;
import cn.aiedge.crm.customer.enums.PoolStatus;
import cn.aiedge.crm.customer.enums.PoolType;
import cn.aiedge.crm.customer.mapper.CustomerFollowUpMapper;
import cn.aiedge.crm.customer.mapper.CustomerPoolMapper;
import cn.aiedge.crm.customer.mapper.CustomerMapper;
import cn.aiedge.crm.customer.service.CustomerPoolService;
import cn.aiedge.crm.customer.service.CustomerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPoolServiceImpl extends ServiceImpl<CustomerPoolMapper, CustomerPool> implements CustomerPoolService {

    /** 自动回收阈值默认值（天），与前端弹窗默认值、本类 putToPool 的 30 天回收期保持一致 */
    private static final int DEFAULT_NO_FOLLOW_UP_DAYS = 30;

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final CustomerFollowUpMapper customerFollowUpMapper;

    @Override
    public CustomerPool getByCustomerId(Long customerId) {
        return baseMapper.selectLatestByCustomer(customerId);
    }

    @Override
    public Page<CustomerPool> pageList(String keyword, Integer poolType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<CustomerPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerPool::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(CustomerPool::getCustomerCode, keyword)
                    .or().like(CustomerPool::getCustomerName, keyword));
        }
        if (poolType != null) {
            wrapper.eq(CustomerPool::getPoolType, poolType);
        }
        if (status != null) {
            wrapper.eq(CustomerPool::getStatus, status);
        }
        wrapper.orderByDesc(CustomerPool::getPoolTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<CustomerPool> listAvailable() {
        return baseMapper.selectAvailable();
    }

    @Override
    public List<CustomerPool> listByClaimSalesPerson(Long salesPersonId) {
        return baseMapper.selectByClaimSalesPerson(salesPersonId);
    }

    @Override
    public List<CustomerPool> listByOriginalSalesPerson(Long salesPersonId) {
        return baseMapper.selectByOriginalSalesPerson(salesPersonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerPool putToPool(Long customerId, Integer poolReason, String remark) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.notFound("客户不存在");
        }
        CustomerPool pool = new CustomerPool();
        pool.setCustomerId(customerId);
        pool.setCustomerCode(customer.getCustomerCode());
        pool.setCustomerName(customer.getCustomerName());
        pool.setPoolType(PoolType.PUBLIC.getCode());
        pool.setPoolReason(poolReason);
        pool.setOriginalSalesPersonId(customer.getSalesPersonId());
        pool.setOriginalSalesPersonName(customer.getSalesPersonName());
        pool.setOriginalDepartmentId(customer.getDepartmentId());
        pool.setOriginalDepartmentName(customer.getDepartmentName());
        pool.setPoolTime(LocalDateTime.now());
        pool.setPoolDays(0);
        pool.setExpireTime(LocalDateTime.now().plusDays(30));
        pool.setStatus(PoolStatus.AVAILABLE.getCode());
        pool.setRemark(remark);
        save(pool);
        customer.setSalesPersonId(null);
        customer.setSalesPersonName(null);
        customer.setDepartmentId(null);
        customer.setDepartmentName(null);
        customer.setStatus(0);
        customerMapper.updateById(customer);
        return getById(pool.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerPool claimFromPool(Long poolId, Long salesPersonId) {
        CustomerPool pool = getById(poolId);
        if (pool == null) {
            throw BusinessException.notFound("公海池记录不存在");
        }
        if (pool.getStatus() != PoolStatus.AVAILABLE.getCode()) {
            throw BusinessException.badRequest("该客户不可领取");
        }
        pool.setClaimSalesPersonId(salesPersonId);
        pool.setClaimTime(LocalDateTime.now());
        pool.setStatus(PoolStatus.CLAIMED.getCode());
        updateById(pool);
        Customer customer = customerMapper.selectById(pool.getCustomerId());
        customer.setSalesPersonId(salesPersonId);
        customer.setStatus(1);
        customerMapper.updateById(customer);
        return getById(poolId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerPool returnToPool(Long poolId, String remark) {
        CustomerPool pool = getById(poolId);
        if (pool == null) {
            throw BusinessException.notFound("公海池记录不存在");
        }
        CustomerPool newPool = new CustomerPool();
        newPool.setCustomerId(pool.getCustomerId());
        newPool.setCustomerCode(pool.getCustomerCode());
        newPool.setCustomerName(pool.getCustomerName());
        newPool.setPoolType(PoolType.RECOVERY.getCode());
        newPool.setPoolReason(PoolReason.MANUAL_PUT.getCode());
        newPool.setOriginalSalesPersonId(pool.getClaimSalesPersonId());
        newPool.setOriginalSalesPersonName(pool.getClaimSalesPersonName());
        newPool.setOriginalDepartmentId(pool.getClaimDepartmentId());
        newPool.setOriginalDepartmentName(pool.getClaimDepartmentName());
        newPool.setPoolTime(LocalDateTime.now());
        newPool.setPoolDays(0);
        newPool.setExpireTime(LocalDateTime.now().plusDays(30));
        newPool.setStatus(PoolStatus.AVAILABLE.getCode());
        newPool.setRemark(remark);
        save(newPool);
        pool.setStatus(PoolStatus.RETURNED.getCode());
        updateById(pool);
        Customer customer = customerMapper.selectById(pool.getCustomerId());
        customer.setSalesPersonId(null);
        customer.setSalesPersonName(null);
        customer.setStatus(0);
        customerMapper.updateById(customer);
        return getById(newPool.getId());
    }

    /**
     * 长期未跟进自动回收。
     *
     * <p>规则（与前端「执行自动回收」弹窗的文案一致）：把**当前有归属**（{@code sales_person_id} 非空）
     * 且**最近一次跟进日期早于 `今天 - noFollowUpDays`** 的客户放入公海池，回收原因记
     * {@link PoolReason#NO_FOLLOW_UP}。</p>
     *
     * <p>两条刻意的边界，避免误伤：</p>
     * <ol>
     *   <li><b>从未有跟进记录的客户不回收</b>——「无跟进天数」没有可计算的基准；若拿建档时间当基准，
     *       当天新建的客户会被立刻回收。这类客户在日志里单列计数，需要人工判断。</li>
     *   <li><b>已挂在公海池（status=1）的客户跳过</b>——保证反复点「自动回收」不会重复入池。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoRecovery(Integer noFollowUpDays) {
        int days = (noFollowUpDays == null || noFollowUpDays <= 0) ? DEFAULT_NO_FOLLOW_UP_DAYS : noFollowUpDays;
        LocalDate deadline = LocalDate.now().minusDays(days);

        List<Customer> owned = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .isNotNull(Customer::getSalesPersonId));

        int recovered = 0;
        int skippedNoFollowUp = 0;
        for (Customer customer : owned) {
            if (baseMapper.countAvailableByCustomer(customer.getId()) > 0) {
                continue;
            }
            LocalDate lastFollowUp = customerFollowUpMapper.selectMaxFollowUpDate(customer.getId());
            if (lastFollowUp == null) {
                skippedNoFollowUp++;
                continue;
            }
            if (lastFollowUp.isAfter(deadline)) {
                continue;
            }
            putToPool(customer.getId(), PoolReason.NO_FOLLOW_UP.getCode(),
                    "系统自动回收：超过 " + days + " 天无跟进");
            recovered++;
        }
        log.info("自动回收完成：阈值={} 天，扫描有归属客户 {} 个，回收 {} 个，因无跟进记录跳过 {} 个",
                days, owned.size(), recovered, skippedNoFollowUp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkExpired() {
        List<CustomerPool> expiredList = lambdaQuery()
                .eq(CustomerPool::getStatus, PoolStatus.AVAILABLE.getCode())
                .lt(CustomerPool::getExpireTime, LocalDateTime.now())
                .eq(CustomerPool::getDeleted, 0)
                .list();
        for (CustomerPool pool : expiredList) {
            pool.setStatus(PoolStatus.EXPIRED.getCode());
            updateById(pool);
        }
    }

    @Override
    public Integer getAvailableCount() {
        return baseMapper.countAvailable();
    }

    @Override
    public Integer getClaimCountBySalesPerson(Long salesPersonId) {
        return baseMapper.countByClaimSalesPerson(salesPersonId);
    }
}