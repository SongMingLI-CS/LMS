#!/usr/bin/env bash
# ============================================================
# LMS 图书馆管理系统 · 云服务器一键部署脚本（在【目标 Linux 服务器】上执行）
#
# 用法（脚本自动定位仓库根，可在任意目录调用）：
#   bash deploy/remote-deploy.sh
#   BACKEND_PORT=8080 MYSQL_HOST_PORT=127.0.0.1:3306 bash deploy/remote-deploy.sh
#
# 前置：仓库内必须已存在构建产物 target/lms-backend-0.0.1-SNAPSHOT.jar
#   （本机先构建：cd frontend && npm ci && npm run build
#                 mvn -B clean package "-DskipTests" "-Dfrontend.skip=true"）
#   本脚本只做「编排 + 启动 + 校验」，不在服务器上编译（服务器无需 JDK/Maven/Node）。
#
# 特性：幂等可重复执行；不会删除 MySQL 数据卷 db-data。
#
# 可覆盖环境变量：
#   COMPOSE_FILE       默认 docker-compose.yml（本仓库 compose 位于仓库根）
#   ENV_FILE           默认 <仓库根>/.env
#   JAR_PATH           默认 target/lms-backend-0.0.1-SNAPSHOT.jar
#   BACKEND_PORT       后端对外端口，默认 8080
#   MYSQL_HOST_PORT    MySQL 宿主映射，默认 127.0.0.1:3306（仅本机可达，避免数据库暴露公网）
#   MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD / JWT_SECRET
#                      未提供且 .env 仍为模板占位值时，自动生成强随机值（48 位十六进制）
#   APP_MAIL_ENABLED   默认 false（无 SMTP 凭据时不影响注册/找回密码流程）
#   NO_MIRROR=1        跳过 Docker 镜像加速器自动配置
#   VERIFY_WAIT        就绪等待上限秒，默认 300
# ============================================================
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
ENV_FILE="${ENV_FILE:-${ROOT_DIR}/.env}"
JAR_PATH="${JAR_PATH:-target/lms-backend-0.0.1-SNAPSHOT.jar}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
MYSQL_HOST_PORT="${MYSQL_HOST_PORT:-127.0.0.1:3306}"
NO_MIRROR="${NO_MIRROR:-0}"
VERIFY_WAIT="${VERIFY_WAIT:-300}"

# docker compose 必须以「数组」保存再展开（"${COMPOSE_CMD[@]}"）：按字符串保存并加引号
# 执行会把 "docker compose" 当成单个命令名 → command not found（rc=127）。
COMPOSE_CMD=(docker compose)
if [[ -n "${COMPOSE:-}" ]]; then
    read -r -a COMPOSE_CMD <<< "${COMPOSE}"
fi

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1; then SUDO="sudo"; fi
fi

if [ -t 1 ]; then
    GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; BOLD=$'\033[1m'; NC=$'\033[0m'
else
    GREEN=''; RED=''; YELLOW=''; BOLD=''; NC=''
fi
say()  { printf '%s\n' "$*"; }
ok()   { printf '%s[ OK ]%s %s\n' "${GREEN}" "${NC}" "$*"; }
warn() { printf '%s[WARN]%s %s\n' "${YELLOW}" "${NC}" "$*"; }
die()  { printf '%s[FAIL]%s %s\n' "${RED}" "${NC}" "$*" >&2; exit 1; }

run_compose() { "${COMPOSE_CMD[@]}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"; }

env_get() { sed -n -E "s|^$1=(.*)$|\1|p" "${ENV_FILE}" | tail -n 1 | tr -d '\r'; }

http_code() {
    curl -s -o /dev/null -w '%{http_code}' \
        --connect-timeout 3 --max-time 8 "$1" 2>/dev/null || true
}

echo "${BOLD}==> LMS 图书馆管理系统 · 云服务器部署 @ $(hostname) [${ROOT_DIR}]${NC}"

# ---------- 0) 前置检查 ----------
echo
echo "${BOLD}[0/6] 前置检查${NC}"
[ -f "${COMPOSE_FILE}" ] || die "未找到编排文件 ${COMPOSE_FILE}，请确认本脚本位于仓库 deploy/ 目录内。"
if [ ! -f "${JAR_PATH}" ]; then
    die "未找到构建产物 ${JAR_PATH}。请先在本机执行：
    cd frontend && npm ci && npm run build
    mvn -B clean package \"-DskipTests\" \"-Dfrontend.skip=true\""
