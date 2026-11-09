#!/usr/bin/env python3
"""Generate the GeoMind Agent competition PDF from structured content."""

from pathlib import Path

from fpdf import FPDF

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "competition" / "智图灵-GeoMind-Agent-作品介绍.pdf"
FONT = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
FALLBACK = "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"


class DocPDF(FPDF):
    def __init__(self) -> None:
        super().__init__(format="A4", unit="mm")
        self.set_auto_page_break(auto=True, margin=18)
        font_path = FONT if Path(FONT).exists() else FALLBACK
        self.add_font("zh", "", font_path)
        self.add_font("zh", "B", font_path)
        self._in_header = False

    def header(self) -> None:
        if self.page_no() == 1:
            return
        self._in_header = True
        self.set_font("zh", "", 9)
        self.set_text_color(100, 116, 139)
        self.cell(0, 8, "智图灵 GeoMind Agent · 公司 AI 创新大赛作品介绍", align="L")
        self.ln(4)
        self.set_draw_color(203, 213, 225)
        self.set_line_width(0.2)
        self.line(15, 14, 195, 14)
        self.ln(6)
        self.set_text_color(15, 23, 42)
        self._in_header = False

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
        self.cell(0, 8, "公司 AI 创新大赛 · 参赛作品介绍文档")

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
            ("作品类型", "全栈 AI Agent 应用"),
            ("技术底座", "Spring Cloud / PostGIS / MapLibre"),
            ("智能范式", "规则引擎 + LLM + 多工具 ReAct"),
            ("数据许可", "© OpenStreetMap · ODbL"),
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
        self.cell(0, 6, "文档版本 v1.0  |  按已实现口径撰写，便于大赛报送与路演")

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

    def quote(self, text: str) -> None:
        self.set_fill_color(240, 249, 255)
        self.set_draw_color(125, 211, 252)
        self.set_line_width(0.4)
        x, y = 15, self.get_y()
        self.set_x(x)
        start = self.get_y()
        self.set_font("zh", "", 10)
        self.set_text_color(30, 64, 175)
        self.set_left_margin(20)
        self.multi_cell(170, 6, text)
        self.set_left_margin(15)
        end = self.get_y()
        self.set_xy(x, start)
        self.set_fill_color(14, 165, 233)
        self.rect(15, start, 1.5, end - start, "F")
        self.set_y(end + 2)

    def _row_height(self, row: list[str], widths: list[float], line_h: float = 5.0) -> float:
        tallest = 1
        for cell, w in zip(row, widths):
            # approximate wrapped line count for CJK/latin mix
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
            self.set_fill_color(241, 245, 249) if fill else self.set_fill_color(255, 255, 255)
            self.set_text_color(30, 41, 59)
            self.set_font("zh", "", 9)
            xs = x0
            bg = (241, 245, 249) if fill else (255, 255, 255)
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
        x = self.get_x()
        y = self.get_y()
        # estimate height
        lines = text.split("\n")
        h = 4.8 * len(lines) + 6
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


