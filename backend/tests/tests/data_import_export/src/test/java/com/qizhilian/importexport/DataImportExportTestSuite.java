package com.qizhilian.importexport;

import org.junit.platform.suite.api.*;

/**
 * 数据导入导出测试套件
 * 运行所有导入导出相关测试
 */
@Suite
@SuiteDisplayName("企智连数据导入导出测试套件")
@SelectClasses({
    ExcelImportTest.class,
    ExcelExportTest.class,
    JsonXmlImportExportTest.class
})
@IncludeTags({"import", "export", "json", "xml", "validation", "performance"})
public class DataImportExportTestSuite {
    // 测试套件类，用于批量运行所有导入导出测试
}
