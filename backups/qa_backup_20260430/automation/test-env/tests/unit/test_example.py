"""
示例测试用例 - 展示测试用例编写规范和最佳实践

此文件演示如何编写规范的单元测试用例，包含：
1. 测试类结构
2. 测试方法命名规范
3. 断言使用
4. 夹具使用
5. 测试数据管理
6. 错误处理
"""

import pytest
from typing import Dict, Any
from optimized_framework.core.base_test import BaseTest


class TestExample(BaseTest):
    """
    示例测试类
    
    命名规范：
    - 类名以 'Test' 开头
    - 描述测试的功能模块
    """
    
    @classmethod
    def setup_class(cls):
        """类级别设置 - 在所有测试方法之前执行一次"""
        print("\n=== 测试类设置 ===")
        cls.shared_data = {"class_counter": 0}
    
    @classmethod
    def teardown_class(cls):
        """类级别清理 - 在所有测试方法之后执行一次"""
        print("\n=== 测试类清理 ===")
        cls.shared_data = None
    
    def setup_method(self):
        """方法级别设置 - 在每个测试方法之前执行"""
        print(f"\n--- 测试方法设置: {self._testMethodName} ---")
        self.test_data = {
            "counter": 0,
            "message": "Hello, Test!"
        }
        self.shared_data["class_counter"] += 1
    
    def teardown_method(self):
        """方法级别清理 - 在每个测试方法之后执行"""
        print(f"\n--- 测试方法清理: {self._testMethodName} ---")
        self.test_data = None
    
    # ========== 测试方法示例 ==========
    
    def test_addition(self):
        """测试基本加法功能
        
        命名规范：
        - 方法名以 'test_' 开头
        - 描述要测试的具体行为
        """
        # Arrange (准备)
        a = 5
        b = 3
        
        # Act (执行)
        result = a + b
        
        # Assert (断言)
        assert result == 8, f"预期 5 + 3 = 8，实际得到 {result}"
        assert isinstance(result, int), f"结果应为整数，实际类型为 {type(result)}"
    
    def test_string_operations(self):
        """测试字符串操作"""
        # 测试字符串连接
        assert "Hello" + " " + "World" == "Hello World"
        
        # 测试字符串长度
        text = "测试字符串"
        assert len(text) == 5, f"字符串长度应为5，实际为 {len(text)}"
        
        # 测试字符串方法
        assert "HELLO".lower() == "hello"
        assert "hello".upper() == "HELLO"
        assert "  hello  ".strip() == "hello"
    
    @pytest.mark.parametrize("input_a, input_b, expected", [
        (1, 2, 3),
        (0, 0, 0),
        (-1, 1, 0),
        (100, 200, 300),
    ])
    def test_addition_with_parameters(self, input_a, input_b, expected):
        """参数化测试示例
        
        使用 @pytest.mark.parametrize 装饰器
        可以一次测试多组输入输出
        """
        result = input_a + input_b
        assert result == expected, f"{input_a} + {input_b} 应等于 {expected}，实际得到 {result}"
    
    def test_exception_handling(self):
        """测试异常处理"""
        # 测试预期异常
        with pytest.raises(ZeroDivisionError) as exc_info:
            result = 1 / 0
        
        # 验证异常信息
        assert str(exc_info.value) == "division by zero"
        
        # 测试不应抛出异常的情况
        try:
            result = 1 / 1
            assert result == 1
        except ZeroDivisionError:
            pytest.fail("不应抛出 ZeroDivisionError 异常")
    
    def test_with_fixtures(self, logger_fixture, temp_dir):
        """测试夹具使用
        
        夹具通过参数注入到测试方法中
        """
        # 使用日志夹具
        logger_fixture.info("测试开始 - 使用夹具")
        
        # 使用临时目录夹具
        test_file = temp_dir / "test.txt"
        test_file.write_text("测试数据")
        
        assert test_file.exists(), "测试文件应已创建"
        assert test_file.read_text() == "测试数据", "文件内容不正确"
        
        logger_fixture.info("测试结束 - 夹具使用成功")
    
    def test_data_structures(self):
        """测试数据结构操作"""
        # 列表测试
        numbers = [1, 2, 3, 4, 5]
        assert len(numbers) == 5
        assert numbers[0] == 1
        assert numbers[-1] == 5
        assert sum(numbers) == 15
        
        # 字典测试
        person = {"name": "张三", "age": 30, "city": "北京"}
        assert person["name"] == "张三"
        assert "age" in person
        assert len(person) == 3
        
        # 集合测试
        set_a = {1, 2, 3}
        set_b = {3, 4, 5}
        assert set_a.union(set_b) == {1, 2, 3, 4, 5}
        assert set_a.intersection(set_b) == {3}
    
    @pytest.mark.slow
    def test_slow_operation(self):
        """标记为慢速测试
        
        使用 @pytest.mark.slow 装饰器标记
        可以通过 pytest -m "not slow" 排除慢速测试
        """
        import time
        
        start_time = time.time()
        time.sleep(0.1)  # 模拟慢速操作
        end_time = time.time()
        
        duration = end_time - start_time
        assert duration >= 0.1, f"操作应至少持续0.1秒，实际持续 {duration:.3f}秒"
    
    def test_skip_example(self):
        """跳过测试示例"""
        import sys
        
        if sys.version_info < (3, 8):
            pytest.skip("需要Python 3.8或更高版本")
        
        # 只有Python 3.8+才会执行下面的代码
        assert True
    
    @pytest.mark.xfail
    def test_expected_failure(self):
        """预期失败的测试
        
        使用 @pytest.mark.xfail 标记预期会失败的测试
        如果测试通过，会被标记为"意外通过"
        """
        # 这个测试预期会失败
        assert 1 == 2, "这是一个预期会失败的测试"


class TestAdvancedFeatures:
    """高级功能测试示例"""
    
    @pytest.fixture
    def complex_data(self):
        """自定义夹具示例"""
        data = {
            "users": [
                {"id": 1, "name": "张三", "active": True},
                {"id": 2, "name": "李四", "active": False},
                {"id": 3, "name": "王五", "active": True},
            ],
            "metadata": {
                "total": 3,
                "active_count": 2,
                "inactive_count": 1,
            }
        }
        return data
    
    def test_custom_fixture(self, complex_data):
        """使用自定义夹具"""
        users = complex_data["users"]
        metadata = complex_data["metadata"]
        
        assert len(users) == metadata["total"]
        
        active_users = [user for user in users if user["active"]]
        assert len(active_users) == metadata["active_count"]
        
        inactive_users = [user for user in users if not user["active"]]
        assert len(inactive_users) == metadata["inactive_count"]


# ========== 测试执行说明 ==========
"""
运行测试：
1. 运行单个测试文件：
   pytest tests/unit/test_example.py -v
   
2. 运行特定测试类：
   pytest tests/unit/test_example.py::TestExample -v
   
3. 运行特定测试方法：
   pytest tests/unit/test_example.py::TestExample::test_addition -v
   
4. 运行标记的测试：
   pytest tests/unit/test_example.py -m "slow" -v
   
5. 排除标记的测试：
   pytest tests/unit/test_example.py -m "not slow" -v
   
6. 生成HTML报告：
   pytest tests/unit/test_example.py --html=reports/example.html
   
7. 生成Allure报告：
   pytest tests/unit/test_example.py --alluredir=reports/allure
   allure serve reports/allure
"""