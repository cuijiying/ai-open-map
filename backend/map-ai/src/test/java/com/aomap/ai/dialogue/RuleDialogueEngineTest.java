package com.aomap.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDialogueEngineTest {

    private final RuleDialogueEngine engine = new RuleDialogueEngine();

    @Test
    void fliesToHangzhouAndResetsToCurrentProvince() {
        DialoguePlan hangzhou = engine.plan("定位到杭州").orElseThrow();
        MapAction fly = hangzhou.actions().stream().filter(action -> "fly_to".equals(action.type())).findFirst().orElseThrow();
        assertEquals(120.155, fly.center().get(0), 0.01);
        assertEquals(30.274, fly.center().get(1), 0.01);
        assertEquals("zhejiang", fly.regionCode());
        assertTrue(hangzhou.reply().contains("浙江省"));
        DialoguePlan reset = engine.plan("重置地图", "zhejiang").orElseThrow();
        MapAction back = reset.actions().stream().filter(action -> "fly_to".equals(action.type())).findFirst().orElseThrow();
        assertEquals(120.20, back.center().get(0), 0.01);
        assertTrue(reset.reply().contains("浙江省"));
    }

    @Test
    void fliesToHefei() {
        DialoguePlan plan = engine.plan("定位到合肥").orElseThrow();
        MapAction fly = plan.actions().stream().filter(action -> "fly_to".equals(action.type())).findFirst().orElseThrow();
        assertEquals(117.227, fly.center().get(0), 0.01);
        assertEquals(31.821, fly.center().get(1), 0.01);
        assertEquals("anhui", fly.regionCode());
    }

    @Test
    void filtersMotorways() {
        DialoguePlan plan = engine.plan("只显示高速公路").orElseThrow();
        MapAction action = plan.actions().get(0);
        assertEquals("only_layers", action.type());
        assertEquals("highway", action.layers().get(0));
        assertTrue(action.filters().get("highway").contains("motorway"));
    }

    @Test
    void showsRoadsAndHidesBuildings() {
        DialoguePlan show = engine.plan("显示道路").orElseThrow();
        assertEquals("show_layers", show.actions().get(0).type());
        assertEquals("highway", show.actions().get(0).layers().get(0));
        DialoguePlan hide = engine.plan("隐藏建筑").orElseThrow();
        assertEquals("hide_layers", hide.actions().get(0).type());
        assertEquals("building", hide.actions().get(0).layers().get(0));
    }

    @Test
    void switchesStyleAndRecolorsRoads() {
        DialoguePlan dark = engine.plan("切换深色风格").orElseThrow();
        assertEquals("set_style", dark.actions().get(0).type());
        assertEquals("dark", dark.actions().get(0).label());
        DialoguePlan imagery = engine.plan("切换影像风格").orElseThrow();
        assertEquals("imagery", imagery.actions().get(0).label());
        DialoguePlan color = engine.plan("把道路改成红色").orElseThrow();
        assertEquals("paint_layer", color.actions().get(0).type());
        assertEquals("highway", color.actions().get(0).layers().get(0));
        assertEquals("#dc2626", color.actions().get(0).label());
        DialoguePlan motorway = engine.plan("把高速公路改成蓝色").orElseThrow();
        assertTrue(motorway.actions().get(0).filters().get("highway").contains("motorway"));
        assertEquals("#2563eb", motorway.actions().get(0).label());
        DialoguePlan reset = engine.plan("恢复默认样式").orElseThrow();
        assertEquals("reset", reset.actions().get(0).label());
        assertTrue(engine.plan("显示道路").orElseThrow().actions().stream().noneMatch(action -> "set_style".equals(action.type())));
    }

    @Test
    void helpAndUnknown() {
        assertTrue(engine.plan("你能做什么").orElseThrow().reply().contains("显示道路"));
        assertTrue(engine.plan("今天天气怎么样").isEmpty());
    }
}
