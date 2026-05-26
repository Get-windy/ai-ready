# AI环境配置脚本
# 用于配置TensorFlow/PyTorch/LangChain环境

Write-Host "=== AI环境配置脚本 ===" -ForegroundColor Green
Write-Host "开始时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host ""

# 1. 创建Python虚拟环境
Write-Host "1. 创建Python虚拟环境..." -ForegroundColor Yellow
$venvPath = "I:\AI-Ready\ai\venv"
if (-not (Test-Path $venvPath)) {
    Write-Host "   创建虚拟环境..." -ForegroundColor Cyan
    python -m venv $venvPath
    Write-Host "   虚拟环境创建完成" -ForegroundColor Green
} else {
    Write-Host "   虚拟环境已存在" -ForegroundColor Green
}

# 2. 激活虚拟环境并安装基础包
Write-Host "2. 安装基础依赖..." -ForegroundColor Yellow
$activateScript = "$venvPath\Scripts\Activate.ps1"
if (Test-Path $activateScript) {
    Write-Host "   激活虚拟环境..." -ForegroundColor Cyan
    
    # 创建requirements.txt文件
    $requirements = @"
# AI环境依赖包
numpy==1.24.3
pandas==2.1.4
scipy==1.11.4
scikit-learn==1.3.2
matplotlib==3.8.2
jupyter==1.0.0
notebook==7.0.6

# TensorFlow (CPU版本)
tensorflow==2.15.0
tensorflow-addons==0.22.0
tensorflow-datasets==4.9.4

# PyTorch (CPU版本)
torch==2.1.0
torchvision==0.16.0
torchaudio==2.1.0

# LangChain和AI工具
langchain==0.0.340
langchain-core==0.0.5
langchain-community==0.0.10
langchain-text-splitters==0.0.1
openai==0.28.1
chromadb==0.4.18
faiss-cpu==1.7.4
sentence-transformers==2.2.2
transformers==4.35.2
datasets==2.15.0

# Web框架
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0

# 工具库
psutil==5.9.7
pypdf==3.17.4
python-docx==1.1.0
beautifulsoup4==4.12.2
"@
    
    # 保存requirements.txt
    $reqPath = "I:\AI-Ready\ai\requirements.txt"
    $requirements | Out-File -FilePath $reqPath -Encoding UTF8
    Write-Host "   requirements.txt已创建" -ForegroundColor Green
    
    # 安装依赖
    Write-Host "   安装依赖包..." -ForegroundColor Cyan
    & "$venvPath\Scripts\pip.exe" install --upgrade pip
    & "$venvPath\Scripts\pip.exe" install -r $reqPath
    
    Write-Host "   依赖包安装完成" -ForegroundColor Green
} else {
    Write-Host "   错误: 虚拟环境激活脚本不存在" -ForegroundColor Red
}

# 3. 创建环境验证脚本
Write-Host "3. 创建环境验证脚本..." -ForegroundColor Yellow
$verifyScript = @"
import sys
import importlib
import pkg_resources

REQUIRED_PACKAGES = [
    ("numpy", "1.24.0"),
    ("pandas", "2.0.0"),
    ("torch", "2.0.0"),
    ("tensorflow", "2.15.0"),
    ("langchain", "0.0.340"),
    ("openai", "0.28.0"),
    ("fastapi", "0.104.0"),
    ("uvicorn", "0.24.0"),
]

def check_package(package_name, min_version):
    try:
        dist = pkg_resources.get_distribution(package_name)
        installed_version = pkg_resources.parse_version(dist.version)
        required_version = pkg_resources.parse_version(min_version)
        
        if installed_version >= required_version:
            return True, f"✓ {package_name} {dist.version} (要求: >= {min_version})"
        else:
            return False, f"✗ {package_name} {dist.version} (要求: >= {min_version})"
    except pkg_resources.DistributionNotFound:
        return False, f"✗ {package_name} 未安装"

def main():
    print("=== AI环境验证 ===")
    print(f"Python版本: {sys.version}")
    print()
    
    all_passed = True
    for package_name, min_version in REQUIRED_PACKAGES:
        passed, message = check_package(package_name, min_version)
        print(message)
        if not passed:
            all_passed = False
    
    print()
    if all_passed:
        print("✅ 所有依赖包检查通过")
        return 0
    else:
        print("❌ 部分依赖包检查失败")
        return 1

if __name__ == "__main__":
    sys.exit(main())
"@

$verifyPath = "I:\AI-Ready\ai\scripts\verify_environment.py"
New-Item -ItemType Directory -Force -Path "I:\AI-Ready\ai\scripts" | Out-Null
$verifyScript | Out-File -FilePath $verifyPath -Encoding UTF8
Write-Host "   环境验证脚本已创建" -ForegroundColor Green

