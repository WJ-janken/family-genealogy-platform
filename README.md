# 家族族谱数字化管理平台

## 项目结构

```
族谱/
├── server/          # Spring Boot 后端服务
├── miniprogram/     # 微信小程序
├── admin/           # Vue3 后台管理系统
└── docs/            # 项目文档
```

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Redis |
| 小程序 | 微信小程序原生 + Canvas 2D |
| 后台管理 | Vue3 + TypeScript + Element Plus + Vite |

## 快速开始

### 后端服务

```bash
cd server
# 配置数据库连接 (application-dev.yml)
# 执行 sql/init.sql 初始化数据库
mvn spring-boot:run
```

### 后台管理系统

```bash
cd admin
npm install
npm run dev
```

### 小程序

使用微信开发者工具打开 `miniprogram` 目录。

## 开发阶段

- [x] 一期：项目骨架搭建
- [ ] 一期：数据库设计 + 后端核心 CRUD + 审核流程
- [ ] 二期：Canvas 族谱树引擎 + 小程序前端
- [ ] 三期：地方志、任职履历、批量导入导出
- [ ] 四期：用户体系完善、消息通知、性能优化
