import json
import os

def cleanup_mock_data():
    """清理Mock数据文件"""
    files = [
        'mock_users.json',
        'mock_customers.json',
        'mock_orders.json',
        'mock_data.json'
    ]
    
    print("开始清理Mock数据...")
    
    for file in files:
        if os.path.exists(file):
            os.remove(file)
            print(f"已删除: {file}")
        else:
            print(f"文件不存在: {file}")
    
    print("Mock数据清理完成!")

if __name__ == '__main__':
    cleanup_mock_data()
