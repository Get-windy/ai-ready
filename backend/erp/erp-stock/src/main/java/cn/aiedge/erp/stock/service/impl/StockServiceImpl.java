package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.controller.initial.InitialStockDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.WarehouseMapper;
import cn.aiedge.erp.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存管理ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Autowired
    private StockMapper stockMapper;

    /** 用于补齐库存行的展示快照列（商品名/编码/单位）——见 {@link #fillSnapshot} */
    @Autowired
    private ProductMapper productMapper;

    /** 用于补齐库存行的展示快照列（仓库名）——见 {@link #fillSnapshot} */
    @Autowired
    private WarehouseMapper warehouseMapper;

    /**
     * 默认库存预警阈值（从配置文件读取）
     */
    @Value("${stock.alert.low-stock-threshold:10}")
    private Integer defaultLowStockThreshold;

    @Override
    public Stock getStockDetail(Long productId, Long warehouseId) {
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .eq("warehouse_id", warehouseId)
                .eq("deleted", 0);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean increaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null) {
            // 如果库存记录不存在，创建新的库存记录
            stock = new Stock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stock.setAvailableQuantity(quantity);
            stock.setFrozenQuantity(BigDecimal.ZERO);
            // 本方法只收 id（调用方如 wms 的 ERP 轨镜像无从传名称）⇒ 快照必须自己查档案补
            fillSnapshot(stock);
            return this.save(stock);
        } else {
            // 更新现有库存
            BigDecimal newQuantity = stock.getQuantity().add(quantity);
            BigDecimal newAvailableQuantity = stock.getAvailableQuantity().add(quantity);
            stock.setQuantity(newQuantity);
            stock.setAvailableQuantity(newAvailableQuantity);
            // 顺带自愈历史空快照行（已完整的行不查库、不改值）
            fillSnapshot(stock);
            return this.updateById(stock);
        }
    }

    @Override
    public boolean decreaseStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getAvailableQuantity().compareTo(quantity) < 0) {
            return false; // 库存不足
        }

        BigDecimal newQuantity = stock.getQuantity().subtract(quantity);
        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().subtract(quantity);
        stock.setQuantity(newQuantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean recordStockIn(Stock movement) {
        if (movement == null || movement.getQuantity() == null
                || movement.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                || movement.getProductId() == null || movement.getWarehouseId() == null) {
            return false;
        }
        Long productId = movement.getProductId();
        Long warehouseId = movement.getWarehouseId();
        BigDecimal qty = movement.getQuantity();
        BigDecimal cost = movement.getUnitPrice() != null ? movement.getUnitPrice() : BigDecimal.ZERO;

        QueryWrapper<Stock> qw = new QueryWrapper<>();
        qw.eq("product_id", productId)
                .eq("warehouse_id", warehouseId)
                .eq("deleted", 0);
        if (StringUtils.hasText(movement.getBatchNo())) qw.eq("batch_no", movement.getBatchNo());
        if (movement.getValidityDate() != null) qw.eq("validity_date", movement.getValidityDate());
        Stock exist = this.getOne(qw, false);

        if (exist == null) {
            Stock s = new Stock();
            s.setProductId(productId);
            s.setProductCode(movement.getProductCode());
            s.setProductName(movement.getProductName());
            s.setWarehouseId(warehouseId);
            s.setWarehouseName(movement.getWarehouseName());
            s.setQuantity(qty);
            s.setAvailableQuantity(qty);
            s.setFrozenQuantity(BigDecimal.ZERO);
            s.setUnit(movement.getUnit());
            s.setUnitPrice(cost);
            s.setBatchNo(movement.getBatchNo());
            s.setProductionDate(movement.getProductionDate());
            s.setValidityDate(movement.getValidityDate());
            s.setIsInitial(0);
            // 调用方漏传的名称/编码/单位在这里兜住（单据传了的以单据为准）
            fillSnapshot(s);
            return this.save(s);
        }

        BigDecimal oldQty = exist.getQuantity() != null ? exist.getQuantity() : BigDecimal.ZERO;
        BigDecimal oldPrice = exist.getUnitPrice() != null ? exist.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal newQty = oldQty.add(qty);
        // 移动加权平均成本核定：新单价 = (旧数量×旧单价 + 本次数量×核定单价) / 新数量
        BigDecimal newPrice = cost;
        if (oldQty.signum() > 0) {
            BigDecimal totalCost = oldQty.multiply(oldPrice).add(qty.multiply(cost));
            if (totalCost.signum() > 0 && newQty.signum() > 0) {
                newPrice = totalCost.divide(newQty, 4, RoundingMode.HALF_UP);
            }
        }
        exist.setQuantity(newQty);
        BigDecimal oldAvail = exist.getAvailableQuantity() != null ? exist.getAvailableQuantity() : BigDecimal.ZERO;
        exist.setAvailableQuantity(oldAvail.add(qty));
        exist.setUnitPrice(newPrice);
        if (exist.getProductCode() == null) exist.setProductCode(movement.getProductCode());
        if (exist.getProductName() == null) exist.setProductName(movement.getProductName());
        if (exist.getUnit() == null) exist.setUnit(movement.getUnit());
        // 仓库名等仍未补齐的列回档案取（历史空快照行在此自愈）
        fillSnapshot(exist);
        return this.updateById(exist);
    }

    @Override
    public boolean recordStockOut(Stock movement) {
        if (movement == null || movement.getQuantity() == null
                || movement.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                || movement.getProductId() == null || movement.getWarehouseId() == null) {
            return false;
        }
        Long productId = movement.getProductId();
        Long warehouseId = movement.getWarehouseId();
        BigDecimal qty = movement.getQuantity();

        QueryWrapper<Stock> qw = new QueryWrapper<>();
        qw.eq("product_id", productId)
                .eq("warehouse_id", warehouseId)
                .eq("deleted", 0);
        if (StringUtils.hasText(movement.getBatchNo())) qw.eq("batch_no", movement.getBatchNo());
        Stock exist = this.getOne(qw, false);
        if (exist == null) return false;

        BigDecimal avail = exist.getAvailableQuantity() != null ? exist.getAvailableQuantity() : BigDecimal.ZERO;
        if (avail.compareTo(qty) < 0) return false; // 库存不足
        exist.setQuantity(exist.getQuantity().subtract(qty));
        exist.setAvailableQuantity(avail.subtract(qty));
        return this.updateById(exist);
    }

    @Override
    public boolean freezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getAvailableQuantity().compareTo(quantity) < 0) {
            return false; // 可用库存不足
        }

        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().subtract(quantity);
        BigDecimal newFrozenQuantity = stock.getFrozenQuantity().add(quantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        stock.setFrozenQuantity(newFrozenQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean unfreezeStock(Long productId, Long warehouseId, java.math.BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null || stock.getFrozenQuantity().compareTo(quantity) < 0) {
            return false; // 冻结库存不足
        }

        BigDecimal newAvailableQuantity = stock.getAvailableQuantity().add(quantity);
        BigDecimal newFrozenQuantity = stock.getFrozenQuantity().subtract(quantity);
        stock.setAvailableQuantity(newAvailableQuantity);
        stock.setFrozenQuantity(newFrozenQuantity);
        return this.updateById(stock);
    }

    @Override
    public boolean checkStock(Long productId, Long warehouseId, java.math.BigDecimal actualQuantity) {
        if (actualQuantity == null || actualQuantity.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        Stock stock = getStockDetail(productId, warehouseId);
        if (stock == null) {
            stock = new Stock();
            stock.setProductId(productId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(actualQuantity);
            stock.setAvailableQuantity(actualQuantity);
            stock.setFrozenQuantity(BigDecimal.ZERO);
            // 盘点调整是「凭空建行」的高发路径（StockTakeServiceImpl 只传 id）⇒ 同样要补快照
            fillSnapshot(stock);
            return this.save(stock);
        } else {
            stock.setQuantity(actualQuantity);
            stock.setAvailableQuantity(actualQuantity.subtract(stock.getFrozenQuantity()));
            fillSnapshot(stock);
            return this.updateById(stock);
        }
    }

    @Override
    public List<Stock> checkStockAlert() {
        // 使用默认配置的预警阈值
        Integer alertThreshold = defaultLowStockThreshold;

        // 查询库存数量低于预警阈值的商品
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("available_quantity", alertThreshold)
                .eq("deleted", 0)
                .orderByAsc("available_quantity");

        return this.list(queryWrapper);
    }

    @Override
    public Stock getStockByProductId(Long productId) {
        QueryWrapper<Stock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId)
                .eq("deleted", 0)
                .orderByDesc("create_time")
                .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    /**
     * 补齐库存行的**展示快照列**（`product_code` / `product_name` / `unit` / `warehouse_name`）。
     *
     * <p><b>为什么需要它（STK-BREAK-04，2026-09-21）</b>：`erp_stock` 有一组冗余的展示列
     * （商品名/编码、仓库名）。实测存量 4 行的这些列**全部为 NULL**，原因是写入口对调用方
     * 的"自觉"依赖不一致：`recordStockIn` / `saveInitialStock` 会带齐这些值（调用方
     * 如报溢单 {@code StockOverflowServiceImpl} 确实传了），而 `increaseStock` / `checkStock`
     * 只收 {@code productId + warehouseId + quantity} —— 调用方（如
     * {@code wms.InventoryServiceImpl} 的 ERP 轨镜像、盘点调整）**根本无从传入**，
     * 于是新建出来的行天然是空快照。</p>
     *
     * <p><b>口径</b>：只补**空**列，绝不覆盖调用方已给的值（调用方给的可能是单据当时的值，
     * 属权威值）。因此本方法对"已经完整的行"零成本（不查库、不改值），对历史 NULL 行则
     * 顺带**自愈**（下次该行被任何写路径碰过即补齐）。</p>
     *
     * <p>不处理 {@code supplier_id}/{@code supplier_name}：这两个值无法从商品/仓库档案推导，
     * 只能由单据传入，而当前**没有任何调用方传**（属功能缺口，不是本方法能兜的），
     * 已在 MASTER_TODO 登记。</p>
     */
    private void fillSnapshot(Stock stock) {
        if (stock == null) {
            return;
        }
        boolean needProduct = stock.getProductId() != null
                && (!StringUtils.hasText(stock.getProductCode())
                    || !StringUtils.hasText(stock.getProductName())
                    || !StringUtils.hasText(stock.getUnit()));
        if (needProduct) {
            Product product = productMapper.selectById(stock.getProductId());
            if (product != null) {
                if (!StringUtils.hasText(stock.getProductCode())) {
                    stock.setProductCode(product.getProductCode());
                }
                if (!StringUtils.hasText(stock.getProductName())) {
                    stock.setProductName(product.getProductName());
                }
                if (!StringUtils.hasText(stock.getUnit())) {
                    stock.setUnit(product.getUnit());
                }
            }
        }
        if (stock.getWarehouseId() != null && !StringUtils.hasText(stock.getWarehouseName())) {
            Warehouse warehouse = warehouseMapper.selectById(stock.getWarehouseId());
            if (warehouse != null) {
                stock.setWarehouseName(warehouse.getWarehouseName());
            }
        }
    }

    /**
     * 保存期初库存（按「商品 + 仓库」幂等 upsert）
     * <p>
     * ⚠️ 历史实现（2026-09-18 修复）有两处缺陷：
     * ① **先 `remove(is_initial = 1)` 清空全部期初行再插入** —— 页面上「录入一条期初」会把其它商品/仓库
     *    已录的期初全部删掉（静默数据丢失）。现改为按 (productId, warehouseId) 判断：
     *    同商品同仓库已有期初行则更新，否则新增，实现幂等。
     * ② **显式 `setTenantId(0L)`** —— 多租户插件的 `ignoreInsert` 在「INSERT 列清单已含 tenant_id」时
     *    不再补列，此时租户值完全依赖手写值；虽然当前 `MetaObjectHandler.insertFill` 会按当前会话
     *    覆盖 tenantId（故历史行 tenant_id 实测为 1，未复现落 0），但显式写 0 属危险冗余，
     *    一旦填充链路被跳过即产生「保存成功却查不到」的跨租户脏数据。现交由租户链路统一注入。
     */
    @Override
    public void saveInitialStock(List<InitialStockDTO> initialStockList) {
        if (initialStockList == null || initialStockList.isEmpty()) {
            return;
        }
        for (InitialStockDTO dto : initialStockList) {
            // 同商品 + 同仓库已存在期初行 → 走更新，避免重复期初行
            Stock existingStock = findInitialStock(dto.getProductId(), dto.getWarehouseId());
            if (existingStock != null) {
                applyInitialStockFields(existingStock, dto);
                this.updateById(existingStock);
                continue;
            }

            Stock stock = new Stock();
            applyInitialStockFields(stock, dto);
            stock.setIsInitial(1); // 标记为期初库存
            stock.setFrozenQuantity(BigDecimal.ZERO); // 期初无冻结库存
            stock.setDeleted(0); // 未删除状态
            // 前端若只传了 id 没传名称，在这里回档案补齐 ——
            // 期初库存列表的「仓库」列（InitialStockQueryMapper:60）是**直接取快照**的，不补就是空白列
            fillSnapshot(stock);

            this.save(stock);
        }
    }

    @Override
    public void updateInitialStock(InitialStockDTO initialStockDTO) {
        if (initialStockDTO == null || initialStockDTO.getId() == null) {
            return;
        }
        // 更新指定的期初库存数据（只允许改期初行）
        Stock existingStock = this.getById(initialStockDTO.getId());
        if (existingStock == null || existingStock.getIsInitial() == null || existingStock.getIsInitial() != 1) {
            return;
        }
        // ⚠️ 用 LambdaUpdateWrapper 显式 set 每一列：MyBatis-Plus 的 updateById **忽略 null 字段**，
        //    会导致「清空生产日期 / 有效期至 / 备注」静默失效（改了等于没改）。
        //    这里 null 也会被写入，因此「清空」是本页可表达的语义。
        this.lambdaUpdate()
                .eq(Stock::getId, initialStockDTO.getId())
                .set(Stock::getProductId, initialStockDTO.getProductId())
                .set(Stock::getProductCode, initialStockDTO.getProductCode())
                .set(Stock::getProductName, initialStockDTO.getProductName())
                .set(Stock::getWarehouseId, initialStockDTO.getWarehouseId())
                .set(Stock::getWarehouseName, initialStockDTO.getWarehouseName())
                .set(Stock::getUnit, initialStockDTO.getUnit())
                .set(Stock::getQuantity, initialStockDTO.getQuantity())
                .set(Stock::getUnitPrice, initialStockDTO.getUnitPrice())
                // 期初数量全部计入可用数量（与新增口径一致）
                .set(Stock::getAvailableQuantity, initialStockDTO.getQuantity())
                .set(Stock::getProductionDate, initialStockDTO.getProductionDate())
                .set(Stock::getValidityDate, initialStockDTO.getValidityDate())
                .set(Stock::getRemark, initialStockDTO.getRemark())
                // updateTime 的自动填充只在 updateById 链路生效，此处手工补齐
                .set(Stock::getUpdateTime, LocalDateTime.now())
                .update();
    }

    @Override
    public boolean deleteInitialStock(Long id) {
        if (id == null) {
            return false;
        }
        // 只允许删除期初行：非期初的日常库存行不允许从本页删除（否则会破坏日常库存台账）
        Stock existingStock = this.getById(id);
        if (existingStock == null
                || existingStock.getIsInitial() == null
                || existingStock.getIsInitial() != 1) {
            return false;
        }
        return this.removeById(id);
    }

    /**
     * 查询某商品在某仓库的期初库存行（未找到返回 null）
     */
    private Stock findInitialStock(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(Stock::getIsInitial, 1)
                .eq(Stock::getProductId, productId)
                .eq(Stock::getWarehouseId, warehouseId)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 把 DTO 的期初字段写入库存实体（新增链路使用）
     */
    private void applyInitialStockFields(Stock stock, InitialStockDTO dto) {
        stock.setProductId(dto.getProductId());
        stock.setProductCode(dto.getProductCode());
        stock.setProductName(dto.getProductName());
        stock.setWarehouseId(dto.getWarehouseId());
        stock.setWarehouseName(dto.getWarehouseName());
        stock.setUnit(dto.getUnit());
        stock.setQuantity(dto.getQuantity());
        stock.setUnitPrice(dto.getUnitPrice());
        stock.setProductionDate(dto.getProductionDate());
        stock.setValidityDate(dto.getValidityDate());
        stock.setRemark(dto.getRemark());
        // 期初数量全部计入可用数量（期初不设冻结）
        stock.setAvailableQuantity(dto.getQuantity());
    }
}