#!/usr/bin/env python3
"""
Mock API Gateway 服务
模拟真实API Gateway的行为，用于性能测试设计
"""

from flask import Flask, jsonify, request
import time
import random
import threading
import logging
from datetime import datetime

app = Flask(__name__)

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 模拟的API响应时间配置
RESPONSE_TIMES = {
    'user_login': {'min': 50, 'max': 200, 'p95': 150},
    'user_register': {'min': 100, 'max': 300, 'p95': 250},
    'order_create': {'min': 150, 'max': 500, 'p95': 400},
    'order_query': {'min': 50, 'max': 200, 'p95': 150},
    'inventory_check': {'min': 30, 'max': 100, 'p95': 80},
    'inventory_deduct': {'min': 50, 'max': 200, 'p95': 150},
    'ai_prediction': {'min': 200, 'max': 800, 'p95': 600},
}

# 模拟成功率配置
SUCCESS_RATES = {
    'user_login': 0.99,
    'user_register': 0.98,
    'order_create': 0.97,
    'order_query': 0.99,
    'inventory_check': 0.995,
    'inventory_deduct': 0.985,
    'ai_prediction': 0.96,
}

# 全局统计
request_stats = {
    'total_requests': 0,
    'successful_requests': 0,
    'failed_requests': 0,
    'total_response_time': 0,
    'endpoint_stats': {}
}
stats_lock = threading.Lock()

def simulate_processing(endpoint_name):
    """模拟API处理时间"""
    config = RESPONSE_TIMES.get(endpoint_name, {'min': 50, 'max': 200, 'p95': 150})
    
    # 模拟P95响应时间
    if random.random() < 0.95:
        response_time = random.randint(config['min'], config['p95'])
    else:
        response_time = random.randint(config['p95'], config['max'])
    
    time.sleep(response_time / 1000)  # 转换为秒
    return response_time

def simulate_success(endpoint_name):
    """模拟API成功率"""
    success_rate = SUCCESS_RATES.get(endpoint_name, 0.98)
    return random.random() < success_rate

def update_stats(endpoint_name, response_time, success):
    """更新统计信息"""
    with stats_lock:
        request_stats['total_requests'] += 1
        request_stats['total_response_time'] += response_time
        
        if success:
            request_stats['successful_requests'] += 1
        else:
            request_stats['failed_requests'] += 1
        
        if endpoint_name not in request_stats['endpoint_stats']:
            request_stats['endpoint_stats'][endpoint_name] = {
                'total_requests': 0,
                'successful_requests': 0,
                'total_response_time': 0
            }
        
        endpoint_stats = request_stats['endpoint_stats'][endpoint_name]
        endpoint_stats['total_requests'] += 1
        endpoint_stats['total_response_time'] += response_time
        if success:
            endpoint_stats['successful_requests'] += 1

