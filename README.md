# 📚 LMS 图书馆管理系统



大家好！这是我在学习 Java Spring Boot 和前端技术时搭建的简易图书馆管理系统 (LMS)。项目实现了从用户认证到借阅、数据分析的闭环管理，适用于个人学习或毕业设计参考。



## ✨ 项目介绍



本项目旨在提供一个全栈的图书管理解决方案。后端采用当前主流的 **Spring Boot** 框架，实现高效稳定的 API 服务；前端使用轻量级的 **Vue.js 3** 和 **Tailwind CSS**，以实现快速开发和响应式界面。

**核心目标：** 彻底解决传统图书管理中库存、借阅、归还流程的效率问题，并提供基础数据分析功能。



## ⚙️ 技术栈



| **模块**            | **技术**                   | **描述**                             |
| ------------------- | -------------------------- | ------------------------------------ |
| **后端 (Backend)**  | Java 21 / Spring Boot 3.2+ | 核心业务逻辑实现，RESTful API 服务。 |
| **持久层 (DB)**     | MySQL                      | 存储用户、图书、借阅记录等核心数据。 |
| **安全/认证**       | Spring Security / JWT      | 基于 Token 的无状态认证和权限管理。  |
| **前端 (Frontend)** | Vue 3 (CDN) / JavaScript   | 界面渲染和状态管理。                 |
| **样式/UI**         | Tailwind CSS / Ion Icons   | 快速美观的样式框架和图标库。         |



## 🌟 核心功能点



我的系统支持三级用户权限体系：普通用户 (USER)、管理员 (ADMIN) 和超级管理员 (SUPERADMIN)。

- **用户认证与管理：** 注册、登录、JWT Token 鉴权。超级管理员可以进行用户 CRUD 和角色分配。
- **图书管理：** 图书入库、编辑、库存（`stock`）和可借阅量（`available`）自动管理。
- **借阅与归还：** 普通用户借书、续借、预约；管理员处理归还和预约转借阅。
- **借阅限制：** 支持设置最大借阅数量（默认 5 本）。
- **数据分析：** 实时统计热门图书 Top 5、活跃读者 Top 5 等数据。



## 🚀 本地环境快速启动





### 1. 前提条件



- Java Development Kit (JDK) 21+
- MySQL 8.0+
- Maven 或 Gradle



### 2. 数据库配置



1. 创建数据库（例如 `lms_db`）。
2. 修改后端项目中的 `application.properties` 或 `application.yml` 文件，更新 MySQL 连接信息、用户名和密码。
3. **初始化数据：** 运行应用，Spring Boot 会根据您的 JPA 实体自动创建表结构。



### 3. 后端启动 (Spring Boot)



Bash

```
# 进入后端项目根目录
cd lms-backend/

# 使用 Maven 编译打包
mvn clean install -DskipTests

# 运行 JAR 包
java -jar target/lms-backend-0.0.1-SNAPSHOT.jar 
```

- **默认端口：** 8080



### 4. 前端访问



由于我采用的是单文件 HTML (CDN 方式)，无需复杂的编译。

1. 将项目中的 `index.html` 文件直接用浏览器打开。
2. 或通过 `http://localhost:8080/index.html` 访问。



## ⚠️ 首次启动与管理员设置（关键）



由于 BCrypt 密码加密的特性，您不能直接在数据库中设置密码。

1. **启动应用后，请先注册一个新账号。**
2. 进入 MySQL 终端：`UPDATE users SET role = 'ROLE_SUPERADMIN', name = 'Super Admin' WHERE id = 1;` (假设 ID 为 1)。
3. 使用您注册时的密码和用户名登录。



## 🛑 常见问题与排查 (Troubleshooting)



我在开发和部署过程中遇到了一些关键问题，这里列出解决方案以供参考：

| **问题**                        | **根本原因**                                                 | **解决方案**                                                 |
| ------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **无法登录 (密码错误)**         | 外部工具生成的 BCrypt 哈希与应用环境不兼容。                 | **必须**通过前端注册新账号，然后通过数据库将其 `role` 提升为 `ROLE_SUPERADMIN`。 |
| **导航栏为空 / 身份显示“未知”** | 后端 `User` 实体作为 `JwtResponse` 返回时，`name` 字段被 Spring Security 序列化机制过滤。 | 引入 **`UserDTO`**，在 `AuthController` 中手动将 `User` 实体数据映射到 `UserDTO`，确保 `name` 和 `role` 字段完整返回。 |
| **借阅记录显示“图书已删除”**    | `RecordService` 直接返回原始 `BorrowRecord` 实体，前端无法解析 `bookId` 对应的 `title`。 | 在 `RecordService` 中创建 **`RecordDTO`**，并在 `getRecordsForUser` 方法中执行 `stream().map()` 映射，将 `book.title` 和 `user.name` 填充到 DTO 后再返回。 |
| **数据分析显示“未知图书”**      | 后端聚合查询 (JPQL/SQL) 缺少 `JOIN` 操作。                   | 在 `BorrowRecordRepository` 的 `@Query` 注解中，**必须**使用 `JOIN b.book` 和 `JOIN r.user` 将借阅记录与图书和用户表关联起来，才能在 `GROUP BY` 后获取名称。 |

------

**感谢您的阅读！希望这个项目能对您有所帮助。**
