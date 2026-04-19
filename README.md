# SecureFile（端到端加密文件传输）

用户注册登录、本地 RSA 密钥、分片上传、按接收方加密的文件分发、下载解密与传输审计。服务端仅存密文分片与元数据，不持有用户私钥。

## 仓库结构

```
securefile/
├── backend/          # Spring Boot 2.7 + MyBatis-Plus + MySQL
└── frontend/         # Vue 3 + Vite + Element Plus + Pinia
```

## 功能概览

| 模块 | 说明 
|------|------|
| 认证 | 注册、登录、JWT、当前用户 `/api/v1/auth/me` |
| 密钥 | 上传/查询本用户公钥，按用户 ID 查询公钥（用于加密 AES 密钥） |
| 上传 | 分片初始化、上传、完成；大文件分片存储 |
| 下载 | 申请下载会话、按分片拉取密文、客户端 RSA 解出 AES 后解密 |
| 文件列表 | 我的文件、共享给我的文件、详情与删除 |
| 审计 | 传输相关日志分页查询 |

前端主要页面：首页、文件列表、上传、下载、密钥管理、审计、个人资料等（见 `frontend/src/router/index.js`）。

## 技术栈

**后端**

- Java 8、Spring Boot 2.7.18  
- MyBatis-Plus 3.5.x、MySQL 8（`schema.sql` 随应用初始化，见 `spring.sql.init`）  
- Hutool JWT、Knife4j（Swagger UI）  
- BCrypt 密码哈希  

**前端**

- Vue 3、Vue Router、Pinia  
- Element Plus、Axios  
- Web Crypto：RSA-OAEP、AES 分片加解密（逻辑见 `frontend/src/utils/crypto.js` 等）  

## 环境要求

- **JDK** 8+  
- **Maven** 3.6+  
- **MySQL** 8.x，并创建空库（例如 `secure_file`）  
- **Node.js** `^20.19.0` 或 `>=22.12.0`（见 `frontend/package.json` 的 `engines`）

## 数据库

1. 创建数据库。

2. 在 `backend/src/main/resources/application.yml` 中配置 JDBC URL、用户名与密码。

3. 启动后端时，若 `spring.sql.init.mode` 为 `always`，会使用 `classpath:schema.sql` 建表（可按需改为 `never` 并自行迁移）。

## 配置说明

编辑 `backend/src/main/resources/application.yml`：

- `server.port`：默认 `9002`  
- `spring.datasource.*`：MySQL 连接  
- `file.storage.base-path`：密文分片等文件的磁盘根目录（请改为本机可写路径）  


## 本地运行

### 1. 启动后端

```bash
cd backend
mvn -q -DskipTests spring-boot:run
```

或打包后运行：

```bash
mvn -q -DskipTests package
java -jar target/securefile-1.0.0.jar
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

开发环境下，Vite 将 `/api` 代理到 `http://localhost:9002`（见 `frontend/vite.config.js`）。浏览器访问控制台输出的本地地址（通常为 `http://localhost:5173`）。

### 3. API 文档

服务启动后，在浏览器打开：

- Knife4j：`http://localhost:9002/doc.html`  

（具体路径以 Knife4j 默认配置为准。）

## 运行后使用说明

### 1. 注册并登录

1. 打开前端地址（默认 `http://localhost:5173`）。  
2. 在注册页创建账号并登录。  
3. 登录成功后进入首页/主布局。
<img width="2826" height="1518" alt="71d4ea40757ed0475c687a54ef38f7de" src="https://github.com/user-attachments/assets/d9b17da7-f247-4eae-8f2a-5e8c30df97d1" />
<img width="2803" height="1843" alt="3ef93f76e9792f861ca6d35b07b399b3" src="https://github.com/user-attachments/assets/337ca947-7ada-46b8-b463-76617f0fcabf" />

### 2. 生成并上传公钥

1. 进入“密钥管理”页面。  
2. 点击生成 RSA 密钥对（私钥保存在浏览器本地）。  
3. 点击上传公钥到服务端。
<img width="2842" height="1518" alt="6e42eb26602a9c21f6a6d8e85b09cfb6" src="https://github.com/user-attachments/assets/3a15e06f-8c75-47d8-8e3b-79b2752ae882" />


### 3. 上传文件并设置接收者

1. 进入“文件上传”页面，选择文件。  
2. 选择接收用户（系统会按接收者公钥封装密钥）。  
3. 完成分片上传与合并。
<img width="2811" height="2053" alt="9d9ada56471cdf5b679f98e1f501b3bf" src="https://github.com/user-attachments/assets/75220308-b26e-4900-8a83-a9ddac2a191a" />


### 4. 接收方下载并解密

1. 接收方登录后进入“我的文件/共享给我”。  
2. 选择文件发起下载。  
3. 客户端使用本地私钥解密并合并分片，得到明文文件。
<img width="2822" height="1514" alt="8f7e5159804909f4776e38c730bca81e" src="https://github.com/user-attachments/assets/6e52c8fb-6b68-42e8-aaa2-5f6967d7431a" />
<img width="2822" height="1510" alt="f14f826676e472ac67da4f384e134733" src="https://github.com/user-attachments/assets/7e58a1b3-4d38-4def-a428-95f86cefe4f2" />


### 5. 查看审计日志

1. 打开“审计”页面。  
2. 查询上传、下载等传输记录。
<img width="2828" height="1518" alt="5fe2f65ba22223e13d1b4e7cd19419bf" src="https://github.com/user-attachments/assets/7215dac9-d02b-4801-a27a-c4e9e978a064" />


## 致谢

- 感谢开源社区与相关项目：Spring Boot、MyBatis-Plus、Vue、Vite、Element Plus、Axios、Hutool、Knife4j。  
- 感谢所有测试与反馈本项目问题的同学和朋友。  
- 本项目中的加密与鉴权实现用于学习与工程实践演示，欢迎提出改进建议。

## License

本项目采用 MIT License，详见 LICENSE 文件。
