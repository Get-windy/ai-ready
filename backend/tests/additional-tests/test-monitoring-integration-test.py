                    "passed": 0,
                    "failed": 0,
                    "error": 0,
                    "skipped": 0,
                    "pass_rate": 0
                }
            
            by_category[category]["total"] += 1
            by_category[category][result["result"]] += 1
            
            # 计算通过率
            category_total = by_category[category]["total"]
            category_passed = by_category[category]["passed"]
            if category_total > 0:
                by_category[category]["pass_rate"] = (category_passed / category_total) * 100
        
        return {
            "total_tests": total_tests,
            "passed": passed,
            "failed": failed,
            "error": error,
            "skipped": skipped,
            "pass_rate": pass_rate,
            "by_category": by_category
        }


def main():
    """主函数"""
    print("AI-Ready 测试监控系统集成测试")
    print("版本: 1.0.0")
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 检查服务是否运行
    print("检查测试监控服务是否运行...")
    try:
        response = requests.get("http://localhost:8100/health", timeout=5)
        if response.status_code != 200:
            print("❌ 测试监控服务未运行或不可用")
            print("请先启动测试监控系统:")
            print("  1. 运行 start_test_monitoring.bat")
            print("  2. 或执行: docker-compose up -d")
            sys.exit(1)
        else:
            print("✅ 测试监控服务运行正常")
    except Exception as e:
        print(f"❌ 无法连接到测试监控服务: {str(e)}")
        print("请先启动测试监控系统")
        sys.exit(1)
    
    # 运行集成测试
    runner = TestMonitoringIntegrationRunner()
    report = runner.run_all_tests()
    
    # 根据测试结果返回退出码
    summary = report["summary"]
    if summary["failed"] > 0 or summary["error"] > 0:
        print("\n❌ 集成测试失败，请检查详细报告")
        sys.exit(1)
    elif summary["passed"] == summary["total_tests"]:
        print("\n✅ 所有集成测试通过！")
        sys.exit(0)
    else:
        print("\n⚠️ 集成测试有警告，但无失败")
        sys.exit(0)


if __name__ == "__main__":
    main()