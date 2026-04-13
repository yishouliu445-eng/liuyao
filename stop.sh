#!/usr/bin/env bash

echo "======================================"
echo "    🛑 停止刘爻 (Liuyao) 系统全模块"
echo "======================================"

if [ -f .liuyao_app.pid ]; then
    PID=$(cat .liuyao_app.pid)
    echo "停止 Java 后端 (PID: $PID)..."
    kill $PID 2>/dev/null || true
    rm .liuyao_app.pid
fi

if [ -f .liuyao_worker.pid ]; then
    PID=$(cat .liuyao_worker.pid)
    echo "停止 Python Worker (PID: $PID)..."
    kill $PID 2>/dev/null || true
    rm .liuyao_worker.pid
fi

if [ -f .liuyao_h5.pid ]; then
    PID=$(cat .liuyao_h5.pid)
    echo "停止 H5 前端 (PID: $PID)..."
    kill $PID 2>/dev/null || true
    rm .liuyao_h5.pid
fi

echo "停止 Postgres 数据库容器..."
cd liuyao-app && docker-compose down
cd ..

echo "✅ 所有系统组件已安全停止！"
