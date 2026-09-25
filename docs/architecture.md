# 架构说明文档 (Architecture)

## 1. 系统概览
本项目是一个前后端分离的小说阅读系统，旨在提供极致的视觉体验和流畅的阅读过程。

## 2. 技术栈
- **前端 (Frontend)**: 
  - 核心框架: Vue 3 (Composition API)
  - UI 组件库: Element Plus
  - 构建工具: Vite
  - 路由管理: Vue Router
- **后端 (Backend)**:
  - 核心框架: Spring Boot 3.2.1
  - 接口文档: SpringDoc OpenAPI (SwaggerUI)
  - 运行环境: Java 17
- **基础设施**:
  - 容器化: Docker & Docker Compose
  - Web 服务器: Nginx (用于前端部署及 API 代理)

## 3. 逻辑架构
- **表现层**: 基于 Vue 3 的单页应用 (SPA)，通过 Axios 与后端进行 RESTful 通信。
- **业务逻辑层**: Spring Boot 控制器处理业务逻辑。
- **数据访问层**: 内存型数据仓库 (In-memory Repository)，通过 `ConcurrentHashMap` 模拟数据库交互。

## 4. 部署架构
系统通过 Docker Compose 进行全量容器化部署：
1. **novel-frontend 容器**: 运行 Nginx，提供前端静态资源服务，并将 `/api` 请求转发至后端。
2. **novel-backend 容器**: 运行 Spring Boot 应用，监听 8080 端口。

## 5. 网络拓扑
- 前端访问地址: `http://localhost:3000`
- 后端访问地址: `http://localhost:8080` (内部通信通过 Docker 网络 `novel-net`)
