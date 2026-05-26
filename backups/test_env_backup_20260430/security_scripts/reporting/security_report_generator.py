"""
安全报告生成器 - 修复版
生成安全验证报告和结果分析
"""

import os
import json
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional

class SecurityReportGenerator:
    """安全报告生成器"""
    
    def __init__(self, test_env_root: str):
        """
        初始化安全报告生成器
        
        Args:
            test_env_root: 测试环境根目录
        """
        self.test_env_root = test_env_root
        self.reports_dir = os.path.join(test_env_root, "reports", "security")
        self.logger = logging.getLogger(__name__)
        
        # 确保报告目录存在
        os.makedirs(self.reports_dir, exist_ok=True)
    
    def generate_all_reports(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """
        生成所有安全报告
        
        Args:
            validation_results: 安全验证结果
            
        Returns:
            包含所有报告信息的字典
        """
        self.logger.info("开始生成安全报告...")
        
        report_results = {
            'summary': {
                'reports_generated': 0,
                'formats': [],
                'status': 'UNKNOWN'
            },
            'reports': [],
            'timestamp': datetime.now().isoformat()
        }
        
        try:
            # 1. 生成详细JSON报告
            self.logger.info("生成JSON报告...")
            json_report = self.generate_json_report(validation_results)
            report_results['reports'].append(json_report)
            report_results['summary']['reports_generated'] += 1
            report_results['summary']['formats'].append('json')
            
            # 2. 生成摘要Markdown报告
            self.logger.info("生成Markdown报告...")
            markdown_report = self.generate_markdown_report(validation_results)
            report_results['reports'].append(markdown_report)
            report_results['summary']['reports_generated'] += 1
            report_results['summary']['formats'].append('markdown')
            
            # 3. 生成HTML报告
            self.logger.info("生成HTML报告...")
            html_report = self.generate_html_report(validation_results)
            report_results['reports'].append(html_report)
            report_results['summary']['reports_generated'] += 1
            report_results['summary']['formats'].append('html')
            
            # 4. 生成风险评级报告
            self.logger.info("生成风险评级报告...")
            risk_report = self.generate_risk_report(validation_results)
            report_results['reports'].append(risk_report)
            report_results['summary']['reports_generated'] += 1
            report_results['summary']['formats'].append('risk')
            
            # 5. 生成改进建议报告
            self.logger.info("生成改进建议报告...")
            improvement_report = self.generate_improvement_report(validation_results)
            report_results['reports'].append(improvement_report)
            report_results['summary']['reports_generated'] += 1
            report_results['summary']['formats'].append('improvement')
            
            # 更新状态
            if report_results['summary']['reports_generated'] > 0:
                report_results['summary']['status'] = 'SUCCESS'
            else:
                report_results['summary']['status'] = 'FAILED'
            
            self.logger.info(f"报告生成完成，共生成{report_results['summary']['reports_generated']}份报告")
            
        except Exception as e:
            self.logger.error(f"报告生成过程中发生错误: {e}", exc_info=True)
            report_results['error'] = str(e)
            report_results['summary']['status'] = 'ERROR'
        
        return report_results
    
    def generate_json_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成详细的JSON报告"""
        report_info = {
            'report_name': '安全验证详细报告',
            'format': 'json',
            'description': '完整的JSON格式安全验证结果',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_validation_report_{timestamp}.json")
            
            # 准备报告数据
            report_data = {
                'metadata': {
                    'generated_at': datetime.now().isoformat(),
                    'test_environment': self.test_env_root,
                    'report_version': '1.0.0'
                },
                'summary': validation_results.get('overall', {}),
                'config_validation': validation_results.get('config_validation', {}),
                'vulnerability_scanning': validation_results.get('vulnerability_scanning', {}),
                'compliance_checks': validation_results.get('compliance_checks', {}),
                'raw_results': validation_results
            }
            
            # 保存JSON文件
            with open(report_file, 'w', encoding='utf-8') as f:
                json.dump(report_data, f, indent=2, ensure_ascii=False)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"JSON报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成JSON报告失败: {e}")
        
        return report_info
    
    def generate_markdown_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成Markdown摘要报告"""
        report_info = {
            'report_name': '安全验证摘要报告',
            'format': 'markdown',
            'description': 'Markdown格式的安全验证摘要',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_summary_{timestamp}.md")
            
            # 获取摘要信息
            overall_summary = validation_results.get('overall', {})
            config_summary = validation_results.get('config_validation', {}).get('summary', {})
            vuln_summary = validation_results.get('vulnerability_scanning', {}).get('summary', {})
            compliance_summary = validation_results.get('compliance_checks', {}).get('summary', {})
            
            # 生成Markdown内容
            markdown_content = f"""# 测试环境安全验证摘要报告

**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**测试环境**: {self.test_env_root}  
**报告版本**: 1.0.0

## 总体结果

| 项目 | 结果 |
|------|------|
| **总体状态** | `{overall_summary.get('status', 'UNKNOWN')}` |
| **总检查项** | {overall_summary.get('total_checks', 0)} |
| **通过项** | {overall_summary.get('passed_checks', 0)} |
| **失败项** | {overall_summary.get('failed_checks', 0)} |
| **警告项** | {overall_summary.get('warnings', 0)} |

## 安全配置验证

| 项目 | 结果 |
|------|------|
| **状态** | `{config_summary.get('status', 'UNKNOWN')}` |
| **检查项** | {config_summary.get('total_checks', 0)} |
| **通过** | {config_summary.get('passed_checks', 0)} |
| **失败** | {config_summary.get('failed_checks', 0)} |
| **警告** | {config_summary.get('warnings', 0)} |

## 漏洞扫描结果

| 项目 | 结果 |
|------|------|
| **状态** | `{vuln_summary.get('status', 'UNKNOWN')}` |
| **扫描项** | {vuln_summary.get('total_scans', 0)} |
| **发现漏洞** | {vuln_summary.get('vulnerabilities_found', 0)} |
| **高危漏洞** | {vuln_summary.get('high_severity', 0)} |
| **中危漏洞** | {vuln_summary.get('medium_severity', 0)} |
| **低危漏洞** | {vuln_summary.get('low_severity', 0)} |

## 合规检查结果

| 项目 | 结果 |
|------|------|
| **状态** | `{compliance_summary.get('status', 'UNKNOWN')}` |
| **合规标准** | {compliance_summary.get('total_standards', 0)} |
| **完全合规** | {compliance_summary.get('compliant_standards', 0)} |
| **部分合规** | {compliance_summary.get('partially_compliant', 0)} |
| **不合规** | {compliance_summary.get('non_compliant', 0)} |
| **合规分数** | {compliance_summary.get('compliance_score', 0)}% |

## 安全建议

1. 处理发现的高危安全漏洞
2. 修复不合规的安全配置
3. 更新过期的依赖包
4. 加强访问控制和认证机制
5. 建立安全事件响应流程

---

*报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*  
*报告ID: security_summary_{timestamp}*
"""
            
            # 保存Markdown文件
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(markdown_content)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"Markdown报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成Markdown报告失败: {e}")
        
        return report_info
    
    def generate_html_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成HTML报告"""
        report_info = {
            'report_name': '安全验证HTML报告',
            'format': 'html',
            'description': 'HTML格式的交互式安全报告',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_report_{timestamp}.html")
            
            # 获取摘要信息
            overall_summary = validation_results.get('overall', {})
            config_summary = validation_results.get('config_validation', {}).get('summary', {})
            vuln_summary = validation_results.get('vulnerability_scanning', {}).get('summary', {})
            compliance_summary = validation_results.get('compliance_checks', {}).get('summary', {})
            
            # 生成简单的HTML报告
            html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>测试环境安全验证报告</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
        .header {{ text-align: center; border-bottom: 2px solid #4CAF50; padding-bottom: 20px; margin-bottom: 30px; }}
        .header h1 {{ color: #333; margin-bottom: 10px; }}
        .header .subtitle {{ color: #666; font-size: 16px; }}
        .summary {{ background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 30px; }}
        .summary-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }}
        .summary-card {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }}
        .summary-card h3 {{ margin-top: 0; color: #333; border-bottom: 1px solid #eee; padding-bottom: 10px; }}
        .status {{ font-weight: bold; padding: 5px 10px; border-radius: 4px; display: inline-block; }}
        .status.passed {{ background: #d4edda; color: #155724; }}
        .status.failed {{ background: #f8d7da; color: #721c24; }}
        .status.warning {{ background: #fff3cd; color: #856404; }}
        .status.unknown {{ background: #e2e3e5; color: #383d41; }}
        .metrics {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-top: 15px; }}
        .metric {{ text-align: center; }}
        .metric-value {{ font-size: 24px; font-weight: bold; color: #4CAF50; }}
        .metric-label {{ font-size: 14px; color: #666; margin-top: 5px; }}
        .section {{ margin-bottom: 40px; }}
        .section h2 {{ color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 15px; }}
        th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #f8f9fa; font-weight: bold; }}
        tr:hover {{ background-color: #f5f5f5; }}
        .critical {{ color: #dc3545; font-weight: bold; }}
        .high {{ color: #fd7e14; }}
        .medium {{ color: #ffc107; }}
        .low {{ color: #28a745; }}
        .footer {{ margin-top: 40px; text-align: center; color: #666; font-size: 14px; border-top: 1px solid #eee; padding-top: 20px; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>测试环境安全验证报告</h1>
            <div class="subtitle">
                生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} | 
                测试环境: {os.path.basename(self.test_env_root)} |
                报告版本: 1.0.0
            </div>
        </div>
        
        <div class="summary">
            <h2>执行摘要</h2>
            <div class="summary-grid">
                <div class="summary-card">
                    <h3>总体状态</h3>
                    <div class="status {self._get_status_class(overall_summary.get('status', 'UNKNOWN'))}">
                        {overall_summary.get('status', 'UNKNOWN')}
                    </div>
                    <div class="metrics">
                        <div class="metric">
                            <div class="metric-value">{overall_summary.get('total_checks', 0)}</div>
                            <div class="metric-label">总检查项</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value">{overall_summary.get('passed_checks', 0)}</div>
                            <div class="metric-label">通过项</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value">{overall_summary.get('failed_checks', 0)}</div>
                            <div class="metric-label">失败项</div>
                        </div>
                    </div>
                </div>
                
                <div class="summary-card">
                    <h3>漏洞扫描</h3>
                    <div class="status {self._get_status_class(vuln_summary.get('status', 'UNKNOWN'))}">
                        {vuln_summary.get('status', 'UNKNOWN')}
                    </div>
                    <div class="metrics">
                        <div class="metric">
                            <div class="metric-value critical">{vuln_summary.get('high_severity', 0)}</div>
                            <div class="metric-label">高危漏洞</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value high">{vuln_summary.get('medium_severity', 0)}</div>
                            <div class="metric-label">中危漏洞</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value medium">{vuln_summary.get('low_severity', 0)}</div>
                            <div class="metric-label">低危漏洞</div>
                        </div>
                    </div>
                </div>
                
                <div class="summary-card">
                    <h3>合规检查</h3>
                    <div class="status {self._get_status_class(compliance_summary.get('status', 'UNKNOWN'))}">
                        {compliance_summary.get('status', 'UNKNOWN')}
                    </div>
                    <div class="metrics">
                        <div class="metric">
                            <div class="metric-value">{compliance_summary.get('compliance_score', 0)}%</div>
                            <div class="metric-label">合规分数</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value">{compliance_summary.get('compliant_standards', 0)}</div>
                            <div class="metric-label">完全合规</div>
                        </div>
                        <div class="metric">
                            <div class="metric-value">{compliance_summary.get('non_compliant', 0)}</div>
                            <div class="metric-label">不合规</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="section">
            <h2>安全验证详情</h2>
            <table>
                <tr>
                    <th>检查项</th>
                    <th>状态</th>
                    <th>检查数量</th>
                    <th>通过</th>
                    <th>失败</th>
                    <th>警告</th>
                </tr>
                <tr>
                    <td>所有检查</td>
                    <td><span class="status {self._get_status_class(overall_summary.get('status', 'UNKNOWN'))}">{overall_summary.get('status', 'UNKNOWN')}</span></td>
                    <td>{overall_summary.get('total_checks', 0)}</td>
                    <td>{overall_summary.get('passed_checks', 0)}</td>
                    <td>{overall_summary.get('failed_checks', 0)}</td>
                    <td>{overall_summary.get('warnings', 0)}</td>
                </tr>
            </table>
        </div>
        
        <div class="section">
            <h2>安全建议</h2>
            <div style="background: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107;">
                <h3 style="margin-top: 0; color: #856404;">立即行动建议</h3>
                <ul>
                    <li>处理发现的高危安全漏洞</li>
                    <li>修复不合规的安全配置</li>
                    <li>更新过期的依赖包</li>
                    <li>加强访问控制和认证机制</li>
                </ul>
            </div>
        </div>
        
        <div class="footer">
            <p>报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
            <p>报告ID: security_report_{timestamp} | 测试环境: {self.test_env_root}</p>
            <p>© 2026 AI-Ready 测试团队 - 安全验证报告</p>
        </div>
    </div>
</body>
</html>"""
            
            # 保存HTML文件
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(html_content)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"HTML报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成HTML报告失败: {e}")
        
        return report_info
    
    def generate_risk_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成风险评级报告"""
        report_info = {
            'report_name': '安全风险评级报告',
            'format': 'markdown',
            'description': '安全风险评级和优先级分析',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_risk_assessment_{timestamp}.md")
            
            # 生成风险评级内容
            risk_content = self._generate_risk_assessment(validation_results)
            
            # 保存风险报告
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(risk_content)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"风险评级报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成风险评级报告失败: {e}")
        
        return report_info
    
    def generate_improvement_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成改进建议报告"""
        report_info = {
            'report_name': '安全改进建议报告',
            'format': 'markdown',
            'description': '具体的安全改进建议和实施计划',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_improvements_{timestamp}.md")
            
            # 生成改进建议内容
            improvement_content = self._generate_improvement_plan()
            
            # 保存改进建议报告
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(improvement_content)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"改进建议报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成改进建议报告失败: {e}")
        
        return report_info
    
    def generate_trend_report(self, validation_results: Dict[str, Any]) -> Dict[str, Any]:
        """生成趋势分析报告"""
        report_info = {
            'report_name': '安全趋势分析报告',
            'format': 'markdown',
            'description': '安全状态趋势分析和预测',
            'status': 'GENERATED',
            'file_path': None,
            'file_size': 0
        }
        
        try:
            # 生成时间戳
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = os.path.join(self.reports_dir, f"security_trend_analysis_{timestamp}.md")
            
            # 生成趋势分析内容
            trend_content = self._generate_trend_analysis()
            
            # 保存趋势分析报告
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(trend_content)
            
            # 更新报告信息
            report_info['file_path'] = report_file
            report_info['file_size'] = os.path.getsize(report_file)
            
            self.logger.info(f"趋势分析报告已生成: {report_file}")
            
        except Exception as e:
            report_info['status'] = 'FAILED'
            report_info['error'] = str(e)
            self.logger.error(f"生成趋势分析报告失败: {e}")
        
        return report_info
    
    def _get_status_class(self, status: str) -> str:
        """根据状态获取CSS类名"""
        status_map = {
            'PASSED': 'passed',
            'COMPLIANT': 'passed',
            'SAFE': 'passed',
            'SUCCESS': 'passed',
            'FAILED': 'failed',
            'NON_COMPLIANT': 'failed',
            'CRITICAL': 'failed',
            'ERROR': 'failed',
            'WARNING': 'warning',
            'PARTIALLY_COMPLIANT': 'warning',
            'INFO': 'warning',
            'UNKNOWN': 'unknown',
            'NO_CHECKS': 'unknown'
        }
        return status_map.get(status, 'unknown')
    
    def _generate_risk_assessment(self, validation_results: Dict[str, Any]) -> str:
        """生成风险评级内容"""
        overall_summary = validation_results.get('overall', {})
        vuln_summary = validation_results.get('vulnerability_scanning', {}).get('summary', {})
        
        # 计算风险分数
        risk_score = 100
        high_vulns = vuln_summary.get('high_severity', 0)
        medium_vulns = vuln_summary.get('medium_severity', 0)
        
        risk_score -= high_vulns * 20  # 每个高危漏洞扣20分
        risk_score -= medium_vulns * 10  # 每个中危漏洞扣10分
        risk_score = max(0, risk_score)
        
        # 确定风险等级
        if risk_score >= 80:
            risk_level = "低风险"
        elif risk_score >= 60:
            risk_level = "中等风险"
        elif risk_score >= 40:
            risk_level = "高风险"
        else:
            risk_level = "极高风险"
        
        return f"""# 安全风险评级报告

**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**风险评级**: {risk_level}
**风险分数**: {risk_score}/100

## 风险分析

### 漏洞风险
- 高危漏洞: {high_vulns} 个
- 中危漏洞: {medium_vulns} 个
- 低危漏洞: {vuln_summary.get('low_severity', 0)} 个

### 配置风险
- 配置检查失败: {overall_summary.get('failed_checks', 0)} 项
- 配置检查警告: {overall_summary.get('warnings', 0)} 项

## 风险缓解建议

1. **立即处理**: 修复所有高危漏洞
2. **短期计划**: 处理中危漏洞和配置问题
3. **长期规划**: 建立持续的安全监控体系
4. **预防措施**: 实施安全开发实践和代码审查

---
*报告结束*
"""
    
    def _generate_improvement_plan(self) -> str:
        """生成改进计划内容"""
        return f"""# 安全改进建议报告

**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**改进优先级**: 高

## 改进目标

1. 消除所有高危安全漏洞
2. 达到90%以上的合规分数
3. 建立持续的安全监控机制
4. 提升整体安全防护能力

## 具体改进措施

### 立即执行（1-3天）
1. 修复发现的硬编码密码和密钥
2. 更新过期的依赖包
3. 配置安全的JWT密钥
4. 启用并配置适当的速率限制

### 短期计划（1-2周）
1. 实施数据加密传输和存储
2. 建立安全审计日志系统
3. 完善访问控制机制
4. 创建安全事件响应流程

### 中长期规划（1-3个月）
1. 实施持续的安全漏洞扫描
2. 建立安全开发生命周期(SDLC)
3. 实施安全培训和意识提升计划
4. 建立安全合规自动化检查

## 改进效果评估

### 预期效果
- 高危漏洞减少90%以上
- 合规分数提升至85%以上
- 安全事件响应时间缩短50%
- 安全配置自动化程度提升

---
*改进计划需要定期评审和更新*
*根据实际执行情况调整优先级和计划*
"""
    
    def _generate_trend_analysis(self) -> str:
        """生成趋势分析内容"""
        return f"""# 安全趋势分析报告

**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**趋势分析**: 首次安全验证

## 趋势分析

由于这是第一次安全验证，无法提供历史趋势对比。
建议定期执行安全验证以建立趋势基线。

## 建议监控指标

1. 高危漏洞数量变化趋势
2. 合规分数变化趋势
3. 安全事件数量趋势
4. 漏洞修复时间趋势

## 趋势监控建议

### 数据收集
1. 每月执行一次完整安全验证
2. 每周执行快速安全检查
3. 记录所有安全事件和修复情况

---
*趋势分析需要至少3次历史数据才能准确分析*
*建议每月执行一次安全验证*
"""