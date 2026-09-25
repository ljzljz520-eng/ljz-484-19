# 开发指南 (Development)

## 1. 环境依赖
- Java 17+
- Node.js 20+
- Maven 3.9+
- Docker & Docker Compose

## 2. 目录结构
```text
novel-system/
├── novel-backend/          # 后端项目 (Maven)
│   ├── src/main/java/com/novel/
│   │   ├── controller/    # 接口定义
│   │   ├── model/         # 数据实体
│   │   └── repository/    # 数据存储逻辑
│   └── Dockerfile
├── novel-frontend/         # 前端项目 (Vite + Vue)
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── assets/        # 全局样式与资源
│   │   └── router/        # 路由配置
│   └── Dockerfile
└── docker-compose.yml
```

## 3. 本地开发启动

### 后端启动:
```bash
cd novel-backend
mvn spring-boot:run
```
访问 Swagger 调试: `http://localhost:8080/swagger-ui/index.html`

### 前端启动:
```bash
cd novel-frontend
npm install
npm run dev
```
访问地址: `http://localhost:3000`

## 4. 关键配置
- **前端代理**: 在 `vite.config.js` 中配置了对 `/api` 的本地转发。
- **Docker 镜像**: 后端使用 `eclipse-temurin:17-jre` 基础镜像，前端基于 `nginx:alpine`。

## 5. 发布流程
1. 提交代码至仓库。
2. 确保 `package-lock.json` 已更新。
3. 运行 `docker compose up --build` 进行全量构建。
 Riverside, CA
