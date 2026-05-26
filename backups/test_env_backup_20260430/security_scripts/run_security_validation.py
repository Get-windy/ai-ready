#!/usr/bin/env python3
"""
运行安全验证脚本
简化版的安全验证执行脚本
"""

import os
import sys
import logging
from datetime import datetime

# 添加当前目录到Python路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

def setup_logging():
    """设置日志"""
    log_dir = os.path.join(os.path.dirname(__file__), "..", "logs", "security")
    os.makedirs(log_dir, exist_ok=True)
    
    log_file = os.path.join(log_dir, f"run_security_validation_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file, encoding='utf-8'),
            logging.StreamHandler(sys.stdout)
        ]
    )
    
    return logging.getLogger(__name__)

def main():
    """主函数"""
    logger = setup_logging()
    
    print("=" * 60)
    print("Sprint 27+1 测试环境安全配置验证脚本")
    print("=" * 60)
    
    try:
        # 导入主验证模块
        from security_validation_main import SecurityValidationMain
        
        # 获取测试环境根目录
        test_env_root = os.path.join(os.path.dirname(__file__), "..")
        
        logger.info(f"开始安全验证，测试环境: {test_env_root}")
        
        # 创建验证器
        validator = SecurityValidationMain(test_env_root)
        
        # 运行所有检查
        logger.info("执行安全验证检查...")
        results = validator.run_all_checks()
        
        # 保存结果
        logger.info("保存验证结果...")
        output_file = validator.save_results()
        
        # 打印摘要
        validator.print_summary()
        
        # 生成报告
        logger.info("生成安全报告...")
        if 'overall' in results:
            overall_status = results['overall']['status']
            
            print("\n" + "=" * 60)
            print("安全验证完成!")
            print("=" * 60)
            
            if output_file:
                print(f"结果文件: {output_file}")
            
            print(f"总体状态: {overall_status}")
            print(f"日志文件: 查看 security_scripts/logs/security/ 目录")
            print(f"报告文件: 查看 security_scripts/reports/security/ 目录")
            
            # 根据状态返回退出码
            if overall_status == 'FAILED':
                print("\n⚠️  发现安全风险，请立即处理!")
                return 1
            elif overall_status == 'WARNING':
                print("\n⚠️  存在安全警告，建议处理")
                return 0
            else:
                print("\n✅  安全验证通过")
                return 0
        
        else:
            print("\n❌  安全验证失败，未生成结果")
            return 2
    
    except ImportError as e:
        logger.error(f"导入模块失败: {e}")
        print(f"\n❌  模块导入失败: {e}")
        print("请确保所有依赖模块都已正确创建")
        return 3
    
    except Exception as e:
        logger.error(f"执行失败: {e}", exc_info=True)
        print(f"\n❌  执行失败: {e}")
        return 4

if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)