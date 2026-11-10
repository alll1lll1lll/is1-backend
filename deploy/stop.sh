#!/bin/sh
cd "$(dirname "$0")" || exit 1

if [ -f app.pid ] && kill "$(cat app.pid)" 2>/dev/null; then
    echo "stopped"
else
    echo "not running"
fi
rm -f app.pid
