#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
接口命中审计（运行期证据）——给「静态上没人调用」的接口补上真实流量数据。

为什么需要它：
  tools/audit-api-usage.py 只能证明「前端源码里没搜到调用」，证明不了「生产上没人调」。
  公网链接（扫码评价/签收）、外部系统回调（WMS→ERP）、移动端旧版本、第三方集成
  都不会出现在本仓库的前端源码里。删这些接口前必须看真实命中。
  本脚本把静态候选与访问日志对上：
    · 命中为 0  → 可以进入弃用流程（先 @Deprecated + Sunset 头 + 观察一个周期，再删）
    · 有命中    → 静态审计漏判，禁止删除（脚本会给出命中次数与样例 UA）

数据来源：访问日志（nginx combined / Tomcat access log 都是 "METHOD /path HTTP/1.1" 形式）。
  nginx 未开日志时，可临时在 server 块加：
    log_format aiedge '$remote_addr - $remote_user [$time_local] "$request" $status $body_bytes_sent "$http_user_agent"';
    access_log /var/log/nginx/aiedge.log aiedge;
  观察窗口建议 2~4 周（覆盖月结/对账等低频周期）。

用法:
  python tools/audit-endpoint-hits.py /var/log/nginx/aiedge.log
  zcat aiedge.log.*.gz | python tools/audit-endpoint-hits.py -
  python tools/audit-endpoint-hits.py --self-test          # 用合成日志验证解析与判定逻辑

