package com.aomap.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDialogueEngineTest {

    private final RuleDialogueEngine engine = new RuleDialogueEngine();

    @Test
    void fliesToHefei() {
        DialoguePlan plan = engine.plan("定位到合肥").orElseThrow();
        MapAction fly = plan.actions().stream().filter(action -> "fly_to".equals(action.type())).findFirst().orElseThrow();
        assertEquals(117.227, fly.center().get(0), 0.01);
        assertEquals(31.821, fly.center().get(1), 0.01);
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
    void helpAndUnknown() {
        assertTrue(engine.plan("你能做什么").orElseThrow().reply().contains("显示道路"));
        assertTrue(engine.plan("今天天气怎么样").isEmpty());
    }
}
