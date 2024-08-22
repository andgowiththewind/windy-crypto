# this is a redis config file for master
port ${masterPort}
requirepass ${masterPassword}

# -------------------- General configuration copy from redis.windows.conf --------------------
tcp-backlog 511
timeout 0
tcp-keepalive 0
loglevel notice
logfile ""
databases 16

# RDB持久化配置：在指定的时间间隔内，如果超过指定数量的键被改变，则保存数据快照
# save 900 1: 如果至少有1个键被修改，900秒后自动保存数据快照到磁盘。
# save 300 10: 如果至少有10个键被修改，300秒后自动保存数据快照到磁盘。
# save 60 10000: 如果至少有10000个键被修改，60秒后自动保存数据快照到磁盘。
save 900 1
save 300 10
save 60 10000

stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes

# RDB文件名和存储目录
dbfilename dump.rdb
dir ./

# 复制配置：slave是否可以返回过期数据
slave-serve-stale-data yes

# 复制配置：slave是否为只读
slave-read-only yes

# 启用无盘同步，直接从内存向slave同步数据
repl-diskless-sync yes

# 无盘同步的延迟时间，以秒为单位
repl-diskless-sync-delay 2

repl-disable-tcp-nodelay no
slave-priority 100

# 启用AOF持久化
appendonly yes

# AOF文件名
appendfilename "appendonly.aof"

# AOF持久化的同步频率，每秒都同步
appendfsync everysec

no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-load-truncated yes
lua-time-limit 5000
slowlog-log-slower-than 10000
slowlog-max-len 128
latency-monitor-threshold 0
notify-keyspace-events ""
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-entries 512
list-max-ziplist-value 64
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
hll-sparse-max-bytes 3000
activerehashing yes

# 客户端输出缓冲区限制配置
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit slave 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60

hz 10
aof-rewrite-incremental-fsync yes
