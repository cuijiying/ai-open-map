#!/usr/bin/env python3
"""Generate the GeoMind Agent product PDF from docs/competition/02 content + images."""

from pathlib import Path

from fpdf import FPDF

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "competition" / "智图灵-GeoMind-Agent-作品介绍.pdf"
IMG = ROOT / "docs" / "competition" / "images"
FONT = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
FALLBACK = "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"


class DocPDF(FPDF):
    def __init__(self) -> None:
        super().__init__(format="A4", unit="mm")
        self.set_auto_page_break(auto=True, margin=18)
        font_path = FONT if Path(FONT).exists() else FALLBACK
        self.add_font("zh", "", font_path)
        self.add_font("zh", "B", font_path)

    def header(self) -> None:
        if self.page_no() == 1:
            return
        self.set_font("zh", "", 9)
        self.set_text_color(100, 116, 139)
        self.cell(0, 8, "智图灵 GeoMind Agent · 作品介绍", align="L")
        self.ln(4)
        self.set_draw_color(203, 213, 225)
        self.set_line_width(0.2)
        self.line(15, 14, 195, 14)
        self.ln(6)
        self.set_text_color(15, 23, 42)

    def footer(self) -> None:
        self.set_y(-14)
        self.set_font("zh", "", 8)
        self.set_text_color(148, 163, 184)
        self.cell(0, 8, f"— {self.page_no()} —", align="C")

    def cover(self) -> None:
        self.add_page()
        self.set_fill_color(15, 23, 42)
        self.rect(0, 0, 210, 297, "F")
        self.set_fill_color(14, 165, 233)
        self.rect(0, 0, 8, 297, "F")
        self.set_fill_color(34, 197, 94)
        self.rect(8, 210, 202, 3, "F")

        self.set_xy(24, 48)
        self.set_font("zh", "", 12)
        self.set_text_color(125, 211, 252)
        self.cell(0, 8, "作品介绍文档")

        self.set_xy(24, 72)
        self.set_font("zh", "B", 28)
        self.set_text_color(248, 250, 252)
        self.multi_cell(160, 14, "智图灵 GeoMind Agent")

        self.set_xy(24, 108)
        self.set_font("zh", "", 16)
        self.set_text_color(186, 230, 253)
        self.multi_cell(160, 10, "全栈开源地图智能体")

        self.set_xy(24, 132)
        self.set_font("zh", "", 11)
        self.set_text_color(148, 163, 184)
        self.multi_cell(
            165,
            7,
            "Open-Source Full-Stack Map Agent for China Provincial OSM\n"
            "自然语言驱动 · 开源地图底座 · 可私有化部署 · Agent 工具编排",
        )

        cards = [
            ("作品类型", "全栈 AI Agent 应用（开源地图 + 智能体）"),
            ("技术底座", "Spring Cloud / PostGIS / MapLibre / OpenAI 兼容大模型"),
            ("智能范式", "规则引擎 + LLM + 多工具 ReAct"),
            ("数据许可", "© OpenStreetMap contributors · ODbL"),
        ]
        y = 175
        for title, value in cards:
            self.set_xy(24, y)
            self.set_font("zh", "", 9)
            self.set_text_color(125, 211, 252)
            self.cell(40, 6, title)
            self.set_font("zh", "", 11)
            self.set_text_color(241, 245, 249)
            self.cell(0, 6, value)
            y += 10

        self.set_xy(24, 255)
        self.set_font("zh", "", 10)
        self.set_text_color(148, 163, 184)
        self.cell(0, 6, "文档版本 v1.0  |  智图灵 GeoMind Agent 作品介绍")

    def h1(self, text: str) -> None:
        self.ln(2)
        self.set_font("zh", "B", 16)
        self.set_text_color(15, 23, 42)
        self.cell(0, 10, text)
        self.ln(8)
        self.set_draw_color(14, 165, 233)
        self.set_line_width(0.8)
        y = self.get_y()
        self.line(15, y, 55, y)
        self.ln(5)

    def h2(self, text: str) -> None:
        self.ln(2)
        self.set_font("zh", "B", 12)
        self.set_text_color(30, 64, 175)
        self.cell(0, 8, text)
        self.ln(7)

    def h3(self, text: str) -> None:
        self.ln(1)
        self.set_font("zh", "B", 11)
        self.set_text_color(51, 65, 85)
        self.cell(0, 7, text)
        self.ln(6)

    def body(self, text: str) -> None:
        self.set_font("zh", "", 10)
        self.set_text_color(30, 41, 59)
        self.multi_cell(0, 6, text)
        self.ln(1)

    def bullet(self, text: str) -> None:
        self.set_font("zh", "", 10)
        self.set_text_color(30, 41, 59)
        x = self.get_x()
        self.cell(6, 6, "•")
        self.multi_cell(0, 6, text)
        self.set_x(x)
        self.ln(0.5)

    def numbered(self, idx: int, text: str) -> None:
        self.set_font("zh", "", 10)
        self.set_text_color(30, 41, 59)
        x = self.get_x()
        self.cell(8, 6, f"{idx}.")
        self.multi_cell(0, 6, text)
        self.set_x(x)
        self.ln(0.5)

    def quote(self, text: str) -> None:
        start = self.get_y()
        self.set_left_margin(20)
        self.set_x(20)
        self.set_font("zh", "", 10)
        self.set_text_color(30, 64, 175)
        self.multi_cell(170, 6, text)
        end = self.get_y()
        self.set_left_margin(15)
        self.set_fill_color(14, 165, 233)
        self.rect(15, start, 1.5, max(end - start, 6), "F")
        self.set_y(end + 2)

    def caption(self, text: str) -> None:
        self.set_font("zh", "", 9)
        self.set_text_color(100, 116, 139)
        self.multi_cell(0, 5, text, align="C")
        self.ln(2)

    def figure(self, filename: str, caption: str, max_h: float = 95) -> None:
        path = IMG / filename
        if not path.exists():
            self.body(f"[缺少插图：{filename}]")
            return
        if self.get_y() + max_h + 12 > 275:
            self.add_page()
        # fit width 180mm
        with Image.open(path) as im:  # type: ignore[name-defined]
            iw, ih = im.size
        aspect = ih / iw
        w = 180
        h = w * aspect
        if h > max_h:
            h = max_h
            w = h / aspect
        x = 15 + (180 - w) / 2
        self.image(str(path), x=x, y=self.get_y(), w=w, h=h)
        self.set_y(self.get_y() + h + 2)
        self.caption(caption)

    def _row_height(self, row: list[str], widths: list[float], line_h: float = 5.0) -> float:
        tallest = 1
        for cell, w in zip(row, widths):
            approx = max(1, int(self.get_string_width(str(cell)) / max(w - 3, 8)) + 1)
            tallest = max(tallest, min(approx, 5))
        return line_h * tallest + 2.4

    def _draw_header(self, headers: list[str], widths: list[float]) -> None:
        self.set_font("zh", "B", 9)
        self.set_fill_color(15, 23, 42)
        self.set_text_color(248, 250, 252)
        x0, y0 = self.get_x(), self.get_y()
        h = 8
        xs = x0
        for title, w in zip(headers, widths):
            self.rect(xs, y0, w, h, "F")
            self.set_xy(xs + 1.2, y0 + 1.5)
            self.cell(w - 2.4, 5, title)
            xs += w
        self.set_xy(x0, y0 + h)

    def table(self, headers: list[str], rows: list[list[str]], widths: list[float]) -> None:
        self._draw_header(headers, widths)
        fill = False
        for row in rows:
            h = self._row_height(row, widths)
            if self.get_y() + h > 275:
                self.add_page()
                self._draw_header(headers, widths)
            x0, y0 = self.get_x(), self.get_y()
            bg = (241, 245, 249) if fill else (255, 255, 255)
            self.set_text_color(30, 41, 59)
            self.set_font("zh", "", 9)
            xs = x0
            for cell, w in zip(row, widths):
                self.set_fill_color(*bg)
                self.rect(xs, y0, w, h, "F")
                self.set_xy(xs + 1.2, y0 + 1.2)
                self.multi_cell(w - 2.4, 5, str(cell))
                xs += w
            self.set_xy(x0, y0 + h)
            fill = not fill
        self.ln(3)

    def code_block(self, text: str) -> None:
        self.set_fill_color(15, 23, 42)
        self.set_text_color(186, 230, 253)
        self.set_font("zh", "", 8)
        lines = text.split("\n")
        h = 4.8 * len(lines) + 6
        y = self.get_y()
        if y + h > 280:
            self.add_page()
            y = self.get_y()
        self.rect(15, y, 180, h, "F")
        self.set_xy(18, y + 3)
        for line in lines:
            self.cell(0, 4.8, line)
            self.ln(4.8)
            self.set_x(18)
        self.set_y(y + h + 2)
        self.set_text_color(30, 41, 59)


