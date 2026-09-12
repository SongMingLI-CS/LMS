# LMS 图书馆管理系统 · 云服务器部署手册

将 LMS（Spring Boot 3.2 + MySQL 8 + 内嵌 Vue3 前端）部署到云服务器：由 Spring Boot 自身托管
前端静态资源与 REST 接口，无需额外 Nginx 网关。

## 0. 部署形态与前置条件

**关键差异**：本项目的 `Dockerfile` **只 COPY 现成 jar**（不做编译），前端由
`frontend-maven-plugin` 在 `mvn package` 时构建进 `src/main/resources/static/`。
因此**服务器无需 JDK / Maven / Node**，但**必须在本地先构建出产物**：

```powershell
cd <LMS 仓库根>
cd frontend; npm ci; npm run build; cd ..                     # vite outDir 指向 src/main/resources/static
mvn -B clean package "-DskipTests" "-Dfrontend.skip=true"     # 产出 target/lms-backend-0.0.1-SNAPSHOT.jar
```

> - 与 `.github/workflows/ci.yml` 的既有做法一致（前端单独构建 → Maven 跳过前端）。
> - **PowerShell 陷阱**：`-D*` 参数必须加引号，否则 `-Dfrontend.skip=true` 会被拆成
>   `-Dfrontend` 与 `.skip=true`，Maven 报 `Unknown lifecycle phase ".skip=true"`。

| 项 | 要求 |
|----|------|
| 服务器 | Linux x86_64，≥ 2 vCPU / ≥ 2 GiB 内存 / ≥ 5 GiB 磁盘 |
| Docker | Docker Engine 20.10+ 与 Compose v2（`docker compose`） |
| 端口 | 后端默认 **8080**（需安全组放行）；MySQL 默认仅绑定 `127.0.0.1:3306` |

## 1. 打包与上传

```powershell
# 仓库根：打包源码 + jar（排除 .git、node_modules 与 target 中间产物，仅保留 jar）
tar --exclude=./.git --exclude=./frontend/node_modules `
    --exclude=./target/classes --exclude=./target/test-classes `
    --exclude=./target/maven-status --exclude=./target/generated-sources `
    --exclude=./target/generated-test-sources `
    -czf lms-deploy.tar.gz .
# 体积约 70 MB（主要是 jar，jar 已是压缩包，gzip 收益有限）
```

推荐直接用驱动器（自动 SFTP 上传 → 解包 → 执行部署脚本 → 实时回显）：

```powershell
python -m pip install paramiko                      # 一次性
$env:YZ_SSH_PASSWORD = '<实例密码>'                  # 密码仅经环境变量传递，不进 shell 历史
python deploy/remote-deploy-runner.py --host <公网IP> --user ubuntu `
       --bundle lms-deploy.tar.gz --remote-dir /opt/lms
# 附加开关：--background（后台执行+写远端 deploy.log）/ --tail-log 60（轮询）
#           --skip-deploy（仅上传解包）/ --set-env K=V（透传变量，可重复）
#           --preflight-only（仅探测环境）/ --key <pem>（改用密钥认证）
```

## 2. 服务端脚本做了哪些事（幂等，可重复执行）

`deploy/remote-deploy.sh` 共六步：

| 步骤 | 内容 |
|------|------|
| 0 | 前置检查（jar 是否存在 / docker / compose v2 / 磁盘）+ CRLF→LF 行尾规范化 |
| 1 | Docker Hub 不通时自动写入镜像加速器并重启 Docker |
| 2 | 由 `.env.example` 生成 `.env`；**自动为 `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `JWT_SECRET` 生成强随机值**（48 位十六进制），并把 MySQL 绑定收敛为 `127.0.0.1:3306`、`APP_MAIL_ENABLED=false` |
| 3 | 端口占用检查（8080 / 3306） |
| 4 | `docker compose up -d --build` |
| 5 | 等待 `GET /` 返回 200，并验证 `GET /api/books`、`GET /login` |
| 6 | 输出访问地址与运维命令 |

> `docker-compose.yml` 对 `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` / `JWT_SECRET` 使用 `${VAR:?}`
> 强制要求（缺失即编排失败）。脚本在 `.env` 仍为模板占位值（`change-me*` / `your-*`）时自动
> 替换为强随机值；若你通过环境变量显式提供，则尊重你的值。

手动方式（等效）：

```powershell
scp -P <PORT> lms-deploy.tar.gz <USER>@<EIP>:/tmp/
```

```bash
sudo mkdir -p /opt/lms && sudo tar -xzf /tmp/lms-deploy.tar.gz -C /opt/lms
cd /opt/lms && sudo bash deploy/remote-deploy.sh
```

## 3. 部署后验证

```bash
cd /opt/lms
curl -s -o /dev/null -w 'SPA 首页 %{http_code}\n' http://127.0.0.1:8080/          # 期望 200
curl -s -o /dev/null -w '公开接口 %{http_code}\n' http://127.0.0.1:8080/api/books # 期望 200
curl -s -o /dev/null -w 'SPA 路由 %{http_code}\n' http://127.0.0.1:8080/login     # 期望 200
sudo docker compose --env-file .env -f docker-compose.yml ps
```

