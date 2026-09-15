package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.model.MallKeyword;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 商城搜索关键词服务接口
 */
public interface MallKeywordService {

    /**
     * 分页查询关键词
     *
     * @param remark 备注模糊查询（可空，为空不过滤）
     */
    IPage<MallKeyword> pageKeywords(Integer pageNum, Integer pageSize, String keyword, Integer keywordType, Integer status,
                                   String remark);

    /**
     * 新增关键词（同租户下关键词唯一）
     */
    void createKeyword(MallKeyword keyword);

    /**
     * 更新关键词
     */
    void updateKeyword(MallKeyword keyword);

    /**
     * 删除关键词
     */
    void deleteKeyword(Long id);

    /**
     * 启用/禁用关键词
     */
    void toggleStatus(Long id, Integer status);
}
