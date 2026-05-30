import os
import re

def update_breadcrumb_paths():
    base_path = r"i:\AI-Ready\frontend\apps\pc-admin\src\views"
    
    purchase_files = [
        "purchase/tabs/Orders.vue",
        "purchase/tabs/Inquiry.vue",
        "purchase/tabs/Inbound.vue",
        "purchase/tabs/Return.vue",
        "purchase/tabs/Exchange.vue",
        "purchase/tabs/Payment.vue",
        "purchase/tabs/Suppliers.vue"
    ]
    
    sale_files = [
        "sale/tabs/Orders.vue",
        "sale/tabs/Quotation.vue",
        "sale/tabs/Outbound.vue",
        "sale/tabs/Return.vue",
        "sale/tabs/Exchange.vue",
        "sale/tabs/Receipt.vue",
        "sale/tabs/Customers.vue"
    ]
    
    stock_files = [
        "stock/tabs/Stock.vue",
        "stock/tabs/Inbound.vue",
        "stock/tabs/Outbound.vue",
        "stock/tabs/Check.vue",
        "stock/tabs/Transfer.vue",
        "stock/tabs/Batch.vue"
    ]
    
    for file_path in purchase_files:
        full_path = os.path.join(base_path, file_path)
        if os.path.exists(full_path):
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            content = re.sub(
                r"path: '/purchase'",
                r"path: '/erp/purchase'",
                content
            )
            
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"已更新: {file_path}")
    
    for file_path in sale_files:
        full_path = os.path.join(base_path, file_path)
        if os.path.exists(full_path):
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            content = re.sub(
                r"path: '/sale'",
                r"path: '/erp/sale'",
                content
            )
            
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"已更新: {file_path}")
    
    for file_path in stock_files:
        full_path = os.path.join(base_path, file_path)
        if os.path.exists(full_path):
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            content = re.sub(
                r"path: '/stock'",
                r"path: '/erp/stock'",
                content
            )
            
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"已更新: {file_path}")
    
    print("\n所有面包屑路径已更新完成！")

if __name__ == "__main__":
    update_breadcrumb_paths()