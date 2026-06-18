package com.qizhilian.api;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 测试套件运行器
 * 用于批量执行所有API测试
 */
@Suite
@SuiteDisplayName("企智连API自动化测试套件")
@SelectPackages({
    "com.qizhilian.api.auth",
    "com.qizhilian.api.erp",
    "com.qizhilian.api.crm",
    "com.qizhilian.api.system"
})
public class TestRunner {
    // 测试套件配置
    // 运行此类将执行所有选中包下的测试
}
