# 开吃 · 今天吃什么

Vue 3 + Spring Boot 学习沙盘。维护一份「常点菜单」，纠结时一键随机抽一道。

## 技术栈

- 前端：Vue 3 + Vite + Vue Router（`frontend/`）
- 后端：Spring Boot 3.3 + JPA + **MySQL**（`backend/`）
- 测试：仍用内存 H2（不影响你日常开发）
- JDK：21（Temurin）

## 1. 安装 MySQL（本机）

```bash
brew install mysql
brew services start mysql
```

MySQL 会作为后台服务常驻（与是否启动后端无关）。停掉：`brew services stop mysql`。

### 库与账号

| 项   | 值            |
| ---- | ------------- |
| 库名 | `eatchoice`   |
| 用户 | `hsq`         |
| 密码 | `rollo664283` |

首次建库（用 `hsq`；若账号尚未创建，见下方「初始化账号」）：

```bash
mysql -h 127.0.0.1 -u hsq -p -e "CREATE DATABASE IF NOT EXISTS eatchoice DEFAULT CHARACTER SET utf8mb4;"
```

后端默认读 `backend/src/main/resources/application.yml` 里的 `hsq` / `rollo664283`。可用环境变量覆盖：

```bash
export MYSQL_USER='hsq'
export MYSQL_PASSWORD='rollo664283'
```

**`root` 已锁定**，不能再用 `root`（含空密码）登录。本机管理请用 `hsq`。

### 初始化账号（仅新机器 / 重装 MySQL 时）

在仍能用临时管理员登录时执行（示例：刚装完、root 尚未锁定）：

```sql
CREATE DATABASE IF NOT EXISTS eatchoice DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'hsq'@'localhost' IDENTIFIED BY 'rollo664283';
CREATE USER IF NOT EXISTS 'hsq'@'127.0.0.1' IDENTIFIED BY 'rollo664283';
GRANT ALL PRIVILEGES ON *.* TO 'hsq'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'hsq'@'127.0.0.1' WITH GRANT OPTION;
ALTER USER 'root'@'localhost' ACCOUNT LOCK;
FLUSH PRIVILEGES;
```

## 2. 安装 DBeaver（看表用）

Community 版免费：

```bash
brew install --cask dbeaver-community
```

或官网下载：https://dbeaver.io/download/

### 在 DBeaver 里连本地 MySQL

1. 打开 DBeaver → 新建连接 → 选 **MySQL**
2. 填：
   - Host：`localhost`（或 `127.0.0.1`）
   - Port：`3306`
   - Database：`eatchoice`
   - Username：`hsq`
   - Password：`rollo664283`
3. 测试连接 → 完成
4. 展开库 → 表 `dishes`（后端至少成功启动过一次后才会自动建表）
5. 右键表 → 查看数据 / 写 SQL：`SELECT * FROM dishes;`

命令行：

```bash
mysql -h 127.0.0.1 -u hsq -p eatchoice -e "SELECT * FROM dishes;"
```

## 3. 启动项目

```bash
# 后端
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export ZHIPU_API_KEY='你的智谱密钥'   # 普通用户问 AI
export KIMI_API_KEY='你的Kimi密钥'     # 管理员问 AI
cd backend
./mvnw spring-boot:run

# 前端（另开终端）
cd frontend
npm run dev
```

- 桌面：http://localhost:5174/（左侧大卡直接问 AI）
- 列表：http://localhost:8080/api/dishes/list
- 随机：http://localhost:8080/api/dishes/random

## 目录地图

```
浏览器 Vue（登录页 + 路由守卫）
  → /api/auth/... 、/api/dishes/... 、/api/ai/...
    → AuthController / DishController / AiController
      → Spring Security + JWT
      → MySQL：users、dishes
      → 问 AI：ADMIN→Kimi（KIMI_API_KEY），USER→智谱（ZHIPU_API_KEY）
```

## API

| 方法   | 路径                                | 权限   | 说明                        |
| ------ | ----------------------------------- | ------ | --------------------------- |
| POST   | `/api/auth/login`                   | 公开   | 登录，返回 JWT              |
| GET    | `/api/auth/me`                      | 登录   | 当前用户                    |
| GET    | `/api/dishes/list?q=&favoriteOnly=` | 登录   | 列表 / 搜索 / 只看收藏      |
| GET    | `/api/dishes/detail/{id}`           | 登录   | 详情                        |
| GET    | `/api/dishes/random?favoriteOnly=`  | 登录   | 随机抽一道                  |
| POST   | `/api/dishes/create`                | 管理员 | 创建                        |
| GET    | `/api/dishes/import-template`       | 管理员 | 下载导入用 Excel 模板       |
| POST   | `/api/dishes/import`                | 管理员 | 上传 Excel（字段名 `file`） |
| PUT    | `/api/dishes/update/{id}`           | 管理员 | 更新                        |
| DELETE | `/api/dishes/delete/{id}`           | 管理员 | 删除                        |
| POST   | `/api/ai/chat`                      | 登录   | 流式对话（ADMIN→Kimi，USER→GLM） |

字段：`name`, `note`, `tags`, `favorite`。

### 问 AI（按角色选模型）

桌面左侧大卡可直接对话。**管理员（admin）走 Kimi，普通用户（user）走智谱 GLM。**

| 角色  | 模型默认           | 环境变量 / 本地配置键 | 说明 |
| ----- | ------------------ | --------------------- | ---- |
| ADMIN | `kimi-for-coding`  | `KIMI_API_KEY`        | [Kimi Code](https://api.kimi.com/coding/) OpenAI：`https://api.kimi.com/coding/v1`，默认开思考 |
| USER  | `glm-4.7-flash`    | `ZHIPU_API_KEY`       | 智谱 GLM，关闭 thinking |

**推荐（配一次即可）**：复制示例为本地文件并填密钥（该文件已 gitignore）：

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入两个 api-key
```

也可用环境变量（或写进 `~/.zshrc` 持久化），会覆盖本地文件：

```bash
export ZHIPU_API_KEY='你的智谱密钥'
export KIMI_API_KEY='你的Kimi密钥'
```

接口：`POST /api/ai/chat`，body `{ "messages": [{ "role": "user", "content": "..." }] }`，响应为 SSE（`delta` / `done` / `error`）。

上下文：前端仍展示完整气泡；发给模型时若超过 12 条，后端会先把更早的对话压成摘要，只保留最近 6 条原文（摘要失败则硬截断）。

### 账号（启动自动种子）

| 用户名  | 密码       | 角色                              |
| ------- | ---------- | --------------------------------- |
| `admin` | `admin123` | 全部权限                          |
| `user`  | `user123`  | 只读（不可加菜 / 导入 / 改 / 删） |

请求头：`Authorization: Bearer <token>`。前端未登录会跳到 `/login`。

## 开发提示

- 已加 `spring-boot-devtools`：改 Java 并编译后会自动快速重启。
- 旧的 H2 文件在 `backend/data/`，可删，不再使用。

## 测试

```bash
cd backend
./mvnw test -Dtest=DishControllerTest
```
