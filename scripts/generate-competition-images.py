#!/usr/bin/env python3
"""Generate system diagrams for the GeoMind Agent product PDF."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / "docs" / "competition" / "images"
FONT = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"


def font(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT, size)


def rounded(draw: ImageDraw.ImageDraw, box, fill, outline=None, radius=16, width=2):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def center_text(draw, box, text, fnt, fill):
    x0, y0, x1, y1 = box
    bbox = draw.textbbox((0, 0), text, font=fnt)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(((x0 + x1 - tw) / 2, (y0 + y1 - th) / 2), text, font=fnt, fill=fill)


def multiline_center(draw, cx, cy, lines, fnt, fill, gap=4):
    heights = []
    widths = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=fnt)
        widths.append(bbox[2] - bbox[0])
        heights.append(bbox[3] - bbox[1])
    total_h = sum(heights) + gap * (len(lines) - 1)
    y = cy - total_h / 2
    for i, line in enumerate(lines):
        draw.text((cx - widths[i] / 2, y), line, font=fnt, fill=fill)
        y += heights[i] + gap


def arrow_down(draw, x, y0, y1, color=(14, 165, 233)):
    draw.line((x, y0, x, y1 - 8), fill=color, width=3)
    draw.polygon([(x - 7, y1 - 10), (x + 7, y1 - 10), (x, y1)], fill=color)


def arrow_right(draw, x0, y, x1, color=(14, 165, 233)):
    draw.line((x0, y, x1 - 8, y), fill=color, width=3)
    draw.polygon([(x1 - 10, y - 7), (x1 - 10, y + 7), (x1, y)], fill=color)


def make_architecture() -> Path:
    w, h = 1600, 1100
    img = Image.new("RGB", (w, h), (248, 250, 252))
    draw = ImageDraw.Draw(img)

    # header band
    draw.rectangle((0, 0, w, 90), fill=(15, 23, 42))
    draw.text((48, 28), "智图灵 GeoMind · 总体系统架构", font=font(36), fill=(248, 250, 252))
    draw.text((48, 68), "浏览器 → 网关 → 微服务 → PostGIS", font=font(18), fill=(148, 163, 184))

    # browser
    box = (520, 130, 1080, 230)
    rounded(draw, box, (224, 242, 254), (14, 165, 233), 18)
    multiline_center(draw, 800, 180, ["浏览器前端", "React + TypeScript + MapLibre GL"], font(26), (12, 74, 110))

    arrow_down(draw, 800, 230, 280)

    # gateway
    box = (520, 280, 1080, 370)
    rounded(draw, box, (15, 23, 42), (56, 189, 248), 18)
    multiline_center(draw, 800, 325, ["map-gateway :8080", "Spring Cloud Gateway · 路由 / CORS"], font(24), (241, 245, 249))

    arrow_down(draw, 800, 370, 430)

    # three services
    services = [
        ((80, 440, 480, 620), (219, 234, 254), (37, 99, 235), ["map-ingest :8081", "下载 / 解析 / 发布 OSM", "读写 PostGIS"]),
        ((560, 440, 1040, 620), (220, 252, 231), (22, 163, 74), ["map-geo :8082", "矢量切片 / 搜索 / 统计 / 分析", "只读 PostGIS"]),
        ((1120, 440, 1520, 620), (254, 243, 199), (217, 119, 6), ["map-ai :8083", "Agent 编排 / 规则 / LLM / 工具", "读写模型配置"]),
    ]
    for box, fill, outline, lines in services:
        rounded(draw, box, fill, outline, 18)
        cx = (box[0] + box[2]) / 2
        cy = (box[1] + box[3]) / 2
        multiline_center(draw, cx, cy, lines, font(22), (15, 23, 42), gap=8)

    # arrows from gateway to services (visual already via common parent)
    draw.line((800, 430, 280, 430), fill=(14, 165, 233), width=3)
    draw.line((800, 430, 1320, 430), fill=(14, 165, 233), width=3)
    arrow_down(draw, 280, 430, 440)
    arrow_down(draw, 800, 430, 440)
    arrow_down(draw, 1320, 430, 440)

    # ai calls geo (side route below service cards)
    draw.line((1120, 560, 1040, 560), fill=(217, 119, 6), width=3)
    draw.polygon([(1048, 553), (1048, 567), (1035, 560)], fill=(217, 119, 6))
    draw.text((1045, 568), "Agent → 地图 API", font=font(16), fill=(146, 64, 14))

    # database
    box = (420, 760, 1180, 920)
    rounded(draw, box, (15, 23, 42), (52, 211, 153), 22)
    multiline_center(
        draw,
        800,
        840,
        ["PostgreSQL 16 + PostGIS 3.4", "库 ai_open_map · 分区要素表 · 入库任务 · 模型配置"],
        font(26),
        (236, 253, 245),
        gap=10,
    )
    # service → db connectors
    for x, color in ((280, (37, 99, 235)), (800, (22, 163, 74)), (1320, (217, 119, 6))):
        draw.line((x, 620, x, 700), fill=color, width=3)
        draw.line((x, 700, 800, 700), fill=color, width=3)
    arrow_down(draw, 800, 700, 760)

    draw.text((48, 1000), "说明：前端只与网关通信；切片与查询由 map-geo 提供；对话 Agent 通过 HTTP 调用地图服务。", font=font(20), fill=(100, 116, 139))

    path = OUT / "architecture-overview.png"
    img.save(path, "PNG")
    return path


def make_agent() -> Path:
    w, h = 1600, 1050
    img = Image.new("RGB", (w, h), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, w, 90), fill=(30, 64, 175))
    draw.text((48, 28), "智图灵 GeoMind · Agent 逻辑架构", font=font(36), fill=(248, 250, 252))
    draw.text((48, 68), "感知 → 规划 → 执行 → 反思 → 记忆", font=font(18), fill=(191, 219, 254))

    # user
    box = (600, 120, 1000, 200)
    rounded(draw, box, (241, 245, 249), (100, 116, 139), 16)
    center_text(draw, box, "用户自然语言 + 地图上下文", font(24), (15, 23, 42))
    arrow_down(draw, 800, 200, 250)

    # orchestrator
    box = (420, 250, 1180, 390)
    rounded(draw, box, (15, 23, 42), (56, 189, 248), 20)
    multiline_center(
        draw,
        800,
        320,
        ["Orchestrator Agent（编排智能体）", "意图分解 · 计划生成 · 工具选择 · 反思重试"],
        font(26),
        (241, 245, 249),
        gap=10,
    )
    arrow_down(draw, 800, 390, 450)

    tools = [
        ((60, 460, 400, 680), (219, 234, 254), (37, 99, 235), ["MapOps", "图层 / 定位 / 标记", "风格 / 着色"]),
        ((420, 460, 760, 680), (220, 252, 231), (22, 163, 74), ["GeoAnalyst", "统计 / 搜索 / 分析", "跨省对比"]),
        ((840, 460, 1180, 680), (254, 243, 199), (217, 119, 6), ["IngestOps", "下载入库编排", "任务轮询续聊"]),
        ((1200, 460, 1540, 680), (237, 233, 254), (124, 58, 237), ["ReportOps", "结构化简报", "结论 + 下一步"]),
    ]
    for box, fill, outline, lines in tools:
        rounded(draw, box, fill, outline, 18)
        cx = (box[0] + box[2]) / 2
        cy = (box[1] + box[3]) / 2
        multiline_center(draw, cx, cy, lines, font(22), (15, 23, 42), gap=8)

    # fan-out lines
    for x in (230, 590, 1010, 1370):
        draw.line((800, 450, x, 450), fill=(14, 165, 233), width=3)
        arrow_down(draw, x, 450, 460)

    # foundation
    box = (200, 760, 1400, 920)
    rounded(draw, box, (248, 250, 252), (148, 163, 184), 18)
    multiline_center(
        draw,
        800,
        840,
        ["能力底座", "规则引擎优先  ·  OpenAI 兼容 LLM 兜底  ·  Tool Registry 白名单  ·  PostGIS / 切片 / 任务表"],
        font(24),
        (51, 65, 85),
        gap=12,
    )
    for x in (230, 590, 1010, 1370):
        arrow_down(draw, x, 680, 760)

    draw.text((48, 970), "规则覆盖高频中文指令；模型仅能触发白名单工具，不可直接拼 SQL。", font=font(20), fill=(100, 116, 139))

    path = OUT / "agent-architecture.png"
    img.save(path, "PNG")
    return path


def make_pipeline() -> Path:
    w, h = 1600, 780
    img = Image.new("RGB", (w, h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, w, 90), fill=(6, 95, 70))
    draw.text((48, 28), "智图灵 GeoMind · 开源地图数据管道", font=font(36), fill=(236, 253, 245))
    draw.text((48, 68), "下载 → 解析 → 缓冲 → 发布 → 切片 / 对话可用", font=font(18), fill=(167, 243, 208))

    steps = [
        (40, "Geofabrik\n省级 PBF", (219, 234, 254), (37, 99, 235)),
        (300, "Osmosis\n解析分类", (254, 243, 199), (217, 119, 6)),
        (560, "stage 缓冲表\nUNLOGGED", (254, 226, 226), (220, 38, 38)),
        (820, "分区发布\nosm_feature", (220, 252, 231), (22, 163, 74)),
        (1080, "矢量切片\nMVT / 预生成", (224, 242, 254), (2, 132, 199)),
        (1340, "Agent\n对话可用", (237, 233, 254), (124, 58, 237)),
    ]
    y0, y1 = 220, 420
    for x, label, fill, outline in steps:
        box = (x, y0, x + 220, y1)
        rounded(draw, box, fill, outline, 18)
        multiline_center(draw, x + 110, (y0 + y1) / 2, label.split("\n"), font(24), (15, 23, 42), gap=10)
    for i in range(5):
        x0 = steps[i][0] + 220
        x1 = steps[i + 1][0]
        arrow_right(draw, x0 + 4, 320, x1 - 4)

    # notes
    notes = [
        (80, 500, 520, 680, "九大图层", "道路 / 铁路 / 水系 / 水体\n建筑 / 用地 / 区划 / POI / 地名"),
        (560, 500, 1040, 680, "多省同图", "多省勾选同一瓦片聚合渲染\n单省发布不阻塞其他省浏览"),
        (1080, 500, 1520, 680, "可私有化", "几何与切片留在自有 PostGIS\n支持本地大模型内网推理"),
    ]
    for box in notes:
        x0, y0, x1, y1, title, body = box
        rounded(draw, (x0, y0, x1, y1), (255, 255, 255), (203, 213, 225), 16)
        draw.text((x0 + 24, y0 + 24), title, font=font(26), fill=(15, 23, 42))
        draw.multiline_text((x0 + 24, y0 + 70), body, font=font(20), fill=(71, 85, 105), spacing=8)

    path = OUT / "data-pipeline.png"
    img.save(path, "PNG")
    return path


def make_ui_mock() -> Path:
    w, h = 1600, 960
    img = Image.new("RGB", (w, h), (226, 232, 240))
    draw = ImageDraw.Draw(img)

    # window chrome
    rounded(draw, (40, 40, 1560, 920), (248, 250, 252), (148, 163, 184), 18)
    draw.rectangle((40, 40, 1560, 100), fill=(15, 23, 42))
    draw.text((64, 56), "智图灵 GeoMind Agent", font=font(26), fill=(248, 250, 252))
    draw.text((420, 62), "全栈开源地图智能体", font=font(18), fill=(148, 163, 184))
    draw.ellipse((1480, 58, 1500, 78), fill=(248, 113, 113))
    draw.ellipse((1510, 58, 1530, 78), fill=(250, 204, 21))
    draw.ellipse((1540, 58, 1560, 78), fill=(74, 222, 128))

    # left sider - jobs/layers
    draw.rectangle((40, 100, 360, 920), fill=(241, 245, 249))
    draw.text((64, 120), "省份与任务", font=font(22), fill=(15, 23, 42))
    for i, (name, status) in enumerate([("安徽 anhui", "已发布"), ("浙江 zhejiang", "可下载"), ("江苏 jiangsu", "可下载")]):
        y = 170 + i * 70
        rounded(draw, (60, y, 340, y + 56), (255, 255, 255), (203, 213, 225), 10)
        draw.text((78, y + 10), name, font=font(18), fill=(15, 23, 42))
        draw.text((78, y + 32), status, font=font(14), fill=(22, 163, 74) if "发布" in status else (100, 116, 139))

    draw.text((64, 400), "图层", font=font(22), fill=(15, 23, 42))
    layers = [("道路 highway", True), ("水系 water", True), ("建筑 building", False), ("铁路 railway", False), ("兴趣点 poi", True)]
    for i, (name, on) in enumerate(layers):
        y = 440 + i * 42
        draw.rounded_rectangle((64, y + 6, 86, y + 28), radius=4, fill=(34, 197, 94) if on else (203, 213, 225))
        draw.text((100, y + 8), name, font=font(18), fill=(30, 41, 59))

    # map area
    draw.rectangle((360, 100, 1120, 920), fill=(186, 230, 253))
    # fake terrain blocks
    for pts, color in [
        ([(420, 220), (700, 180), (860, 320), (620, 420), (400, 340)], (134, 239, 172)),
        ([(760, 360), (980, 300), (1060, 480), (840, 560), (720, 470)], (147, 197, 253)),
        ([(430, 520), (680, 500), (740, 700), (480, 760)], (167, 243, 208)),
    ]:
        draw.polygon(pts, fill=color)
    # roads
    draw.line((480, 240, 900, 520), fill=(249, 115, 22), width=5)
    draw.line((560, 180, 560, 780), fill=(234, 179, 8), width=4)
    draw.line((400, 600, 1000, 420), fill=(239, 68, 68), width=4)
    # marker
    draw.ellipse((730, 390, 760, 420), fill=(220, 38, 38))
    draw.text((680, 430), "合肥", font=font(20), fill=(127, 29, 29))
    draw.text((380, 860), "MapLibre · 多省矢量切片 · © OpenStreetMap", font=font(16), fill=(51, 65, 85))

    # chat panel
    draw.rectangle((1120, 100, 1560, 920), fill=(255, 255, 255))
    draw.text((1148, 120), "地图 Agent 对话", font=font(22), fill=(15, 23, 42))
    bubbles = [
        (180, (241, 245, 249), "定位到合肥，只显示高速公路"),
        (280, (224, 242, 254), "已定位到合肥，并只显示高速公路。"),
        (380, (241, 245, 249), "分析当前视野内的道路里程"),
        (480, (220, 252, 231), "当前视野内道路 1284 条，长度约 326.5 公里。"),
    ]
    for y, color, text in bubbles:
        rounded(draw, (1148, y, 1532, y + 70), color, None, 12)
        draw.text((1164, y + 24), text[:22], font=font(16), fill=(30, 41, 59))
        if len(text) > 22:
            draw.text((1164, y + 44), text[22:], font=font(16), fill=(30, 41, 59))

    rounded(draw, (1148, 820, 1532, 880), (241, 245, 249), (203, 213, 225), 12)
    draw.text((1164, 842), "用中文继续指挥地图…", font=font(16), fill=(148, 163, 184))

    path = OUT / "ui-overview.png"
    img.save(path, "PNG")
    return path


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    paths = [make_architecture(), make_agent(), make_pipeline(), make_ui_mock()]
    for p in paths:
        print(p, p.stat().st_size)


if __name__ == "__main__":
    main()
