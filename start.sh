#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")"
PROJECT_DIR="$(pwd -P)"

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

# Jetty は日本語パスだと起動に失敗するため、ASCII のシンボリックリンク経由で起動する
LINK_DIR="/tmp/calendar-app"
rm -f "$LINK_DIR"
ln -sfn "$PROJECT_DIR" "$LINK_DIR"
cd "$LINK_DIR"

if lsof -ti :8080 >/dev/null 2>&1; then
  echo "ポート 8080 を使用中のプロセスを停止します..."
  lsof -ti :8080 | xargs kill 2>/dev/null || true
  sleep 1
fi

echo "起動中... http://localhost:8080/"
echo "終了: Ctrl+C"
echo "※ コード変更の再読込あり。反映されないときは Ctrl+C して再実行。"
echo

exec ./mvnw jetty:run
