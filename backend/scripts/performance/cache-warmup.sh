#!/bin/bash
# AI-Ready Cache Warmup Script
# 缓存预热脚本

set -e

REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}
API_HOST=${API_HOST:-http://localhost:8080}

echo "=========================================="
echo "AI-Ready Cache Warmup Started"
echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# Redis连接参数
REDIS_CLI="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
if [ -n "$REDIS_PASSWORD" ]; then
    REDIS_CLI="$REDIS_CLI -a $REDIS_PASSWORD"
fi

# 1. 预热用户数据
echo "[1/5] Warming up user data..."
$REDIS_CLI <<EOF
  EVAL "local users = redis.call('smembers', 'warmup:users'); for i=1,#users do redis.call('expire', 'user:'..users[i], 3600); end; return #users;" 0
EOF

# 2. 预热配置数据
echo "[2/5] Warming up configuration data..."
curl -s -X POST "$API_HOST/api/config/cache-warmup" \
  -H "Content-Type: application/json" \
  -d '{"types":["system","business","ui"]}' | jq -r '.message // "Config warmup initiated"'

# 3. 预热权限数据
echo "[3/5] Warming up permission data..."
$REDIS_CLI <<EOF
  EVAL "local perms = redis.call('keys', 'perm:*'); for i=1,#perms do redis.call('expire', perms[i], 7200); end; return #perms;" 0
EOF

# 4. 预热字典数据
echo "[4/5] Warming up dictionary data..."
curl -s "$API_HOST/api/dict/all" > /dev/null

# 5. 预热热点数据
echo "[5/5] Warming up hot data..."
$REDIS_CLI <<EOF
  ZREVRANGE hot:data:access 0 99 WITHSCORES
  EVAL "redis.call('expire', 'hot:data:access', 600); return 1;" 0
EOF

# 统计信息
echo ""
echo "=========================================="
echo "Cache Warmup Statistics"
echo "=========================================="
$REDIS_CLI INFO keyspace | grep -E "^db" || echo "No keyspace info available"
$REDIS_CLI INFO stats | grep -E "^(keyspace_hits|keyspace_misses)" || true

echo ""
echo "=========================================="
echo "Cache Warmup Completed!"
echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="