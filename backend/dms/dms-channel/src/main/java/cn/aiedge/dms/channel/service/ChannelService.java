package cn.aiedge.dms.channel.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.channel.adapter.DeliveryAdapter;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配送渠道管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final DmsChannelMapper channelMapper;
    private final ApplicationContext applicationContext;

    /**
     * 分页查询渠道
     */
    public Page<DmsChannel> page(Page<DmsChannel> page, DmsChannel query) {
        LambdaQueryWrapper<DmsChannel> wrapper = new LambdaQueryWrapper<>(query)
                .orderByAsc(DmsChannel::getPriority);
        return channelMapper.selectPage(page, wrapper);
    }

    /**
     * 获取渠道详情
     */
    public DmsChannel getById(Long id) {
        DmsChannel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.notFound("渠道不存在");
        }
        return channel;
    }

    /**
     * 根据编码获取渠道
     */
    public DmsChannel getByCode(String channelCode) {
        return channelMapper.selectOne(
                new LambdaQueryWrapper<DmsChannel>().eq(DmsChannel::getChannelCode, channelCode));
    }

    /**
     * 获取所有可用渠道（已启用，按优先级排序）
     */
    public List<DmsChannel> getAvailableChannels() {
        return channelMapper.selectList(
                new LambdaQueryWrapper<DmsChannel>()
                        .eq(DmsChannel::getStatus, 1)
                        .orderByAsc(DmsChannel::getPriority));
    }

    /**
     * 获取渠道适配器实例
     */
    public DeliveryAdapter getAdapter(String channelCode) {
        DmsChannel channel = getByCode(channelCode);
        if (channel == null) {
            throw BusinessException.notFound("渠道不存在: " + channelCode);
        }
        if (channel.getAdapterBean() == null || channel.getAdapterBean().isEmpty()) {
            throw BusinessException.badRequest("渠道未配置适配器: " + channelCode);
        }
        return applicationContext.getBean(channel.getAdapterBean(), DeliveryAdapter.class);
    }

    /**
     * 新增渠道
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsChannel create(DmsChannel channel) {
        channel.setTenantId(0L);
        channel.setStatus(0);
        channelMapper.insert(channel);
        log.info("新增配送渠道: id={}, code={}", channel.getId(), channel.getChannelCode());
        return channel;
    }

    /**
     * 更新渠道
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsChannel update(Long id, DmsChannel dto) {
        DmsChannel channel = getById(id);
        channel.setChannelName(dto.getChannelName());
        channel.setChannelType(dto.getChannelType());
        channel.setAdapterBean(dto.getAdapterBean());
        channel.setConfigJson(dto.getConfigJson());
        channel.setPriority(dto.getPriority());
        channel.setSortOrder(dto.getSortOrder());
        channel.setRemark(dto.getRemark());
        channelMapper.updateById(channel);
        return channel;
    }

    /**
     * 更新渠道状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsChannel channel = getById(id);
        channel.setStatus(status);
        channelMapper.updateById(channel);
        log.info("渠道状态更新: id={}, status={}", id, status);
    }

    /**
     * 删除渠道
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        channelMapper.deleteById(id);
        log.info("删除渠道: id={}", id);
    }
}