@app.route('/api/v1/users/login', methods=['POST'])
def user_login():
    """用户登录API"""
    start_time = time.time()
    
    try:
        # 模拟处理时间
        response_time = simulate_processing('user_login')
        success = simulate_success('user_login')
        
        update_stats('user_login', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': '登录成功',
                'data': {
                    'user_id': random.randint(1000, 9999),
                    'username': 'test_user',
                    'token': 'mock_token_' + str(random.randint(10000, 99999)),
                    'expires_in': 3600
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 401,
                'message': '用户名或密码错误',
                'response_time': response_time
            }), 401
            
    except Exception as e:
        logger.error(f"User login error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/users/register', methods=['POST'])
def user_register():
    """用户注册API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('user_register')
        success = simulate_success('user_register')
        
        update_stats('user_register', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': '注册成功',
                'data': {
                    'user_id': random.randint(1000, 9999),
                    'username': request.json.get('username', 'new_user'),
                    'created_at': datetime.now().isoformat()
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 400,
                'message': '用户已存在或数据无效',
                'response_time': response_time
            }), 400
            
    except Exception as e:
        logger.error(f"User register error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/orders', methods=['POST'])
def create_order():
    """创建订单API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('order_create')
        success = simulate_success('order_create')
        
        update_stats('order_create', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': '订单创建成功',
                'data': {
                    'order_id': random.randint(10000, 99999),
                    'order_number': 'ORD' + str(random.randint(100000, 999999)),
                    'status': 'pending',
                    'total_amount': random.randint(100, 10000),
                    'created_at': datetime.now().isoformat()
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 400,
                'message': '库存不足或支付失败',
                'response_time': response_time
            }), 400
            
    except Exception as e:
        logger.error(f"Create order error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/orders/<int:order_id>', methods=['GET'])
def get_order(order_id):
    """查询订单API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('order_query')
        success = simulate_success('order_query')
        
        update_stats('order_query', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': '查询成功',
                'data': {
                    'order_id': order_id,
                    'order_number': 'ORD' + str(order_id),
                    'status': random.choice(['pending', 'processing', 'completed', 'cancelled']),
                    'total_amount': random.randint(100, 10000),
                    'items': [
                        {
                            'product_id': random.randint(1, 100),
                            'product_name': f'产品{random.randint(1, 100)}',
                            'quantity': random.randint(1, 10),
                            'price': random.randint(10, 1000)
                        }
                        for _ in range(random.randint(1, 5))
                    ]
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 404,
                'message': '订单不存在',
                'response_time': response_time
            }), 404
            
    except Exception as e:
        logger.error(f"Get order error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/inventory/check', methods=['POST'])
def check_inventory():
    """检查库存API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('inventory_check')
        success = simulate_success('inventory_check')
        
        update_stats('inventory_check', response_time, success)
        
        if success:
            product_id = request.json.get('product_id', random.randint(1, 100))
            return jsonify({
                'code': 200,
                'message': '库存检查成功',
                'data': {
                    'product_id': product_id,
                    'available_quantity': random.randint(0, 1000),
                    'reserved_quantity': random.randint(0, 100),
                    'total_quantity': random.randint(100, 1000),
                    'warehouse_info': [
                        {
                            'warehouse_id': random.randint(1, 5),
                            'quantity': random.randint(0, 500)
                        }
                        for _ in range(random.randint(1, 3))
                    ]
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 500,
                'message': '库存服务暂时不可用',
                'response_time': response_time
            }), 500
            
    except Exception as e:
        logger.error(f"Check inventory error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/inventory/deduct', methods=['POST'])
def deduct_inventory():
    """扣减库存API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('inventory_deduct')
        success = simulate_success('inventory_deduct')
        
        update_stats('inventory_deduct', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': '库存扣减成功',
                'data': {
                    'deduct_id': random.randint(10000, 99999),
                    'product_id': request.json.get('product_id', random.randint(1, 100)),
                    'deducted_quantity': request.json.get('quantity', 1),
                    'remaining_quantity': random.randint(0, 1000),
                    'deducted_at': datetime.now().isoformat()
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 400,
                'message': '库存不足或操作失败',
                'response_time': response_time
            }), 400
            
    except Exception as e:
        logger.error(f"Deduct inventory error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/api/v1/ai/predict', methods=['POST'])
def ai_predict():
    """AI预测API"""
    start_time = time.time()
    
    try:
        response_time = simulate_processing('ai_prediction')
        success = simulate_success('ai_prediction')
        
        update_stats('ai_prediction', response_time, success)
        
        if success:
            return jsonify({
                'code': 200,
                'message': 'AI预测成功',
                'data': {
                    'prediction_id': random.randint(10000, 99999),
                    'model_name': 'mock_ai_model_v1.0',
                    'prediction_result': {
                        'confidence': round(random.uniform(0.7, 0.99), 2),
                        'category': random.choice(['positive', 'neutral', 'negative']),
                        'score': round(random.uniform(0, 1), 3),
                        'suggestions': [
                            f'建议{random.randint(1, 5)}',
                            f'建议{random.randint(6, 10)}'
                        ]
                    },
                    'processing_time': response_time
                },
                'response_time': response_time
            }), 200
        else:
            return jsonify({
                'code': 503,
                'message': 'AI服务暂时不可用',
                'response_time': response_time
            }), 503
            
    except Exception as e:
        logger.error(f"AI predict error: {e}")
        return jsonify({
            'code': 500,
            'message': '服务器内部错误'
        }), 500

@app.route('/actuator/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({
        'status': 'UP',
        'details': {
            'mock_api_gateway': {
                'status': 'UP',
                'uptime': round(time.time() - app_start_time, 2)
            }
        }
    })

@app.route('/api/stats', methods=['GET'])
def get_stats():
    """获取统计信息"""
    with stats_lock:
        total_requests = request_stats['total_requests']
        avg_response_time = 0
        if total_requests > 0:
            avg_response_time = request_stats['total_response_time'] / total_requests
        
        return jsonify({
            'current_time': datetime.now().isoformat(),
            'total_requests': total_requests,
            'successful_requests': request_stats['successful_requests'],
            'failed_requests': request_stats['failed_requests'],
            'success_rate': round(request_stats['successful_requests'] / total_requests * 100, 2) if total_requests > 0 else 0,
            'average_response_time': round(avg_response_time, 2),
            'endpoint_stats': request_stats['endpoint_stats'],
            'response_time_configs': RESPONSE_TIMES,
            'success_rate_configs': SUCCESS_RATES
        })

@app.route('/api/reset_stats', methods=['POST'])
def reset_stats():
    """重置统计信息"""
    with stats_lock:
        request_stats['total_requests'] = 0
        request_stats['successful_requests'] = 0
        request_stats['failed_requests'] = 0
        request_stats['total_response_time'] = 0
        request_stats['endpoint_stats'] = {}
    
    return jsonify({
        'code': 200,
        'message': '统计信息已重置'
    })

if __name__ == '__main__':
    app_start_time = time.time()
    logger.info("Mock API Gateway 启动中...")
    logger.info("API 端点配置:")
    logger.info("  - POST /api/v1/users/login     # 用户登录")
    logger.info("  - POST /api/v1/users/register  # 用户注册")
    logger.info("  - POST /api/v1/orders          # 创建订单")
    logger.info("  - GET  /api/v1/orders/<id>     # 查询订单")
    logger.info("  - POST /api/v1/inventory/check # 检查库存")
    logger.info("  - POST /api/v1/inventory/deduct # 扣减库存")
    logger.info("  - POST /api/v1/ai/predict      # AI预测")
    logger.info("  - GET  /actuator/health        # 健康检查")
    logger.info("  - GET  /api/stats              # 统计信息")
    logger.info("  - POST /api/reset_stats        # 重置统计")
    
    app.run(host='0.0.0.0', port=8080, debug=False, threaded=True)