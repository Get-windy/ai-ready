
# Redis 基准测试数据结构

## String 类型
- user:{id}:name -> "test_user_{id}"
- user:{id}:email -> "user{id}@example.com"
- user:{id}:balance -> "1234.56"

## Hash 类型  
- user:{id} -> {name: "test_user_{id}", email: "user{id}@example.com", age: "25", balance: "1234.56"}

## Set 类型
- users:active -> {1, 2, 3, ...}
- users:inactive -> {...}
- users:suspended -> {...}

## Sorted Set 类型
- users:by_balance -> {1: 1234.56, 2: 2345.67, ...}
- users:by_created_at -> {1: 1640995200, 2: 1641081600, ...}

## List 类型
- user:{id}:activity_log -> ["login", "view_profile", "update_settings", ...]
