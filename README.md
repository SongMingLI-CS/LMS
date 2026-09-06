# 📚 Full-Stack 图书馆管理系统 (LMS)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![JDK Version](https://img.shields.io/badge/JDK-21+-orange.svg)](https://www.oracle.com/java/technologies/javase/21-api-spec.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Frontend](https://img.shields.io/badge/Frontend-Vue%203-4FC08D.svg)](https://vuejs.org/)

本项目是一个全栈的图书馆管理系统，旨在整合现代主流技术栈，实现从用户认证到借阅管理的完整闭环。适用于个人学习、课程设计或作为 Spring Boot + Vue 实践项目的参考。

## ✨ 项目亮点与技术栈

我们通过前后端分离的方式，构建了一个高性能、易维护的管理平台。

| **模块** | **技术** | **关键特性** |
| :--- | :--- | :--- |
| **后端 API** | ☕ Java 21 / Spring Boot 3.2+ | 基于 RESTful 架构的核心业务逻辑和高效 API 服务。 |
| **安全与认证** | 🔒 Spring Security / JWT | 采用无状态 Token 鉴权机制，实现灵活的权限控制。 |
| **数据持久化** | 💾 MySQL 8.0+ | 高性能数据存储，利用 JPA 自动建表。 |
| **前端界面** | 💚 Vue 3 (SFC) / Vite | 组件化拆分：8 页面视图 + 5 通用组件 + 集中 store，支持热更新与生产构建。 |
| **样式与交互** | 🎨 Tailwind CSS (编译版) / CSS 图表 | 渐变玻璃拟态设计体系，原生 CSS 图表无重量级依赖，Vue/Axios/Tailwind 均已本地打包。 |

## 🌟 核心功能点概览

系统支持 **普通用户 (USER)**、**管理员 (ADMIN)** 和 **超级管理员 (SUPERADMIN)** 三级权限体系。

* **用户管理**：提供注册、登录、找回密码等功能；超管可进行用户 CRUD 和角色分配。
* **图书管理**：支持图书的入库、编辑、批量导入，并自动维护 `stock` (库存) 和 `available` (可借阅量)。
* **借阅流程**：支持用户借阅、续借、预约；管理员处理归还和预约转借阅操作。
* **数据分析**：实时展示热门图书 Top 5、活跃读者 Top 5、库存预警等关键运营数据。

## ⚙️ 本地环境快速启动指南

### 1. 前提条件

确保您的环境中已安装：
* Java Development Kit (JDK) **21+**
* MySQL **8.0+**
* Maven 或 Gradle
* （可选，仅前端独立开发时需要）Node.js **18+** 与 npm —— Maven 构建会自动通过 `frontend-maven-plugin` 下载 Node 并完成前端构建

### 2. 数据库初始化

1.  在本地 MySQL 中创建新的数据库，例如 `lms_db`。
2.  更新后端项目 `application.yml` 或 `application.properties` 文件中的 `spring.datasource` 配置项。
3.  **重要提示：** 启动应用后，Spring Boot 会自动创建所需的表结构。

### 3. 后端启动 (Spring Boot)

```bash
# 进入后端项目根目录
cd lms-backend/

# 使用 Maven 编译打包 (跳过单元测试)
mvn clean install -DskipTests

# 运行生成的 JAR 包
java -jar target/lms-backend-0.0.1-SNAPSHOT.jar
```

> 默认 API 端口为 `8080`。



### 4. 前端工程化（Vite + Vue 3 SFC）

> 前端源码位于仓库根目录 `frontend/`，产物自动构建到 `src/main/resources/static/`，由 Spring Boot 直接托管。

**开发模式（热更新）：**
```bash
cd frontend
npm ci        # 首次拉取依赖
npm run dev   # 启动 Vite 开发服务器 http://localhost:5173
```
> Vite 已配置代理：`/api`、`/login` 等请求会转发到本地 `http://localhost:8080`，开发时请保持后端运行。

**生产构建（自动集成到 Maven）：**
```bash
# 在项目根目录执行即可 —— frontend-maven-plugin 会自动 npm ci + npm run build
mvn clean package -DskipTests
java -jar target/lms-backend-0.0.1-SNAPSHOT.jar   # http://localhost:8080
```

> - 如需跳过前端自动构建：`mvn package -Dfrontend.skip=true`
> - 手动仅构建前端：`cd frontend && npm run build`
> - `src/main/resources/static/` 为**构建产物目录**（已 gitignore），不要在源码仓库中手工修改它；改动一律提交 `frontend/` 下源码。

**目录结构：**
```
frontend/
├─ src/
│  ├─ App.vue                 # 应用根组件（装配 + provide 中央 store）
│  ├─ lms.js                  # 中央状态与全部业务逻辑（createLmsStore）
│  ├─ style.css               # Tailwind 指令 + 全局玻璃拟态设计体系
│  ├─ views/                  # DashboardView / AnalysisView / BooksView / RecordsView /
│  │                          # AuditLogsView / BorrowManageView / ProfileView / UsersView
│  └─ components/             # AuthShell / Sidebar / TopHeader / ToastStack / Modal
└─ vite.config.js             # 输出 ../src/main/resources/static；dev 代理 :8080
```

> 依赖说明：Vue 3、Axios、Tailwind CSS 均已随 Vite 本地打包（不再依赖 CDN）；仅图标库 `ionicons` 仍以 Web Component 方式从 CDN 加载（后续可自托管 SVG 进一步离线化）。



## ⚠️ 首次启动与管理员权限设置（关键步骤）



由于 Spring Security 使用 **BCrypt** 加密密码，**不能**直接在数据库中插入管理员账号。

1. **前端注册：** 启动应用后，请先通过前端界面注册一个普通用户账号。
2. **数据库赋权：** 停止应用后，进入 MySQL 终端执行以下 SQL，将新注册账号的角色提升为超级管理员：

SQL

```
-- 假设您刚注册的用户的 ID 为 1
UPDATE users SET role = 'ROLE_SUPERADMIN', name = 'Super Admin' WHERE id = 1;
```

1. 重启应用，即可使用该账号的用户名和密码以超级管理员身份登录。

------



## 💡 架构决策与经验总结



该表格总结了开发过程中遇到的关键挑战及解决方案，是项目中最有价值的经验沉淀：

| **挑战 (Challenge)**      | **背景/根本原因**                                            | **解决方案 (Lesson Learned)**                                |
| ------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **安全登录/密码初始化**   | BCrypt 算法特性导致外部生成的哈希不兼容。                    | **[安全]** 强制要求用户通过前端注册，然后通过 SQL 提升权限，确保密码哈希值由 Spring Security 生成。 |
| **Token认证返回数据不全** | Spring Security 序列化机制默认过滤了 `User` 实体中的部分字段（如 `name`）。 | **[DTO/安全]** 引入 **`UserDTO`**，在 `AuthController` 中执行手动映射，确保敏感数据安全且非敏感数据完整返回。 |
| **借阅记录无法显示名称**  | `RecordService` 默认只返回原始 `bookId` 和 `userId`。        | **[映射/性能]** 在 Service 层创建 **`RecordDTO`**，通过 `stream().map()` 提前将 `book.title` 和 `user.name` 填充到 DTO 中返回，避免前端二次查询。 |
| **数据分析聚合查询**      | JPQL/SQL 聚合查询（如 `GROUP BY bookId`）时，无法直接获取 `title` 字段。 | **[ORM]** 在 `Repository` 的 `@Query` 中，**必须**使用 `JOIN FETCH` 将 `BorrowRecord` 与 `Book` 表关联，才能在聚合查询中获取非聚合字段。 |