fi
ok "构建产物就绪：${JAR_PATH}（$(du -h "${JAR_PATH}" | cut -f1)）"
command -v docker >/dev/null 2>&1 || die "未检测到 docker，请先安装 Docker Engine：curl -fsSL https://get.docker.com | sh"
docker info >/dev/null 2>&1 || die "Docker 守护进程不可用，请先启动：${SUDO} systemctl enable --now docker"
"${COMPOSE_CMD[@]}" version >/dev/null 2>&1 || die "未检测到 Docker Compose v2 插件（docker compose），请安装 docker-compose-plugin。"
ok "$("${COMPOSE_CMD[@]}" version --short 2>/dev/null || docker --version)"
AVAIL_KB="$(df -Pk "${ROOT_DIR}" | awk 'NR==2 {print $4}')"
if [ "${AVAIL_KB:-0}" -lt 5242880 ]; then
    warn "磁盘可用空间仅 $(( ${AVAIL_KB:-0} / 1024 ))MB（建议 ≥ 5GB）"
else
    ok "磁盘可用空间 $(( AVAIL_KB / 1024 / 1024 ))GB"
fi

# 行尾规范化：Windows 工作区（core.autocrlf=true）经 scp/tar 上传后，*.sh 会是 CRLF，
# 在 Linux 上执行会报 "set: pipefail: invalid option name"（已实测复现）。
# 本仓库 compose/Dockerfile 为 CRLF 无妨（Docker/Compose 均可解析），仅收敛脚本与 .env*。
mapfile -t CRLF_FILES < <(
    grep -rlIU $'\r' \
        "${ROOT_DIR}/.env" "${ROOT_DIR}/.env.example" \
        "${ROOT_DIR}/deploy" 2>/dev/null || true
)
if [ "${#CRLF_FILES[@]}" -gt 0 ]; then
    warn "检测到 ${#CRLF_FILES[@]} 个 CRLF 文本文件（Windows 工作区特征），规范化为 LF..."
    if command -v dos2unix >/dev/null 2>&1; then
        dos2unix -q "${CRLF_FILES[@]}"
    else
        sed -i 's/\r$//' "${CRLF_FILES[@]}"
    fi
    ok "行尾规范化完成（deploy/ 与 .env*）"
else
    ok "行尾均为 LF，无需规范化"
fi

# ---------- 1) Docker 镜像加速器 ----------
echo
echo "${BOLD}[1/6] 镜像源连通性${NC}"
HUB_CODE="$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 5 https://registry-1.docker.io/v2/ 2>/dev/null || true)"
if [ -n "${HUB_CODE}" ] && [ "${HUB_CODE}" != "000" ]; then
    ok "Docker Hub 可达（HTTP ${HUB_CODE}），无需加速器"
elif [ "${NO_MIRROR}" = "1" ]; then
    warn "Docker Hub 不可达，但 NO_MIRROR=1 已指定跳过自动配置"
elif [ -z "${SUDO}" ] && [ "$(id -u)" -ne 0 ]; then
    warn "Docker Hub 不可达，但当前用户无 root/sudo，无法写入 /etc/docker/daemon.json"
else
    warn "Docker Hub 不可达，正在写入镜像加速器并重启 Docker..."
    DAC="/etc/docker/daemon.json"
    ${SUDO} mkdir -p /etc/docker
    if [ -f "${DAC}" ]; then
        ${SUDO} cp -n "${DAC}" "${DAC}.bak.$(date +%Y%m%d%H%M%S)" || true
    fi
    if command -v python3 >/dev/null 2>&1; then
        ${SUDO} python3 - "${DAC}" <<'PY'
