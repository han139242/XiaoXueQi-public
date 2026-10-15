# 羽毛球 MES 课程项目

面向羽毛球生产流程的制造执行系统，覆盖生产计划、工单、质量、仓储和权限管理，并包含PC与小程序相关代码。

## 源码入口

- 后端：`workspace/idea/ems`，Spring Boot与MySQL。
- PC前端：`workspace/vscode/ems-forword`，Vue。
- 开发约束：`workspace/idea/ems/docs/羽毛球MES开发约束框架.md`。

## 本地使用

在自己的环境配置数据库及服务凭据，参照后端`pom.xml`和前端`package.json`安装项目所需依赖。AI服务等外部凭据通过环境变量提供。私有共享数据库、课程材料、身份信息和预编译程序保留在私有原件中。

---

## 公开版本说明

本项目用于羽毛球生产制造场景的MES开发实践，后端采用Spring Boot，PC端采用Vue，数据存储采用MySQL。公开内容保留源码，运行凭据通过本地环境变量配置。

运行前配置本机依赖及环境变量，不将真实配置提交到Git。
