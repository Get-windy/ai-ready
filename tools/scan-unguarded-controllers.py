# -*- coding: utf-8 -*-
"""
扫描「整类零权限注解」的控制器 —— E-01（分批补控制器鉴权注解）的记分牌。

为什么需要它：
  gen-permission-effectivity.py 是从「权限码」视角看问题的（这个码有没有消费方），
  看不见反过来的那一半 —— **有端点、却一个权限注解都没有的控制器**。
  那类端点对**任何登录用户**开放（Sa-Token 的 @SaCheckPermission 不写就是不校验），
  是比「僵尸码」严重得多的问题：僵尸码只是"勾了没用"，裸端点则是"谁都能用"。

判定：类内 (Get|Post|Put|Delete|Patch)Mapping 数量 > 0，且
      @SaCheckPermission / @RequiresPermission / @RequirePermission 数量 == 0。

注意：**不是每个零注解控制器都是缺陷**，需人工看三类豁免：
  · 公开接口（商城 mall /api/v1/**、登录验证码等）
  · 只读自己的数据、租户 id 取自会话的（如 SetAppCenterController，头部注释里有裁定）
  · 纯转发/健康检查
所以本脚本只负责**列出来**，不负责定罪。

用法: python tools/scan-unguarded-controllers.py [--json]
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')

MAP = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping')
PERM = re.compile(r'@(?:SaCheckPermission|RequiresPermission|RequirePermission)\s*\(')
LOGIN = re.compile(r'@SaCheckLogin')


def main():
    rows = []
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d != 'target']
        norm = dirpath.replace('\\', '/')
        if '/src/test' in norm:
            continue
        for fn in filenames:
            if not fn.endswith('Controller.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8').read()
            except Exception:
                continue
            n_ep = len(MAP.findall(src))
            if n_ep == 0 or PERM.search(src):
                continue
            rows.append({
                'file': os.path.relpath(p, ROOT).replace('\\', '/'),
                'endpoints': n_ep,
                'loginOnly': bool(LOGIN.search(src)),
            })
    rows.sort(key=lambda r: (-r['endpoints'], r['file']))

    if '--json' in sys.argv:
        print(json.dumps(rows, ensure_ascii=False, indent=1))
        return

    print('整类零权限注解的控制器：%d 个，合计端点 %d 处\n'
          % (len(rows), sum(r['endpoints'] for r in rows)))
    for r in rows:
        flag = '[仅 @SaCheckLogin]' if r['loginOnly'] else '[无任何类级校验注解]'
        print('%3d 处端点  %-96s %s' % (r['endpoints'], r['file'], flag))


if __name__ == '__main__':
    main()