import json, os, sys
path = sys.argv[1]
cfg = {}
if os.path.exists(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            cfg = json.load(fh) or {}
    except Exception:
        cfg = {}
cfg["registry-mirrors"] = [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io",
    "https://docker.nju.edu.cn",
    "https://docker.1ms.run",
]
with open(path, "w", encoding="utf-8") as fh:
    json.dump(cfg, fh, ensure_ascii=False, indent=2)
    fh.write("\n")
print("daemon.json updated:", path)
PY
    else
        warn "无 python3：请手工在 ${DAC} 添加 registry-mirrors 后重启 Docker"
    fi
    ${SUDO} systemctl restart docker 2>/dev/null || warn "systemctl restart docker 失败，请手动重启 Docker"
    for _ in $(seq 1 30); do docker info >/dev/null 2>&1 && break; sleep 1; done
    docker info >/dev/null 2>&1 && ok "Docker 已重启且可用" || die "Docker 重启后仍不可用，请检查 /etc/docker/daemon.json"
fi

# ---------- 2) .env 与密钥 ----------
echo
echo "${BOLD}[2/6] 环境变量与密钥${NC}"
if [ ! -f "${ENV_FILE}" ]; then
    [ -f .env.example ] || die "缺少 .env.example，无法生成 ${ENV_FILE}"
    cp .env.example "${ENV_FILE}"
    ok "已由 .env.example 生成 ${ENV_FILE}"
else
    ok "复用既有 ${ENV_FILE}"
fi

set_env_kv() {
    local key="$1" value="${2:-}"
    [ -n "${value}" ] || return 0
    if grep -qE "^${key}=" "${ENV_FILE}"; then
        # 以 | 作 sed 分隔符，避免值中的 / 破坏替换
        sed -i.bak -E "s|^${key}=.*$|${key}=${value}|" "${ENV_FILE}" && rm -f "${ENV_FILE}.bak"
    else
        printf '%s=%s\n' "${key}" "${value}" >> "${ENV_FILE}"
    fi
}

gen_secret() {
    # $1 = 需要的字符数（默认 48）。优先用 openssl 生成十六进制串（仅含 [0-9a-f]）
    local chars="${1:-48}"
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -hex $(( chars / 2 )) 2>/dev/null && return 0
    fi
    LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom 2>/dev/null | head -c "${chars}" || true
}

# 占位值判定：空、change-me*、your-*、含 CHANGE_ME 均视为「未配置」
is_placeholder() {
    case "${1:-}" in
        ""|change-me*|your-*|*CHANGE_ME*) return 0 ;;
        *) return 1 ;;
    esac
}

ensure_secret() {
    # $1=键名 $2=环境变量提供的值 $3=最小长度（既有值短于该长度时视为不合格并重新生成）
    local key="$1" provided="${2:-}" minlen="${3:-32}" cur gen
    cur="$(env_get "${key}")"
    if [ -n "${provided}" ]; then
        set_env_kv "${key}" "${provided}"
        ok "  ${key}：由环境变量提供"
    elif is_placeholder "${cur}" || [ "${#cur}" -lt "${minlen}" ]; then
        if [ -n "${cur}" ] && ! is_placeholder "${cur}"; then
            warn "  ${key}：既有值仅 ${#cur} 字符，低于要求的 ${minlen}，重新生成"
        fi
        gen="$(gen_secret $(( minlen * 2 )))"
        [ -n "${gen}" ] || die "生成 ${key} 随机值失败，请手工在 .env 中配置"
        set_env_kv "${key}" "${gen}"
        ok "  ${key}：已自动生成强随机值（$(( minlen * 2 )) 字符）"
    else
        ok "  ${key}：沿用既有配置"
    fi
}

# docker-compose.yml 对以下三项使用 ${VAR:?} 强制要求，缺失会直接导致编排失败
ensure_secret MYSQL_ROOT_PASSWORD "${MYSQL_ROOT_PASSWORD:-}" 32
ensure_secret MYSQL_PASSWORD "${MYSQL_PASSWORD:-}" 32
# JWT_SECRET：项目用 HS512 签发 token，JJWT 要求密钥字符串 ≥ 64 字节（512 bit）。
# 过短时【启动不报错】，但登录签发 token 会抛 WeakKeyException → HTTP 500，务必保证长度。
ensure_secret JWT_SECRET "${JWT_SECRET:-}" 64

set_env_kv BACKEND_PORT "${BACKEND_PORT}"
# 默认只监听 127.0.0.1：MySQL 无需对外暴露（后端经容器网络 mysql-db:3306 访问）
set_env_kv MYSQL_HOST_PORT "${MYSQL_HOST_PORT}"
set_env_kv APP_MAIL_ENABLED "${APP_MAIL_ENABLED:-false}"
chmod 600 "${ENV_FILE}" 2>/dev/null || true

LMS_DB="$(env_get MYSQL_DATABASE)"; LMS_DB="${LMS_DB:-lms_db}"
LMS_USER="$(env_get MYSQL_USER)"; LMS_USER="${LMS_USER:-lmsuser}"
ok "后端端口=${BACKEND_PORT} / MySQL 映射=${MYSQL_HOST_PORT} / 库=${LMS_DB} / 用户=${LMS_USER}"

