#!/usr/bin/env bash

# 加载 zshrc 确保能读到 npm / python 等环境变量
if [ -f ~/.zshrc ]; then
    source ~/.zshrc
fi

echo "======================================"
echo "    ☄️ 启动刘爻 (Liuyao) 系统全模块"
echo "======================================"

# 1. 启动 Postgres 数据库容器
echo "[1/4] 启动 Postgres 数据库环境..."
cd liuyao-app
docker-compose up -d
cd ..

# 2. 编译并启动 Java 后端
echo "[2/4] 启动 Java 后端规则引擎 (8080)..."
if command -v mvn &> /dev/null; then
    cd liuyao-app
    nohup mvn spring-boot:run > ../app.log 2>&1 &
    APP_PID=$!
    echo $APP_PID > ../.liuyao_app.pid
    cd ..
    echo "  ✅ Java 后端已启动 (PID: $APP_PID)，日志输出至 app.log"
elif [ -f "../apache-maven-3.9.6/bin/mvn" ] || [ -f "apache-maven-3.9.6/bin/mvn" ]; then
    echo "  🔧 使用本地安装的 Maven 编译并启动..."
    cd liuyao-app
    nohup ../apache-maven-3.9.6/bin/mvn spring-boot:run > ../app.log 2>&1 &
    APP_PID=$!
    echo $APP_PID > ../.liuyao_app.pid
    cd ..
    echo "  ✅ Java 后端已启动 (PID: $APP_PID)，日志输出至 app.log"
else
    echo "  ⚠️ [警告] 终端环境中未检测到 mvn 命令。"
    echo "      脚本已跳过后端的自动启动。如果您习惯使用 IntelliJ IDEA 等 IDE，请在 IDE 内手动运行 LiuyaoAppApplication。"
fi

# 3. 启动 Python Worker
echo "[3/4] 启动 Python Worker (百炼知识提取核)..."
cd liuyao-worker
if [ -d "venv" ]; then
    source venv/bin/activate
fi
nohup python app/worker.py > ../worker.log 2>&1 &
WORKER_PID=$!
echo $WORKER_PID > ../.liuyao_worker.pid
cd ..
echo "  ✅ Python Worker 已启动 (PID: $WORKER_PID)，日志输出至 worker.log"

# 4. 启动 H5 前端 Vite 服务
echo "[4/4] 启动高定版 H5 交互前端 (5173)..."
cd liuyao-h5
if [ ! -d "node_modules" ]; then
    echo "  📦 正在为前端安装依赖，请稍候..."
    npm install
fi
nohup npm run dev > ../h5.log 2>&1 &
H5_PID=$!
echo $H5_PID > ../.liuyao_h5.pid
cd ..
echo "  ✅ H5 前端已启动 (PID: $H5_PID)，日志输出至 h5.log"

echo ""
echo "🚀 所有启动指令已分配完毕！"
echo "- 后端 Swagger API (如有)：http://localhost:8080"
echo "- 前端 H5 访问地址 (建议)：http://localhost:5173"
echo "如需一键停止或重启，请直接执行根目录的: ./stop.sh"