def build() -> Path:
    pdf = DocPDF()
    pdf.set_margins(15, 18, 15)
    pdf.cover()

    # 作品基本信息
    pdf.add_page()
    pdf.h1("一、作品基本信息")
    pdf.table(
        ["项目", "内容"],
        [
            ["作品名称", "智图灵 GeoMind Agent —— 全栈开源地图智能体"],
            ["英文名称", "GeoMind Agent: Open-Source Full-Stack Map Agent"],
            ["作品类型", "全栈 AI Agent 应用（开源地图 + 智能体）"],
            ["核心定位", "用自然语言驱动 OSM 入库、地图操控、空间分析与报告生成"],
            ["技术底座", "Spring Cloud / PostGIS / MapLibre / OpenAI 兼容大模型"],
            ["数据来源", "OpenStreetMap 中国省级数据（Geofabrik PBF）"],
            ["部署形态", "可本机一键启动，支持可选 Nacos；可完全私有化"],
            ["开源合规", "保留 © OpenStreetMap contributors 署名，遵循 ODbL"],
        ],
        [42, 138],
    )

    pdf.h2("报送用：作品简介")
    pdf.body(
        "智图灵是面向企业内网与开源社区的全栈地图智能体。系统基于 Spring Cloud 微服务、"
        "PostGIS 与 MapLibre，完整覆盖省级 OSM 数据下载解析、多省矢量切片发布与中文对话操控。"
        "在规则引擎与大模型之上，构建了「规划—工具调用—执行—反思」的 Agent 闭环：可自动编排入库任务、"
        "切换图层与风格、检索要素、视野分析、跨省对比，并输出结构化地图报告。"
        "支持 OpenAI 兼容本地/云端模型，数据与推理均可私有化，适合应急、规划、教育与企业 WebGIS 场景。"
    )
    pdf.quote(
        "一句话价值：把「会说话的开源地图」变成「会办事的地图 Agent」——可见、可控、可私有化、可开源复用。"
    )

    # 应用场景
    pdf.h1("二、应用场景")
    scenarios = [
        (
            "2.1 应急指挥快速研判",
            "灾害发生后，指挥人员无需熟悉图层面板与空间 SQL，直接说："
            "「定位到黄山，显示道路和水系，分析当前视野内道路里程，并标记医院。」"
            "Agent 自动完成定位、图层切换、PostGIS 相交分析与 POI 检索打点，分钟级形成初判材料。",
        ),
        (
            "2.2 城市规划与基建盘点",
            "规划人员可请求：「对比安徽和浙江的道路分类数量，只显示高速公路，把道路改成红色。」"
            "系统跨省聚合统计、过滤路网子类并着色强调，辅助路网密度与结构对比。",
        ),
        (
            "2.3 高校 GIS / OSM 教学实验",
            "教师一句话启动「下载安徽省 OSM 数据」；学生用对话完成显示图层、搜索大学、统计建筑等实验步骤。"
            "全程基于真实开源数据与可审计工具调用，适合课程实验与毕业设计底座。",
        ),
        (
            "2.4 政企私有化 WebGIS 底座",
            "对数据不出域有要求的单位，可在内网部署整套服务，对接本地大模型。"
            "业务地图来自自建 PostGIS，不依赖商业地图 Token；Agent 降低业务人员使用门槛。",
        ),
        (
            "2.5 文旅区域导览与内容生产",
            "文旅运营说：「去九华山，打开影像风格，查找附近景点。」"
            "Agent 切换底图主题、检索兴趣点并生成简短导览摘要，服务内容生产与数字化导览。",
        ),
    ]
    for title, text in scenarios:
        pdf.h3(title)
        pdf.body(text)

    # 痛点与创新
    pdf.add_page()
    pdf.h1("三、问题洞察与创新点")
    pdf.h2("3.1 痛点")
    for item in [
        "传统 WebGIS 交互重：图层、筛选、统计分散在多个面板，非专业用户学习成本高。",
        "商业地图难私有化：Token、配额与合规限制阻碍内网与开源交付。",
        "开源 OSM 上手难：PBF 下载、解析、切片、发布链路长，缺少对话式封装。",
        "普通 ChatBot 不懂地图：只会聊天，不能真正改地图状态、算空间指标、跑数据任务。",
    ]:
        pdf.bullet(item)

    pdf.h2("3.2 创新点")
    pdf.table(
        ["创新点", "说明"],
        [
            ["全栈地图 Agent", "覆盖数据生产到地图消费的闭环，而非外挂聊天框"],
            ["规则 + LLM + 工具调用", "高频指令走规则（可测、低延迟），复杂意图走多步 ReAct"],
            ["开源可私有化", "OSM + 自建切片 + 本地模型，服务内网与开源社区"],
            ["多省同图与异步入库", "入库期间已发布省份仍可浏览；Agent 可编排跨省任务"],
            ["场景化 Skills", "应急/规划/教学等技能包可插拔，同一底座多业务复用"],
            ["结果可审计", "工具调用可追踪，分析结果带来源说明与指标口径"],
        ],
        [48, 132],
    )

    # 架构
    pdf.h1("四、系统架构")
    pdf.h2("4.1 总体架构")
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
    pdf.h2("4.2 Agent 逻辑架构")
    pdf.body(
        "用户自然语言进入 Orchestrator Agent（编排智能体），完成感知 → 规划 → 执行 → 反思 → 记忆："
    )
    for item in [
        "感知：读取地图上下文（中心、缩放、bbox、可见图层、已选省份、风格）。",
        "规划：将复杂指令分解为有序工具调用计划（可多步）。",
        "执行：经 Tool Registry 调用 MapOps / GeoAnalyst / IngestOps / ReportOps。",
        "反思：工具失败或结果为空时调整计划重试；成功则汇总回复与地图动作。",
        "记忆：会话、中间结论与地图状态持久化，支持「再分析一下」「换浙江」等指代。",
    ]:
        pdf.bullet(item)

    pdf.h2("4.3 技术栈")
    pdf.table(
        ["层级", "技术选型"],
        [
            ["前端", "React 18、TypeScript、Vite、Ant Design、MapLibre GL"],
            ["网关与微服务", "Spring Cloud Gateway、Spring Boot 3.2、可选 Nacos"],
            ["数据访问", "MyBatis、Alibaba Druid"],
            ["空间数据库", "PostgreSQL 16、PostGIS 3.4、pg_trgm"],
            ["OSM 管道", "Osmosis Reader、Geofabrik 省级 PBF"],
            ["智能层", "规则对话引擎、OpenAI 兼容 LLM、Agent 工具编排"],
            ["运行环境", "JDK 17、Node.js 22、本地启动脚本"],
        ],
        [36, 144],
    )

    # 核心能力
    pdf.add_page()
    pdf.h1("五、核心能力详解")
    pdf.h2("5.1 开源地图数据管道")
    for item in [
        "支持中国多省区域从 Geofabrik 下载 PBF；",
        "解析 node / way / relation（面关系），分类写入九大图层；",
        "分区发布：单省发布不阻塞其他省切片查询；",
        "可选全国低缩放瓦片预生成，提升首屏与宏观浏览体验。",
    ]:
        pdf.bullet(item)

    pdf.h2("5.2 矢量地图与多省同图")
    pdf.body(
        "九图层覆盖道路、铁路、水系线、水体、建筑、土地利用、行政区划、兴趣点、地名。"
        "前端可勾选多省，同一 MVT 瓦片聚合渲染；并支持浅色 / 深色 / 彩色 / 纸图 / 影像 / 素底等风格及图层着色。"
    )

    pdf.h2("5.3 地图操控工具 MapOps")
    pdf.table(
        ["工具", "能力"],
        [
            ["show / hide / only_layers", "显示、隐藏、独显图层，支持高速等子类过滤"],
            ["fly_to", "定位到省、市、景点等目标"],
            ["markers / clear_markers", "搜索结果打点与清除"],
            ["set_style / paint_layer", "底图主题切换与图层颜色调整"],
        ],
        [55, 125],
    )

    pdf.h2("5.4 空间分析工具 GeoAnalyst")
    pdf.table(
        ["工具", "能力"],
        [
            ["layers", "各图层要素总量统计"],
            ["stats", "图层子类 TopN 统计"],
            ["search", "名称/类型模糊检索并定位"],
            ["analyze", "当前视野内数量、长度、面积（PostGIS）"],
            ["compare_regions", "多省指标对比表"],
        ],
        [45, 135],
    )

    pdf.h2("5.5 入库编排与报告叙事")
    pdf.body(
        "IngestOps 支持按省创建下载解析任务、轮询进度日志，并在完成后自动续执行后续地图指令，"
        "实现「下载浙江并在完成后只显示道路」这类跨时长计划。"
        "ReportOps 将分析表、关键结论与建议下一步汇总为结构化简报，便于汇报与存档。"
    )

    pdf.h2("5.6 规则优先与模型兜底")
    for item in [
        "规则引擎覆盖显示/隐藏/定位/统计/查找/分析/风格/着色等高频中文指令，离线可用、结果可单测；",
        "大模型在规则未命中时输出受约束的工具调用计划，负责开放表达与多步推理；",
        "安全边界：模型不能直接拼 SQL，只能触发白名单工具，保证可控与可审计。",
    ]:
        pdf.bullet(item)

    # 演示
    pdf.add_page()
    pdf.h1("六、典型演示流程（路演）")
    demos = [
        (
            "演示 A：一句话看懂路网",
            [
                "「定位到合肥，只显示高速公路」",
                "「分析当前视野内的道路里程」",
                "地图飞入、图层过滤、表格输出里程——展示 Agent 感知视野并调用 PostGIS。",
            ],
        ),
        (
            "演示 B：跨省对比",
            [
                "勾选安徽、浙江（或口述切换）",
                "「对比两省道路分类数量」",
                "输出对比表，并可「把道路改成红色」强化可视化。",
            ],
        ),
        (
            "演示 C：数据生产闭环",
            [
                "「下载浙江省数据，完成后显示水系和道路」",
                "Agent 创建入库任务、播报进度，成功后自动执行图层动作。",
            ],
        ),
        (
            "演示 D：应急简报",
            [
                "「定位黄山，显示道路和建筑，查找医院，生成简报」",
                "展示多工具串联与 ReportOps 输出。",
            ],
        ),
    ]
    for title, steps in demos:
        pdf.h3(title)
        for step in steps:
            pdf.bullet(step)

    pdf.h2("3 分钟路演话术提纲")
    for item in [
        "痛点（20s）：传统 WebGIS 门槛高；商业地图难私有化；开源 OSM 上手重。",
        "方案（40s）：智图灵 = 开源地图底座 + 地图 Agent；自然语言驱动全链路。",
        "现场（90s）：下载省份 → 显示高速 → 分析视野里程 → 跨省对比 → 生成简报。",
        "价值（30s）：可私有化、可开源复用、可扩展 Skills，服务创新与业务落地。",
    ]:
        pdf.bullet(item)

    # 成效与开源
    pdf.h1("七、开源合规与作品成效")
    pdf.h2("7.1 开源与合规")
    for item in [
        "地图数据来自 OpenStreetMap，使用时保留署名，遵循 ODbL；",
        "工程以可运行的全栈仓库形态组织，文档覆盖架构、库表、接口与运行方式；",
        "模型密钥在接口层打码；支持无密钥本地模型，降低试用门槛；",
        "鼓励在协议允许范围内二次开发 Skills 与行业插件。",
    ]:
        pdf.bullet(item)

    pdf.h2("7.2 成效指标")
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

    # 未来与总结
    pdf.add_page()
    pdf.h1("八、未来演进")
    for item in [
        "更多空间工具：缓冲区、路径邻近、开源地形叠加等（在许可范围内）；",
        "多模态输入：截图 / 框选区域作为 Agent 输入；",
        "协作 Agent：研判 Agent 与制图 Agent 分工，输出标准应急一张图；",
        "评测集：中文地图指令基准，持续衡量工具选择准确率与任务完成率；",
        "社区生态：公开 Skills 模板，吸引高校与政企共建行业包。",
    ]:
        pdf.bullet(item)

    pdf.h1("九、总结")
    pdf.body(
        "智图灵 GeoMind Agent 以开源地图为根基、以 Agent 为交互范式，"
        "把分散的 WebGIS 能力编织成可对话、可编排、可审计的智能体。"
        "它既是一个能跑起来的全栈工程，也是一个可复制的「开源地图 + AI Agent」创新范式——"
        "适合作为公司 AI 创新大赛中强调真实落地、工程完整、开源可复用的参赛作品。"
    )
    pdf.quote(
        "作品口号：让每一句中文，都在地图上留下可验证的行动。"
    )

    pdf.h1("附录：能力对照速查")
    pdf.table(
        ["编号", "能力", "用户可感知效果"],
        [
            ["A1", "多步 ReAct 规划", "复杂一句话自动拆成多步并执行"],
            ["A2", "工具注册中心", "地图/分析/入库/报告能力统一可扩展"],
            ["A3", "地图状态记忆", "支持指代与连续追问"],
            ["A4", "入库 Agent", "下载解析可编排进对话计划"],
            ["A5", "跨省对比分析", "多省指标并排呈现"],
            ["A6", "空间问答增强", "检索+分析+着色组合完成"],
            ["A7", "地图报告生成", "对话内输出结构化简报"],
            ["A8", "Skills 插件", "应急/规划/教学场景一键切换"],
            ["A9", "本地/云端双模", "内网可用本地模型"],
            ["A10", "可观测与安全", "工具白名单与调用审计"],
        ],
        [18, 42, 120],
    )

    pdf.ln(6)
    pdf.set_font("zh", "", 9)
    pdf.set_text_color(100, 116, 139)
    pdf.multi_cell(
        0,
        5,
        "本 PDF 由 scripts/generate-competition-pdf.py 生成。\n"
        "同期材料：docs/competition/01-作品定位与规划.md 、02-作品介绍全文.md",
    )

    OUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUT))
    return OUT


if __name__ == "__main__":
    path = build()
    print(path)
    print(f"size={path.stat().st_size}")
