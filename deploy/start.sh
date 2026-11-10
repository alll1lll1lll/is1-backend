#!/bin/sh
cd "$(dirname "$0")" || exit 1

if [ -f app.pid ] && kill -0 "$(cat app.pid)" 2>/dev/null; then
    echo "already running, pid $(cat app.pid)"
    exit 1
fi

if [ ! -s jwt.secret ]; then
    (umask 077 && openssl rand -base64 32 > jwt.secret)
fi

export PORT="${PORT:-24680}"
export DB_URL="${DB_URL:-jdbc:postgresql://pg:5432/studs}"
export DB_USER="${DB_USER:-$(whoami)}"
export DB_PASSWORD="${DB_PASSWORD:-$(head -n 1 ~/.pgpass | cut -d: -f5-)}"
export JWT_SECRET="$(cat jwt.secret)"

nohup java -Xmx512m -XX:MaxMetaspaceSize=192m -jar app.jar > app.log 2>&1 &
echo $! > app.pid
echo "started, pid $!, port $PORT, log: $(pwd)/app.log"