# Late import for typing-free Image size probe
from PIL import Image  # noqa: E402


def build() -> Path:
    pdf = DocPDF()
    pdf.set_margins(15, 18, 15)
    pdf.cover()

    # 一、作品名称
    pdf.add_page()
    pdf.h1("一、作品名称")
    pdf.body("智图灵 GeoMind Agent —— 全栈开源地图智能体")
    pdf.body("命名释义：")
    pdf.bullet("智：大模型与规则引擎驱动的智能决策；")
    pdf.bullet("图：以开源地图（OSM）与矢量 WebGIS 为本体；")
    pdf.bullet("灵：具备感知地图状态、规划步骤、调用工具、反思修正的 Agent「灵性」。")

    pdf.h1("二、作品简介")
    pdf.body(
        "智图灵是一套可私有化部署的全栈开源地图智能体。它面向中国省级 OpenStreetMap 数据，"
        "打通「下载 → 解析 → 入库 → 切片 → 对话操控 → 空间分析 → 报告生成」全链路，"
        "让用户用中文自然语言完成原本需要专业 GIS 软件与大量手工操作才能完成的工作。"
    )
    pdf.body(
        "系统采用 Spring Cloud 微服务架构（网关、入库、地图、对话），以 PostgreSQL + PostGIS 存储几何要素，"
        "以 MapLibre 渲染多省矢量切片；在此之上构建 Orchestrator Agent（编排智能体），"
        "通过工具注册中心调度地图操控、空间分析、入库编排与报告叙事等子能力。"
        "规则引擎保障高频指令的确定性与低延迟，大模型负责开放表达与多步推理；"
        "本地 Ollama 与云端 OpenAI 兼容接口均可接入，数据与推理路径支持完全内网化。"
    )
    pdf.quote(
        "一句话价值：把「会说话的开源地图」变成「会办事的地图 Agent」——可见、可控、可私有化、可开源复用。"
    )

    pdf.h2("系统实机截图")
    pdf.body(
        "以下为系统真实运行界面：左侧为风格/图层或入库任务，中央为 MapLibre 地图，右侧为地图 Agent 对话面板。"
    )
    pdf.figure(
        "screenshot-dark-roads.jpg",
        "图 1  深色风格全省路网：图层勾选 + 地图对话快捷指令",
        max_h=78,
    )
    pdf.figure(
        "screenshot-city-detail.jpg",
        "图 2  城市级路网细节：要素点击弹窗 + OSM 入库日志",
        max_h=78,
    )
    pdf.figure(
        "screenshot-imagery-chat.jpg",
        "图 3  影像风格与对话操控：定位合肥、显示道路水系、切换日落风格",
        max_h=78,
    )

    # 三、应用场景
    pdf.h1("三、应用场景")
    pdf.h3("3.1 应急指挥快速研判")
    pdf.body(
        "灾害发生后，指挥人员无需熟悉图层面板与空间 SQL，直接说："
        "「定位到黄山，显示道路和水系，分析当前视野内道路里程，并标记医院。」"
        "Agent 自动完成定位、图层切换、PostGIS 相交分析与 POI 检索打点，分钟级形成初判材料。"
    )
    pdf.h3("3.2 城市规划与基建盘点")
    pdf.body(
        "规划人员可请求：「对比安徽和浙江的道路分类数量，只显示高速公路，把道路改成红色。」"
        "系统跨省聚合统计、过滤路网子类并着色强调，辅助路网密度与结构对比。"
    )
    pdf.h3("3.3 高校 GIS / OSM 教学实验")
    pdf.body(
        "教师一句话启动「下载安徽省 OSM 数据」；学生用对话完成显示图层、搜索大学、统计建筑等实验步骤。"
        "全程基于真实开源数据与可审计工具调用，适合课程实验与毕业设计底座。"
    )
    pdf.h3("3.4 政企私有化 WebGIS 底座")
    pdf.body(
        "对数据不出域有要求的单位，可在内网部署整套服务，对接本地大模型。"
        "业务地图来自自建 PostGIS，不依赖商业地图 Token；Agent 降低业务人员使用门槛。"
    )
    pdf.h3("3.5 文旅区域导览与内容生产")
    pdf.body(
        "文旅运营说：「去九华山，打开影像风格，查找附近景点。」"
        "Agent 切换底图主题、检索兴趣点并生成简短导览摘要，服务内容生产与数字化导览。"
    )

    # 四、痛点与创新
    pdf.add_page()
    pdf.h1("四、要解决的问题与创新点")
    pdf.h2("4.1 痛点")
    pdf.numbered(1, "传统 WebGIS 交互重：图层、筛选、缓冲、统计分散在多个面板，非专业用户学习成本高。")
    pdf.numbered(2, "商业地图难私有化：Token、配额与合规限制阻碍内网与开源交付。")
    pdf.numbered(3, "开源 OSM 上手难：PBF 下载、解析、投影、切片、发布链路长，缺少「对话式」封装。")
    pdf.numbered(4, "普通 ChatBot 不懂地图：只会聊天，不能真正改地图状态、算空间指标、跑数据任务。")

    pdf.h2("4.2 创新点")
    pdf.table(
        ["创新点", "说明"],
        [
            ["全栈地图 Agent", "不是外挂聊天框，而是覆盖数据生产到地图消费的闭环智能体"],
            ["规则 + LLM + 工具调用", "高频指令走规则（可测、低延迟），复杂意图走多步 ReAct"],
            ["开源可私有化", "OSM + 自建切片 + 本地模型，满足内网与开源社区双重场景"],
            ["多省同图与异步入库", "入库期间已发布省份仍可浏览；Agent 可编排跨省任务"],
            ["场景化 Skills", "应急/规划/教学等技能包可插拔，同一底座多业务复用"],
            ["结果可审计", "每次工具调用可追踪，分析结果带来源表与指标说明"],
        ],
        [48, 132],
    )

    # 五、系统架构
    pdf.h1("五、系统架构")
    pdf.h2("5.1 总体架构")
    pdf.code_block(
        "浏览器（React + MapLibre）\n"
        "        │  /api\n"
        "        ▼\n"
        "   map-gateway（Spring Cloud Gateway）\n"
        "        ├── map-ingest   下载 / 解析 / 发布 OSM\n"
        "        ├── map-geo      矢量切片 / 搜索 / 统计 / 分析\n"
        "        └── map-ai       Agent 编排 / 规则 / LLM / 工具\n"
        "                │\n"
        "                ▼\n"
        "        PostgreSQL 16 + PostGIS（ai_open_map）"
    )
    pdf.figure("architecture-overview.png", "图 4  总体系统架构：网关分流与微服务职责", max_h=100)

    pdf.h2("5.2 Agent 逻辑架构")
    pdf.body("用户自然语言进入 Orchestrator Agent：")
    pdf.numbered(1, "感知：读取地图上下文（中心、缩放、bbox、可见图层、已选省份、风格）。")
    pdf.numbered(2, "规划：分解为有序工具调用计划（可多步）。")
    pdf.numbered(3, "执行：经 Tool Registry 调用 MapOps / GeoAnalyst / IngestOps / ReportOps。")
    pdf.numbered(4, "反思：工具失败或结果为空时调整计划重试；成功则汇总回复与地图动作。")
    pdf.numbered(5, "记忆：将会话、中间结论与地图状态写入会话存储，支持指代（「再分析一下」「换浙江」）。")
    pdf.figure("agent-architecture.png", "图 5  Agent 逻辑架构：编排智能体与四大工具域", max_h=100)

    pdf.h2("5.3 技术栈")
    pdf.table(
        ["层级", "技术"],
        [
            ["前端", "React 18、TypeScript、Vite、Ant Design、MapLibre GL"],
            ["网关", "Spring Cloud Gateway、可选 Nacos"],
            ["业务服务", "Spring Boot 3.2、MyBatis、Druid"],
            ["空间库", "PostgreSQL 16、PostGIS 3.4、pg_trgm"],
            ["OSM", "Osmosis Reader、Geofabrik 省级 PBF"],
            ["智能", "规则对话引擎、OpenAI 兼容 LLM、Agent 工具编排"],
            ["运行", "JDK 17、Node.js 22、本地一键脚本"],
        ],
        [36, 144],
    )

    # 六、核心能力
    pdf.add_page()
    pdf.h1("六、核心能力详解")
    pdf.h2("6.1 开源地图数据管道")
    pdf.bullet("支持中国多省区域从 Geofabrik 下载 PBF；")
    pdf.bullet("解析 node/way/relation（面关系），分类写入九大图层；")
    pdf.bullet("分区发布：单省发布不阻塞其他省切片；")
    pdf.bullet("可选全国低缩放瓦片预生成，提升首屏与宏观浏览体验。")
    pdf.figure("data-pipeline.png", "图 6  开源地图数据管道：从 PBF 到 Agent 可用", max_h=78)

    pdf.h2("6.2 矢量地图与多省同图")
    pdf.bullet("九图层：道路、铁路、水系线、水体、建筑、土地利用、行政区划、兴趣点、地名；")
    pdf.bullet("前端可勾选多省，同一 MVT 瓦片聚合渲染；")
    pdf.bullet("支持浅色 / 深色 / 彩色 / 纸图 / 影像 / 素底等风格，以及图层着色。")

    pdf.h2("6.3 地图操控工具（MapOps）")
    pdf.table(
        ["工具", "能力"],
        [
            ["show / hide / only_layers", "显示、隐藏、独显图层，支持高速等子类过滤"],
            ["fly_to", "定位到省、市、景点等"],
            ["markers / clear_markers", "搜索结果打点与清除"],
            ["set_style / paint_layer", "底图主题与图层颜色"],
        ],
        [55, 125],
    )

    pdf.h2("6.4 空间分析工具（GeoAnalyst）")
    pdf.table(
        ["工具", "能力"],
        [
            ["layers", "各图层要素总量"],
            ["stats", "图层子类 TopN 统计"],
            ["search", "名称/类型模糊检索并定位"],
            ["analyze", "当前视野内数量、长度、面积（PostGIS）"],
            ["compare_regions", "多省指标对比表"],
        ],
        [45, 135],
    )

    pdf.h2("6.5 入库编排工具（IngestOps）")
    pdf.bullet("start_ingest：按省份创建下载解析任务；")
    pdf.bullet("watch_job：轮询进度与日志，完成后自动续执行后续地图指令；")
    pdf.bullet("支持「下载完成后再显示道路」这类跨时长计划。")

    pdf.h2("6.6 报告叙事工具（ReportOps）")
    pdf.body(
        "将分析表、关键结论与建议下一步动作汇总为结构化简报（对话内表格 + Markdown 摘要），便于汇报与存档。"
    )

    pdf.h2("6.7 规则优先与模型兜底")
    pdf.bullet("规则引擎：覆盖显示/隐藏/定位/统计/查找/分析/风格/着色等高频中文指令，离线可用、结果可单测；")
    pdf.bullet("大模型：规则未命中时，按系统提示输出受约束的工具调用计划；")
    pdf.bullet("安全边界：模型不能直接拼 SQL，只能触发白名单工具。")

    # 七、演示
    pdf.add_page()
    pdf.h1("七、典型演示流程")
    pdf.h3("演示 A：一句话看懂路网")
    pdf.numbered(1, "「定位到合肥，只显示高速公路」")
    pdf.numbered(2, "「分析当前视野内的道路里程」")
    pdf.numbered(3, "地图飞入、图层过滤、表格输出里程——展示 Agent 感知视野并调用 PostGIS。")

    pdf.h3("演示 B：跨省对比")
    pdf.numbered(1, "勾选安徽、浙江（或口述切换）")
    pdf.numbered(2, "「对比两省道路分类数量」")
    pdf.numbered(3, "输出对比表，并可「把道路改成红色」强化可视化。")

    pdf.h3("演示 C：数据生产闭环")
    pdf.numbered(1, "「下载浙江省数据，完成后显示水系和道路」")
    pdf.numbered(2, "Agent 创建入库任务、播报进度，成功后自动执行图层动作。")

    pdf.h3("演示 D：应急简报")
    pdf.numbered(1, "「定位黄山，显示道路和建筑，查找医院，生成简报」")
    pdf.numbered(2, "展示多工具串联与 ReportOps 输出。")

    # 八、开源
    pdf.h1("八、开源与合规")
    pdf.bullet("地图数据来自 OpenStreetMap，使用时保留署名，遵循 ODbL；")
    pdf.bullet("工程以可运行的全栈仓库形态组织，文档覆盖架构、库表、接口与运行方式；")
    pdf.bullet("模型密钥在接口层打码；支持无密钥本地模型，降低试用门槛；")
    pdf.bullet("鼓励在保留 OSM 署名与开源协议要求的前提下二次开发 Skills 与行业插件。")

    # 九、成果
    pdf.h1("九、成果与指标")
    pdf.table(
        ["维度", "成效"],
        [
            ["链路完整度", "从 PBF 到对话驱动地图的端到端闭环"],
            ["交互效率", "常见 GIS 操作由多步面板点击收敛为 1 句中文"],
            ["部署形态", "支持本机脚本启动与可选微服务注册发现"],
            ["智能可靠", "规则保障确定性；Agent 多步补齐复杂任务"],
            ["可扩展性", "工具注册 + Skills，可横向扩展行业场景"],
            ["数据主权", "几何与切片留在自有 PostGIS，可完全内网"],
        ],
        [36, 144],
    )

    # 十、未来
    pdf.h1("十、未来演进")
    pdf.numbered(1, "更多空间工具：缓冲区、路径邻近、等高线/DEM 叠加（在开源数据许可范围内）；")
    pdf.numbered(2, "多模态：截图/框选区域作为 Agent 输入；")
    pdf.numbered(3, "协作 Agent：研判 Agent 与制图 Agent 分工，输出标准应急一张图；")
    pdf.numbered(4, "评测集：中文地图指令基准，持续衡量工具选择准确率与任务完成率；")
    pdf.numbered(5, "社区生态：公开 Skills 模板，吸引高校与政企共建行业包。")

    # 十一、总结
    pdf.h1("十一、总结")
    pdf.body(
        "智图灵 GeoMind Agent 以开源地图为根基、以 Agent 为交互范式，"
        "把分散的 WebGIS 能力编织成可对话、可编排、可审计的智能体。"
        "它既是一个能跑起来的全栈工程，也是一个可复制的「开源地图 + AI Agent」创新范式，"
        "强调真实落地、工程完整、开源可复用。"
    )
    pdf.quote("作品口号：让每一句中文，都在地图上留下可验证的行动。")

    pdf.ln(4)
    pdf.set_font("zh", "", 9)
    pdf.set_text_color(100, 116, 139)
    pdf.multi_cell(
        0,
        5,
        "本文档内容对应 docs/competition/02-作品介绍全文.md。\n"
        "插图由 scripts/generate-competition-images.py 生成；PDF 由 scripts/generate-competition-pdf.py 生成。",
    )

    OUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUT))
    return OUT


if __name__ == "__main__":
    path = build()
    print(path)
    print(f"size={path.stat().st_size}")
