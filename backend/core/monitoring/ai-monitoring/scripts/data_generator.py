#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI模型监控数据生成器
用于生成测试数据，验证监控系统功能
"""

import time
import random
import json
import requests
import logging
from datetime import datetime, timedelta
import numpy as np
import threading
import sys
import os

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AIMonitorDataGenerator:
    """AI模型监控数据生成器"""
    
    def __init__(self, monitor_url="http://localhost:8000", model_name="test_model"):
        self.monitor_url = monitor_url
        self.model_name = model_name
        self.running = False
        self.threads = []
        
        # 模拟数据参数
        self.base_accuracy = 0.92
        self.base_latency = 0.15  # 秒
        self.request_rate = 10  # 每秒请求数
        
        # 漂移和异常参数
        self.drift_start_time = None
        self.anomaly_active = False
        self.anomaly_start_time = None
        
        logger.info(f"数据生成器初始化 - 模型: {model_name}, 监控URL: {monitor_url}")
    
    def start(self, generation_interval=30):
        """启动数据生成"""
        self.running = True
        
        # 启动请求生成线程
        request_thread = threading.Thread(target=self._generate_requests, args=(generation_interval,))
        request_thread.daemon = True
        request_thread.start()
        self.threads.append(request_thread)
        
        # 启动质量指标更新线程
        quality_thread = threading.Thread(target=self._generate_quality_metrics, args=(generation_interval * 5,))
        quality_thread.daemon = True
        quality_thread.start()
        self.threads.append(quality_thread)
        
        # 启动漂移检测线程
        drift_thread = threading.Thread(target=self._simulate_drift, args=(generation_interval * 10,))
        drift_thread.daemon = True
        drift_thread.start()
        self.threads.append(drift_thread)
        
        # 启动异常模拟线程
        anomaly_thread = threading.Thread(target=self._simulate_anomalies, args=(generation_interval * 20,))
        anomaly_thread.daemon = True
        anomaly_thread.start()
        self.threads.append(anomaly_thread)
        
        logger.info(f"数据生成器启动，生成间隔: {generation_interval}秒")
        
        # 等待所有线程
        for thread in self.threads:
            thread.join()
    
    def stop(self):
        """停止数据生成"""
        self.running = False
        logger.info("数据生成器停止")
    
    def _generate_requests(self, interval=30):
        """生成模型请求数据"""
        logger.info("开始生成请求数据...")
        
        request_count = 0
        endpoints = ['/predict', '/batch-predict', '/health', '/metrics']
        
        while self.running:
            try:
                # 模拟请求
                endpoint = random.choice(endpoints)
                
                # 模拟延迟（添加一些随机性）
                if self.anomaly_active:
                    # 异常期间增加延迟
                    latency = self.base_latency * random.uniform(3.0, 10.0)
                else:
                    latency = self.base_latency * random.uniform(0.8, 1.2)
                
                # 记录请求到监控系统
                data = {
                    'endpoint': endpoint,
                    'latency_seconds': latency
                }
                
                response = requests.post(
                    f"{self.monitor_url}/record-request",
                    json=data,
                    timeout=5
                )
                
                if response.status_code == 200:
                    request_count += 1
                    if request_count % 100 == 0:
                        logger.info(f"已生成 {request_count} 个请求")
                else:
                    logger.warning(f"记录请求失败: {response.status_code}")
                
                # 控制请求速率
                time.sleep(1.0 / self.request_rate)
                
            except requests.exceptions.RequestException as e:
                logger.error(f"请求监控系统失败: {e}")
                time.sleep(5)  # 等待后重试
            except Exception as e:
                logger.error(f"生成请求数据时出错: {e}")
                time.sleep(1)
    
    def _generate_quality_metrics(self, interval=150):
        """生成质量指标数据"""
        logger.info("开始生成质量指标数据...")
        
        evaluation_count = 0
        
        while self.running:
            try:
                # 模拟质量评估
                if self.drift_start_time:
                    # 漂移期间质量下降
                    time_since_drift = (datetime.now() - self.drift_start_time).total_seconds()
                    drift_factor = min(time_since_drift / 3600, 1.0)  # 1小时内线性下降
                    current_accuracy = self.base_accuracy * (1.0 - drift_factor * 0.3)
                else:
                    current_accuracy = self.base_accuracy
                
                # 添加一些随机波动
                current_accuracy += random.uniform(-0.02, 0.02)
                current_accuracy = max(0.5, min(1.0, current_accuracy))
                
                # 生成模拟的真实标签和预测标签
                sample_count = random.randint(100, 1000)
                y_true = [random.randint(0, 1) for _ in range(sample_count)]
                
                # 基于准确率生成预测标签
                correct_count = int(sample_count * current_accuracy)
                y_pred = []
                
                for i in range(sample_count):
                    if i < correct_count:
                        y_pred.append(y_true[i])  # 正确预测
                    else:
                        y_pred.append(1 - y_true[i])  # 错误预测
                
                # 生成概率预测（用于AUC计算）
                y_prob = []
                for true_label, pred_label in zip(y_true, y_pred):
                    if true_label == pred_label:
                        # 正确预测时，概率接近1
                        prob = random.uniform(0.7, 0.95)
                    else:
                        # 错误预测时，概率接近0.5
                        prob = random.uniform(0.3, 0.6)
                    y_prob.append([1 - prob, prob])
                
                # 更新质量指标到监控系统
                data = {
                    'y_true': y_true,
                    'y_pred': y_pred,
                    'y_prob': y_prob
                }
                
                response = requests.post(
                    f"{self.monitor_url}/update-quality",
                    json=data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    evaluation_count += 1
                    logger.info(f"第 {evaluation_count} 次质量评估完成，准确率: {current_accuracy:.4f}")
                else:
                    logger.warning(f"更新质量指标失败: {response.status_code}")
                
                # 等待指定间隔
                time.sleep(interval)
                
            except requests.exceptions.RequestException as e:
                logger.error(f"请求监控系统失败: {e}")
                time.sleep(10)
            except Exception as e:
                logger.error(f"生成质量指标时出错: {e}")
                time.sleep(5)
    
    def _simulate_drift(self, interval=300):
        """模拟数据漂移"""
        logger.info("开始模拟数据漂移...")
        
        drift_cycles = 0
        
        while self.running:
            try:
                # 随机决定是否开始漂移
                if random.random() < 0.2 and self.drift_start_time is None:  # 20%概率开始漂移
                    self.drift_start_time = datetime.now()
                    drift_duration = random.randint(300, 1800)  # 5-30分钟
                    logger.warning(f"开始模拟数据漂移，预计持续 {drift_duration//60} 分钟")
                    
                    # 设置漂移结束定时器
                    def end_drift():
                        time.sleep(drift_duration)
                        self.drift_start_time = None
                        logger.info("数据漂移模拟结束")
                    
                    end_thread = threading.Thread(target=end_drift)
                    end_thread.daemon = True
                    end_thread.start()
                
                # 检查漂移
                response = requests.post(
                    f"{self.monitor_url}/check-drift",
                    timeout=10
                )
                
                if response.status_code == 200:
                    drift_cycles += 1
                    if drift_cycles % 10 == 0:
                        logger.info(f"已完成 {drift_cycles} 次漂移检查")
                else:
                    logger.warning(f"检查漂移失败: {response.status_code}")
                
                # 等待指定间隔
                time.sleep(interval)
                
            except requests.exceptions.RequestException as e:
                logger.error(f"请求监控系统失败: {e}")
                time.sleep(10)
            except Exception as e:
                logger.error(f"模拟漂移时出错: {e}")
                time.sleep(5)
    
    def _simulate_anomalies(self, interval=600):
        """模拟异常情况"""
        logger.info("开始模拟异常情况...")
        
        anomaly_cycles = 0
        
        while self.running:
            try:
                # 随机决定是否触发异常
                if random.random() < 0.1 and not self.anomaly_active:  # 10%概率触发异常
                    self.anomaly_active = True
                    self.anomaly_start_time = datetime.now()
                    anomaly_duration = random.randint(60, 300)  # 1-5分钟
                    logger.error(f"开始模拟异常，预计持续 {anomaly_duration} 秒")
                    
                    # 设置异常结束定时器
                    def end_anomaly():
                        time.sleep(anomaly_duration)
                        self.anomaly_active = False
                        self.anomaly_start_time = None
                        logger.info("异常模拟结束")
                    
                    end_thread = threading.Thread(target=end_anomaly)
                    end_thread.daemon = True
                    end_thread.start()
                
                anomaly_cycles += 1
                if anomaly_cycles % 5 == 0:
                    logger.info(f"已完成 {anomaly_cycles} 次异常检查")
                
                # 等待指定间隔
                time.sleep(interval)
                
            except Exception as e:
                logger.error(f"模拟异常时出错: {e}")
                time.sleep(5)
    
    def get_monitor_status(self):
        """获取监控系统状态"""
        try:
            response = requests.get(f"{self.monitor_url}/health", timeout=5)
            if response.status_code == 200:
                return response.json()
            else:
                return {'status': 'unreachable', 'error': f"HTTP {response.status_code}"}
        except Exception as e:
            return {'status': 'error', 'error': str(e)}
    
    def get_monitor_metrics(self):
        """获取监控指标"""
        try:
            response = requests.get(f"{self.monitor_url}/metrics/json", timeout=5)
            if response.status_code == 200:
                return response.json()
            else:
                return None
        except Exception as e:
            logger.error(f"获取监控指标失败: {e}")
            return None
    
    def get_monitor_report(self):
        """获取监控报告"""
        try:
            response = requests.get(f"{self.monitor_url}/report", timeout=10)
            if response.status_code == 200:
                return response.json()
            else:
                return None
        except Exception as e:
            logger.error(f"获取监控报告失败: {e}")
            return None


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='AI模型监控数据生成器')
    parser.add_argument('--monitor-url', default='http://localhost:8000',
                       help='监控系统URL (默认: http://localhost:8000)')
    parser.add_argument('--model-name', default='test_model',
                       help='模型名称 (默认: test_model)')
    parser.add_argument('--interval', type=int, default=30,
                       help='数据生成间隔（秒）(默认: 30)')
    parser.add_argument('--duration', type=int, default=0,
                       help='运行时长（秒），0表示无限运行 (默认: 0)')
    parser.add_argument('--check-status', action='store_true',
                       help='只检查监控系统状态，不生成数据')
    
    args = parser.parse_args()
    
    # 创建数据生成器
    generator = AIMonitorDataGenerator(
        monitor_url=args.monitor_url,
        model_name=args.model_name
    )
    
    if args.check_status:
        # 只检查状态
        status = generator.get_monitor_status()
        print("监控系统状态:")
        print(json.dumps(status, indent=2, ensure_ascii=False))
        
        metrics = generator.get_monitor_metrics()
        if metrics:
            print("\n监控指标:")
            print(json.dumps(metrics, indent=2, ensure_ascii=False))
        
        report = generator.get_monitor_report()
        if report:
            print("\n监控报告:")
            print(json.dumps(report, indent=2, ensure_ascii=False))
        
        return
    
    try:
        # 启动数据生成
        print(f"启动AI模型监控数据生成器")
        print(f"监控URL: {args.monitor_url}")
        print(f"模型名称: {args.model_name}")
        print(f"生成间隔: {args.interval}秒")
        print(f"运行时长: {'无限' if args.duration == 0 else f'{args.duration}秒'}")
        print("按Ctrl+C停止")
        print("-" * 50)
        
        # 检查监控系统状态
        status = generator.get_monitor_status()
        print(f"监控系统状态: {status.get('status', 'unknown')}")
        
        if status.get('status') != 'healthy':
            print("警告: 监控系统可能未正常运行")
            response = input("是否继续? (y/n): ")
            if response.lower() != 'y':
                print("退出")
                return
        
        # 启动生成器
        if args.duration > 0:
            # 有限时长运行
            generator.start(args.interval)
            time.sleep(args.duration)
            generator.stop()
        else:
            # 无限运行
            generator.start(args.interval)
            
    except KeyboardInterrupt:
        print("\n收到停止信号...")
        generator.stop()
    except Exception as e:
        print(f"运行出错: {e}")
        generator.stop()


if __name__ == "__main__":
    main()