# ---------- 3) 端口占用检查 ----------
echo
echo "${BOLD}[3/6] 端口占用检查${NC}"
port_busy() {
    if command -v ss >/dev/null 2>&1; then
        ss -lnt 2>/dev/null | awk 'NR>1 {print $4}' | grep -qE "[:.]$1$"
    else
        netstat -lnt 2>/dev/null | awk 'NR>2 {print $4}' | grep -qE "[:.]$1$"
    fi
}
for p in "${BACKEND_PORT}" "${MYSQL_HOST_PORT##*:}"; do
    if port_busy "${p}"; then
        if docker ps --format '{{.Ports}}' 2>/dev/null | grep -qE "[:.]${p}->"; then
            warn "端口 ${p} 已被本机 Docker 容器占用（若为本项目容器，部署会自动收敛）"
        else
            warn "端口 ${p} 被非本项目进程占用；如冲突请显式指定 BACKEND_PORT/MYSQL_HOST_PORT 后重跑"
        fi
    else
        ok "端口 ${p} 空闲"
    fi
done

# ---------- 4) 构建镜像并启动 ----------
echo
echo "${BOLD}[4/6] 构建镜像并启动（lms-backend + mysql-db）${NC}"
say "    首次启动需拉取 eclipse-temurin / mysql 基础镜像，请耐心等待..."
if ! run_compose up -d --build; then
    warn "构建或启动失败，最近日志如下："
    run_compose logs --tail 60 || true
    die "部署失败：docker compose up -d --build 未成功"
fi
run_compose ps

# ---------- 5) 就绪等待 + 接口验证 ----------
echo
echo "${BOLD}[5/6] 服务就绪等待（上限 ${VERIFY_WAIT}s）${NC}"
waited=0
while :; do
    code="$(http_code "http://127.0.0.1:${BACKEND_PORT}/")"
    if [ "${code}" = "200" ]; then
        break
    fi
    if [ "${waited}" -ge "${VERIFY_WAIT}" ]; then
        echo
        warn "后端 ${VERIFY_WAIT}s 内未就绪，最近日志如下："
        run_compose logs --tail 80 lms-backend || true
        die "部署失败：后端未就绪"
    fi
    printf '.'
    sleep 5
    waited=$((waited + 5))
done
echo
ok "SPA 首页可访问（约 ${waited}s）"

VERIFY_FAIL=0
api_code="$(http_code "http://127.0.0.1:${BACKEND_PORT}/api/books")"
if [ "${api_code}" = "200" ]; then
    ok "GET /api/books -> HTTP 200（公开接口；Flyway 迁移与 MySQL 已连通）"
else
    warn "GET /api/books -> HTTP ${api_code:-timeout/refused}（期望 200）"
    VERIFY_FAIL=1
fi
login_code="$(http_code "http://127.0.0.1:${BACKEND_PORT}/login")"
if [ "${login_code}" = "200" ]; then
    ok "GET /login -> HTTP 200（SPA 路由可达）"
else
    warn "GET /login -> HTTP ${login_code:-timeout/refused}（期望 200）"
    VERIFY_FAIL=1
fi
if [ "${VERIFY_FAIL}" -ne 0 ]; then
    run_compose logs --tail 60 lms-backend || true
    die "部署后接口验证未通过"
fi

# ---------- 6) 汇总 ----------
HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
[ -n "${HOST_IP}" ] || HOST_IP="<服务器地址>"
echo
echo "----------------------------------------------"
printf '%s%s[部署完成] LMS 图书馆管理系统已上线%s\n' "${GREEN}" "${BOLD}" "${NC}"
echo "  前端 + 后端 : http://${HOST_IP}:${BACKEND_PORT}/"
echo "  公开接口     : http://${HOST_IP}:${BACKEND_PORT}/api/books"
echo "  MySQL        : 仅监听 ${MYSQL_HOST_PORT}（不对公网暴露）"
echo
say "运维命令（在 ${ROOT_DIR} 执行）："
say "  查看状态  ${COMPOSE_CMD[*]} --env-file ${ENV_FILE} -f ${COMPOSE_FILE} ps"
say "  查看日志  ${COMPOSE_CMD[*]} --env-file ${ENV_FILE} -f ${COMPOSE_FILE} logs -f"
say "  停止服务  ${COMPOSE_CMD[*]} --env-file ${ENV_FILE} -f ${COMPOSE_FILE} down"
say "  进入 MySQL ${COMPOSE_CMD[*]} --env-file ${ENV_FILE} -f ${COMPOSE_FILE} exec mysql-db sh -c 'mysql -u root -p\"\$MYSQL_ROOT_PASSWORD\" lms_db'"
warn "若为云主机，请在安全组放行 TCP ${BACKEND_PORT}，否则公网无法访问前端与接口。"
warn "首个管理员账号：先经前端注册普通用户，再按 README 执行 SQL 提升为 SUPERADMIN。"
