# AI Open Map

面向中国省级 OpenStreetMap 数据的本地 WebGIS。可以下载并解析 Geofabrik 的省级 PBF，写入带 PostGIS 的 PostgreSQL，按图层输出矢量切片。地图上可以同时显示多个省。下载、解析和建索引期间，已经发布的省份切片仍可访问。也可以用中文指令操作地图。

地图数据来自 OpenStreetMap，使用时需保留署名：[© OpenStreetMap contributors](https://www.openstreetmap.org/copyright)，许可为 ODbL。

## 文档

| 文档 | 内容 |
| --- | --- |
| [构建过程](docs/00-构建过程.md) | 按真实顺序记录怎么搭起来、踩了哪些坑、用什么数据验证 |
| [架构与技术栈](docs/01-架构与技术栈.md) | 五个进程怎么分工，为什么选这些库 |
| [数据库设计](docs/02-数据库设计.md) | 表、索引、入库缓冲和坐标系 |
| [运行与接口](docs/03-运行与接口.md) | 启动、端口、对话指令和 HTTP 接口 |
| [Nacos 安装与使用](docs/06-Nacos安装与使用.md) | 注册中心安装、cloud 模式联调 |
| [多省切片学习笔记](docs/05-多省切片学习笔记.md) | 2026-10-21 的实现：入库时切片为何还能用，多个省如何画在同一张地图上 |
| [作品定位与规划](docs/competition/01-作品定位与规划.md) | AI 创新大赛：智图灵 GeoMind Agent 定位与 Agent 增强规划 |
| [作品介绍全文](docs/competition/02-作品介绍全文.md) | 大赛报送用作品介绍文稿（按已实现口径） |
| [作品介绍 PDF](docs/competition/智图灵-GeoMind-Agent-作品介绍.pdf) | 详细 PDF 作品介绍文档 |

## 本地启动

环境：JDK 17、Maven、Node.js 22、PostgreSQL 16 + PostGIS 3.4。数据库用户 `postgres`，密码 `post`，库名 `ai_open_map`。

```powershell
.\scripts\init-db.ps1
mvn -DskipTests package
.\scripts\start-all.ps1
```

浏览器打开 http://127.0.0.1:5173 。接口统一走网关 http://127.0.0.1:8080 。停止服务：

```powershell
.\scripts\stop-all.ps1
```

Druid 监控台：http://127.0.0.1:8081/druid/ 、`:8082/druid/` 、`:8083/druid/` ，默认账号 `admin` / `admin`。