浏览器访问 `http://<公网IP>:8080/`。

**首个管理员账号**（见 README「首次启动与管理员权限设置」）：
BCrypt 哈希必须由 Spring Security 生成，**不能直接在库里插账号**。流程为：

1. 前端注册一个普通用户；
2. 进 MySQL 执行：
   ```sql
   UPDATE users SET role = 'ROLE_SUPERADMIN', name = 'Super Admin' WHERE id = 1;
   ```
3. 重新登录即为超级管理员。

## 4. 运维命令

```bash
cd /opt/lms
sudo docker compose --env-file .env -f docker-compose.yml ps
sudo docker compose --env-file .env -f docker-compose.yml logs -f lms-backend
sudo docker compose --env-file .env -f docker-compose.yml logs -f mysql-db
sudo docker compose --env-file .env -f docker-compose.yml down       # 停服务，保留 db-data 数据卷
```

**更新发布**：本机重新构建产物 → 重新打包 → 上传 → 再跑一次 `deploy/remote-deploy.sh`
（`db-data` 卷保留，Flyway 会自动应用新增迁移）。

**回滚**：用旧的 jar 重新打包上传后重跑脚本；若新迁移已改变 schema，需另按 Flyway 策略处理。

## 5. 常见问题

| 现象 | 处理 |
|------|------|
| `MYSQL_PASSWORD 必须设置` | 未生成 `.env`：直接执行 `deploy/remote-deploy.sh`（脚本会自动生成强随机值） |
| `no such file target/lms-backend-...jar` | 本地产物未构建或未打进包，见第 0 节 |
| PowerShell 报 `Unknown lifecycle phase ".skip=true"` | `-Dfrontend.skip=true` 必须加引号 |
| 公网访问不了 8080 | 控制台安全组放行 TCP 8080（MySQL 无需放行：已绑定 `127.0.0.1`） |
| 登录后无权限/看不到菜单 | 账号仍是 `USER`，需按第 3 节 SQL 提升为 `ROLE_SUPERADMIN` |
| 启动即退出 | `docker compose logs lms-backend`；常见为 MySQL 未就绪或迁移失败 |
| 前端页面是旧版 | `src/main/resources/static/` 未重新构建，重跑第 0 节前端构建后再打包 |

## 6. 部署期发现并修复的应用缺陷（GET 直链 /login 返回 500）

**现象**：浏览器**直接访问或刷新** `/login`、`/register`、`/forgot-password`、`/reset-password`
返回 HTTP 500（`HttpRequestMethodNotSupportedException: Request method 'GET' is not supported`）；
而 `/books`、`/profile` 等其它 SPA 路由正常（200）。

**根因**：`AuthController` 对这些路径**只有 POST 映射**。Spring MVC 的
`RequestMappingHandlerMapping`（order 0）在「路径能匹配、但请求方法不匹配」时会**直接抛异常**，
不会再交给 `WebConfig#addViewControllers` 注册的、order 更低的视图控制器映射。
实测对照：`GET /books` → 200（无控制器映射，落到视图控制器）vs `GET /login` → 500（被 POST-only
映射触发方法不匹配）。登录页恰是系统入口页，直链/刷新 500 会影响可用性。

**修法**：新增 `src/main/java/com/npu/lms/controller/AuthSpaRouteController.java`：

```java
@Controller
public class AuthSpaRouteController {
    @GetMapping({"/login", "/register", "/register/verify", "/forgot-password", "/reset-password"})
    public String authSpaRoutes() {
        return "forward:/index.html";
    }
}
```

要点：
- 必须是 `@Controller` 而**非** `@RestController` —— 后者的 `@ResponseBody` 语义会把返回字符串
  当响应体写出，不做视图解析；
- 与既有 POST 接口请求方法不同，无映射冲突；
- 路由清单与 `SecurityConfig` 中 `permitAll` 的前端路由保持一致。

> 部署后实测：`GET /login` → **HTTP 200**（此前 500）。

## 7. 部署记录

| 项 | 值 |
|----|----|
| 服务器 | 腾讯云轻量 `lhins-1rduasp8`（Ubuntu 24.04.4 LTS，4C/3.6G/40G） |
| 公网 / 内网 IP | `192.144.160.196` / `10.2.0.13` |
| 部署目录 | `/opt/lms` |
| 部署日志 | `/opt/lms/deploy.log`（使用 `--background` 时） |
| 后端端口 | 8080（**需在安全组放行 TCP 8080 才能公网访问**） |
| MySQL | 仅监听 `127.0.0.1:3306`，数据卷 `db-data`（不对外暴露） |
| 密钥 | `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `JWT_SECRET` 已自动生成强随机值并写入 `/opt/lms/.env`（`chmod 600`） |
| 验证结果 | `GET /` → 200；`GET /api/books` → 200（Flyway V1–V6 已应用、MySQL 连通）；`GET /login` → 200 |
| 与智汇于庄共存 | 端口无冲突：智汇于庄占 80/5433/6379；LMS 占 8080 与 3306（仅本机） |

