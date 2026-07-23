package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.model.MallNotice;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 商城公告服务接口
 */
public interface MallNoticeService {

    /**
     * 分页查询公告（管理后台）
     */
    IPage<MallNotice> pageNotices(Integer pageNum, Integer pageSize, String title, Integer noticeType, Integer status);

    /**
     * 查询公告详情
     */
    MallNotice getNotice(Long id);

    /**
     * 创建公告（草稿）
     */
    void createNotice(MallNotice notice);

    /**
     * 更新公告
     */
    void updateNotice(MallNotice notice);

    /**
     * 删除公告
     */
    void deleteNotice(Long id);

    /**
     * 发布公告（回写发布时间）
     */
    void publishNotice(Long id);

    /**
     * 下线公告
     */
    void offlineNotice(Long id);

    /**
     * 查询已发布公告（商城端公开列表，按排序+发布时间倒序）
     */
    List<MallNotice> listPublished(Integer limit);
}