依赖: 先跑一次 tools/audit-api-usage.py 生成 tools/audit-api-usage.json（静态候选清单）。
"""
import argparse
import collections
import io
import json
import os
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
USAGE_JSON = os.path.join(ROOT, 'tools', 'audit-api-usage.json')

# nginx combined / Tomcat access log 共同的可解析部分
REQUEST_RE = re.compile(
    r'"(?P<method>[A-Z]{3,7})\s+(?P<path>[^\s"?]+)(?:\?[^\s"]*)?\s+HTTP/[\d.]+"'
)
UA_RE = re.compile(r'"\s*"([^"]*)"\s*$')

# 这些方法不计入命中（OPTIONS 是浏览器预检，HEAD 常由探活产生）
IGNORED_METHODS = {'OPTIONS', 'HEAD'}
METHOD_ALIASES = {'HEAD': 'GET'}


def norm_path(path):
    """把真实请求路径与声明的 URI 模板归一到同一形态，便于比较。"""
    path = path.split('?')[0]
    path = re.sub(r'\$\{[^}]*\}', '{}', path)          # ${id}
    path = re.sub(r'\{[^}]*\}', '{}', path)            # {id} / {id:\\d+}
    path = re.sub(r'/[0-9]+(?=/|$)', '/{}', path)      # /123
    path = re.sub(r'/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)',
                  '/{}', path)                          # UUID
    path = re.sub(r'/+', '/', path)
    if len(path) > 1 and path.endswith('/'):
        path = path[:-1]
    return path


def load_candidates():
    """从静态审计产物中取出「前端从未调用」的接口：{(method, normalized_path): 声明位置}"""
    if not os.path.exists(USAGE_JSON):
        sys.exit(f"缺少静态审计产物 {USAGE_JSON}，请先运行: python tools/audit-api-usage.py")
    with open(USAGE_JSON, encoding='utf-8') as fh:
        data = json.load(fh)

    candidates = {}
    for bucket in ('unused_normal', 'unused_internal'):
        for path, decls in (data.get(bucket) or {}).items():
            for decl in decls:
                method, source, line = (list(decl) + ['', '', ''])[:3]
                key = ((method or '').upper(), norm_path(path))
                candidates[key] = f"{source}:{line}"
    return candidates


def parse_log(streams):
    """返回 ((method, path) -> 命中次数, 命中 -> 样例 UA, 总行数, 解析失败行数)"""
    hits = collections.Counter()
    sample_ua = {}
    total = 0
    unparsed = 0
    for stream in streams:
        for raw in stream:
            total += 1
            match = REQUEST_RE.search(raw)
            if not match:
                unparsed += 1
                continue
            method = match.group('method').upper()
            if method in IGNORED_METHODS:
                method = METHOD_ALIASES.get(method, method)
                if method == 'HEAD':
                    continue
            key = (method, norm_path(match.group('path')))
            hits[key] += 1
            if key not in sample_ua:
                ua = UA_RE.search(raw)
                sample_ua[key] = (ua.group(1)[:80] if ua else '')
    return hits, sample_ua, total, unparsed


def report(candidates, hits, sample_ua, total, unparsed, limit):
    zero_hit, hit_unexpected = {}, {}
    for key, source in candidates.items():
        if hits.get(key):
            hit_unexpected[key] = (hits[key], sample_ua.get(key, ''), source)
        else:
            zero_hit[key] = source

    print(f"日志行数 {total}（解析失败 {unparsed}）｜静态候选接口 {len(candidates)} 个")
    if total == 0 or len(candidates) < 100:
        print("⚠️  输入疑似为空或静态清单不完整——本报告的结论不可用于删除决策")
    elif unparsed > total * 0.5:
        print(f"⚠️  {unparsed}/{total} 行无法解析（日志格式不匹配？），命中数据可能严重缺失")

    print(f"\n=== ① 观察窗口内零命中（{len(zero_hit)} 个）：可进入弃用流程 ===")
    print("   流程：@Deprecated + 文档标注 + Sunset 响应头 → 再观察一个发布周期 → 删除")
    for (method, path), source in sorted(zero_hit.items(), key=lambda kv: kv[0][1])[:limit]:
        print(f"   {method:<7} {path:<70} {source}")

    print(f"\n=== ② 静态审计漏判、实际有命中（{len(hit_unexpected)} 个）：禁止删除 ===")
    for (method, path), (count, ua, source) in sorted(
            hit_unexpected.items(), key=lambda kv: -kv[1][0])[:limit]:
        print(f"   {count:>7} 次  {method:<7} {path:<60} UA={ua}")
    print("\n② 里的接口说明调用方不在本仓库（公网链接／外部系统回调／旧版客户端），"
          "属于对外契约，需按契约废弃流程处理。")
    return zero_hit, hit_unexpected


def self_test():
    """用合成日志验证解析与分类：不依赖任何真实日志。"""
    candidates = load_candidates()
    if not candidates:
        sys.exit("静态候选为空，无法自检")
    hit_key = sorted(candidates)[0]
    silent_key = sorted(candidates)[-1]
    method, path = hit_key
    concrete = re.sub(r'\{\}', '12345', path)
    fake = [
        f'10.0.0.1 - - [19/Sep/2026:10:00:00 +0800] "{method} {concrete} HTTP/1.1" 200 12 "-" "self-test-UA"\n',
        '10.0.0.2 - - [19/Sep/2026:10:00:01 +0800] "GET /api/self-test-not-declared HTTP/1.1" 404 0 "-" "x"\n',
        'this line is not an access log line\n',
    ]
    hits, sample_ua, total, unparsed = parse_log([io.StringIO(''.join(fake))])
    ok = True
    if total != 3 or unparsed != 1:
        print(f"✗ 解析计数不符：total={total}（期望 3）, unparsed={unparsed}（期望 1）")
        ok = False
    if not hits.get(hit_key):
        print(f"✗ 命中未被识别：{hit_key}（合成路径 {concrete}）")
        ok = False
    if hits.get(silent_key):
        print(f"✗ 零命中项被误判为有命中：{silent_key}")
        ok = False
    if ok:
        print(f"✅ 自检通过：解析 3 行（1 行格式错误按预期跳过），"
              f"命中识别 {sum(hits.values())} 次，零命中项未被误判")
    return 0 if ok else 1


def main():
    parser = argparse.ArgumentParser(description='接口命中审计：用访问日志验证静态「未调用」候选')
    parser.add_argument('logs', nargs='*', help='访问日志文件；- 表示从标准输入读取')
    parser.add_argument('--self-test', action='store_true', help='用合成日志自检')
    parser.add_argument('--limit', type=int, default=60, help='每节最多打印条数（默认 60）')
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    if not args.logs:
        parser.print_help()
        return 2

    streams = []
    for path in args.logs:
        if path == '-':
            streams.append(sys.stdin)
        else:
            streams.append(open(path, encoding='utf-8', errors='replace'))
    try:
        candidates = load_candidates()
        hits, sample_ua, total, unparsed = parse_log(streams)
        report(candidates, hits, sample_ua, total, unparsed, args.limit)
    finally:
        for stream in streams:
            if stream is not sys.stdin:
                stream.close()
    return 0


if __name__ == '__main__':
    sys.exit(main())