# 4. 创建AI模型目录结构
Write-Host "4. 创建AI模型目录结构..." -ForegroundColor Yellow
$modelDirs = @(
    "models/tensorflow/classification",
    "models/tensorflow/nlp",
    "models/tensorflow/vision",
    "models/pytorch/classification",
    "models/pytorch/nlp",
    "models/pytorch/vision",
    "models/huggingface/pretrained",
    "models/huggingface/fine-tuned",
    "models/langchain/embeddings",
    "models/langchain/vectorstores",
    "cache",
    "logs",
    "data/raw",
    "data/processed"
)

foreach ($dir in $modelDirs) {
    $fullPath = "I:\AI-Ready\ai\$dir"
    New-Item -ItemType Directory -Force -Path $fullPath | Out-Null
}
Write-Host "   目录结构创建完成" -ForegroundColor Green

# 5. 创建模型管理器
Write-Host "5. 创建模型管理脚本..." -ForegroundColor Yellow
$modelManagerScript = @"
import os
import json
import hashlib
from datetime import datetime

class ModelManager:
    def __init__(self, base_dir="I:\\\\AI-Ready\\\\ai\\\\models"):
        self.base_dir = base_dir
        self.metadata_file = os.path.join(base_dir, "model_metadata.json")
        self.load_metadata()
    
    def load_metadata(self):
        if os.path.exists(self.metadata_file):
            with open(self.metadata_file, 'r', encoding='utf-8') as f:
                self.metadata = json.load(f)
        else:
            self.metadata = {"models": {}, "last_updated": None}
    
    def save_metadata(self):
        self.metadata["last_updated"] = datetime.now().isoformat()
        os.makedirs(os.path.dirname(self.metadata_file), exist_ok=True)
        with open(self.metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.metadata, f, indent=2, ensure_ascii=False)
    
    def register_model(self, model_type, model_name, model_path, metadata=None):
        model_id = hashlib.md5(f"{model_type}_{model_name}".encode()).hexdigest()[:8]
        
        model_info = {
            "id": model_id,
            "type": model_type,
            "name": model_name,
            "path": model_path,
            "registered_at": datetime.now().isoformat(),
            "metadata": metadata or {}
        }
        
        self.metadata["models"][model_id] = model_info
        self.save_metadata()
        return model_id
    
    def get_model(self, model_id):
        return self.metadata["models"].get(model_id)
    
    def list_models(self, model_type=None):
        if model_type:
            return {k: v for k, v in self.metadata["models"].items() 
                    if v["type"] == model_type}
        return self.metadata["models"]

if __name__ == "__main__":
    manager = ModelManager()
    print("模型管理器初始化完成")
    print(f"元数据文件: {manager.metadata_file}")
"@

$managerPath = "I:\AI-Ready\ai\scripts\model_manager.py"
$modelManagerScript | Out-File -FilePath $managerPath -Encoding UTF8
Write-Host "   模型管理脚本已创建" -ForegroundColor Green

# 6. 验证环境
Write-Host "6. 验证AI环境..." -ForegroundColor Yellow
Write-Host "   运行环境验证脚本..." -ForegroundColor Cyan
& "$venvPath\Scripts\python.exe" $verifyPath

Write-Host ""
Write-Host "=== AI环境配置完成 ===" -ForegroundColor Green
Write-Host "完成时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host ""
Write-Host "环境配置总结:" -ForegroundColor Yellow
Write-Host "1. Python虚拟环境: $venvPath" -ForegroundColor Cyan
Write-Host "2. 依赖包: I:\AI-Ready\ai\requirements.txt" -ForegroundColor Cyan
Write-Host "3. 模型目录: I:\AI-Ready\ai\models\" -ForegroundColor Cyan
Write-Host "4. 验证脚本: I:\AI-Ready\ai\scripts\verify_environment.py" -ForegroundColor Cyan
Write-Host "5. 模型管理: I:\AI-Ready\ai\scripts\model_manager.py" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "1. 激活虚拟环境: & '$venvPath\Scripts\Activate.ps1'" -ForegroundColor Cyan
Write-Host "2. 测试TensorFlow: python -c 'import tensorflow as tf; print(tf.__version__)'" -ForegroundColor Cyan
Write-Host "3. 测试PyTorch: python -c 'import torch; print(torch.__version__)'" -ForegroundColor Cyan
Write-Host "4. 测试LangChain: python -c 'import langchain; print(langchain.__version__)'" -ForegroundColor Cyan