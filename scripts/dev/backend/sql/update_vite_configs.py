import os
import re

def update_vite_configs():
    apps = [
        'mobile-admin',
        'driver-delivery',
        'pda-warehouse',
        'mobile-mall'
    ]
    
    base_path = r'i:\AI-Ready\frontend\apps'
    
    for app in apps:
        vite_config_path = os.path.join(base_path, app, 'vite.config.ts')
        
        if os.path.exists(vite_config_path):
            with open(vite_config_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否已经有别名配置
            if '@ai-ready/components' in content:
                print(f"{app}: 已有别名配置，跳过")
                continue
            
            # 添加别名配置
            pattern = r"alias:\s*\{\s*'@':\s*resolve\(__dirname,\s*'src'\),?\s*\}"
            replacement = """alias: {
      '@': resolve(__dirname, 'src'),
      '@ai-ready/components': resolve(__dirname, '../../packages/components/src'),
    }"""
            
            content = re.sub(pattern, replacement, content)
            
            with open(vite_config_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"{app}: 已添加别名配置")
        else:
            print(f"{app}: vite.config.ts 不存在")
    
    print("\n所有应用的vite.config.ts已更新完成！")

if __name__ == "__main__":
    update_vite_configs()