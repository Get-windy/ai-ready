package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.PriceStrategyDTO;
import cn.aiedge.erp.sale.entity.PriceStrategy;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 价格策略服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface IPriceStrategyService extends IService<PriceStrategy> {

    /**
     * 创建价格策略
     *
     * @param dto 策略DTO
     * @return 策略ID
     */
    Long createStrategy(PriceStrategyDTO dto);

    /**
     * 更新价格策略
     *
     * @param id 策略ID
     * @param dto 策略DTO
     */
    void updateStrategy(Long id, PriceStrategyDTO dto);

    /**
     * 删除价格策略
     *
     * @param id 策略ID
     */
    void deleteStrategy(Long id);

    /**
     * 获取策略详情
     *
     * @param id 策略ID
     * @return 策略DTO
     */
    PriceStrategyDTO getStrategyDetail(Long id);

    /**
     * 分页查询策略列表
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param name 策略名称
     * @param status 状态
     * @param strategyType 策略类型
     * @return 分页结果
     */
    Page<PriceStrategyDTO> pageStrategies(Page<PriceStrategyDTO> page, Long tenantId,
                                          String name, String status, String strategyType);

    /**
     * 查询生效中的策略列表
     *
     * @return 策略列表
     */
    List<PriceStrategyDTO> getActiveStrategies();

    /**
     * 启用策略
     *
     * @param id 策略ID
     */
    void activateStrategy(Long id);

    /**
     * 停用策略
     *
     * @param id 策略ID
     */
    void deactivateStrategy(Long id);

    /**
     * 复制策略
     *
     * @param id 原策略ID
     * @return 新策略ID
     */
    Long copyStrategy(Long id);
}
