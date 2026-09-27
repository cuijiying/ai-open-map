# AI Open Map

面向安徽省 OpenStreetMap 数据的本地 WebGIS。0.1 版本可以下载并解析 Geofabrik 的安徽 PBF，写入带 PostGIS 的 PostgreSQL，按图层输出矢量切片，并用中文指令操作地图。

地图数据来自 OpenStreetMap，使用时需保留署名：[© OpenStreetMap contributors](https://www.openstreetmap.org/copyright)，许可为 ODbL。

## 文档

| 文档 | 内容 |
| --- | --- |
| [构建过程](docs/00-构建过程.md) | 按真实顺序记录怎么搭起来、踩了哪些坑、用什么数据验证 |
| [架构与技术栈](docs/01-架构与技术栈.md) | 五个进程怎么分工，为什么选这些库 |
| [数据库设计](docs/02-数据库设计.md) | 表、索引、入库缓冲和坐标系 |
| [运行与接口](docs/03-运行与接口.md) | 启动、端口、对话指令和 HTTP 接口 |

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
