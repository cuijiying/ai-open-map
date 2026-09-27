package com.aomap.ai.dialogue;

import com.aomap.common.geo.Layers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RuleDialogueEngine {

    public static final String HELP = """
            我可以帮你操作地图。未配置模型时，直接说这些指令即可：
            · 显示道路 / 水系 / 建筑 / 铁路 / 土地利用 / 兴趣点 / 地名 / 行政区划
            · 只显示高速公路，或隐藏建筑
            · 定位到合肥、黄山、芜湖等安徽城市
            · 统计道路数量，或查找大学
            · 分析当前视野内的道路里程
            · 重置地图
            配置 OpenAI 兼容模型后，也可以用自己的话提问。""";

    private static final List<String> DEFAULT_LAYERS = List.of("highway", "waterway", "water", "place", "boundary");

    private static final List<Place> PLACES = List.of(
            new Place("安徽省", 117.283, 31.861, 6.5),
            new Place("黄山市", 118.338, 29.715, 10),
            new Place("合肥市", 117.227, 31.821, 11),
            new Place("马鞍山", 118.508, 31.670, 11),
            new Place("淮南市", 117.000, 32.626, 11),
            new Place("淮北市", 116.798, 33.956, 11),
            new Place("铜陵市", 117.812, 30.945, 11),
            new Place("安庆市", 117.064, 30.544, 11),
            new Place("滁州市", 118.316, 32.304, 11),
            new Place("阜阳市", 115.814, 32.890, 11),
            new Place("宿州市", 116.964, 33.646, 11),
            new Place("六安市", 116.522, 31.736, 11),
            new Place("亳州市", 115.779, 33.845, 11),
            new Place("池州市", 117.491, 30.665, 11),
            new Place("宣城市", 118.759, 30.941, 11),
            new Place("蚌埠市", 117.389, 32.917, 11),
            new Place("芜湖市", 118.376, 31.326, 11),
            new Place("黄山", 118.167, 30.133, 11),
            new Place("合肥", 117.227, 31.821, 11),
            new Place("芜湖", 118.376, 31.326, 11),
            new Place("蚌埠", 117.389, 32.917, 11),
            new Place("淮南", 117.000, 32.626, 11),
            new Place("淮北", 116.798, 33.956, 11),
            new Place("铜陵", 117.812, 30.945, 11),
            new Place("安庆", 117.064, 30.544, 11),
            new Place("滁州", 118.316, 32.304, 11),
            new Place("阜阳", 115.814, 32.890, 11),
            new Place("宿州", 116.964, 33.646, 11),
            new Place("六安", 116.522, 31.736, 11),
            new Place("亳州", 115.779, 33.845, 11),
            new Place("池州", 117.491, 30.665, 11),
            new Place("宣城", 118.759, 30.941, 11),
            new Place("巢湖", 117.874, 31.598, 11),
            new Place("九华山", 117.803, 30.478, 12)
    );

    private static final List<LayerWord> LAYER_WORDS = List.of(
            new LayerWord(List.of("water", "waterway"), List.of("水系", "河流", "湖泊", "水体"), null),
            new LayerWord(List.of("highway"), List.of("高速公路", "高速路", "高速"), List.of("motorway", "motorway_link", "trunk", "trunk_link")),
            new LayerWord(List.of("highway"), List.of("主干道", "主干路", "主要道路"), List.of("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link")),
            new LayerWord(List.of("highway"), List.of("道路", "公路", "路网"), null),
            new LayerWord(List.of("railway"), List.of("铁路", "高铁"), null),
            new LayerWord(List.of("building"), List.of("建筑物", "建筑", "房屋"), null),
            new LayerWord(List.of("landuse"), List.of("土地利用", "用地"), null),
            new LayerWord(List.of("boundary"), List.of("行政区划", "行政区", "边界", "区划"), null),
            new LayerWord(List.of("poi"), List.of("兴趣点", "poi", "设施"), null),
            new LayerWord(List.of("place"), List.of("地名", "居民点"), null)
    );

    public Optional<DialoguePlan> plan(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (containsAny(text, "帮助", "你能做什么", "怎么用", "有哪些指令")) {
            return Optional.of(new DialoguePlan(HELP, List.of(), List.of()));
        }
        if (containsAny(text, "重置地图", "恢复默认")) {
            return Optional.of(new DialoguePlan("已恢复默认图层，并回到安徽省。", List.of(
                    action("only_layers", DEFAULT_LAYERS, null),
                    fly(place("安徽省")),
                    action("clear_markers", null, null)
            ), List.of()));
        }
        if (containsAny(text, "清除标记", "清空标记")) {
            return Optional.of(new DialoguePlan("已清除地图标记。", List.of(action("clear_markers", null, null)), List.of()));
        }

        Place place = findPlace(text);
        LayerMatch layers = findLayers(text);
        if (containsAny(text, "统计", "有多少", "数量", "多少条", "多少个")) {
            return Optional.of(statsPlan(text, place, layers));
        }
        if (containsAny(text, "搜索", "查找", "找一下", "查询")) {
            String keyword = cleanup(text, layers, place);
            if (keyword.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(searchPlan(keyword, layers, place));
        }
        if (containsAny(text, "分析", "视野", "范围内", "当前范围")) {
            String layer = layers.layers().isEmpty() ? "highway" : layers.layers().get(0);
            String metric = text.contains("面积") ? "area" : text.contains("长度") || text.contains("里程") ? "length" : "count";
            return Optional.of(new DialoguePlan("正在统计当前视野。", List.of(), List.of(new FollowUp("analyze", layer, null, metric))));
        }
        if (place != null && containsAny(text, "定位", "飞到", "跳转", "前往", "去到", "看看")) {
            return Optional.of(flyPlan(place, layers));
        }
        if (containsAny(text, "只显示", "只看", "仅显示", "只加载")) {
            if (layers.layers().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new DialoguePlan("已只显示" + labels(layers.layers()) + filterText(layers) + "。",
                    List.of(action("only_layers", layers.layers(), layers.filters())), List.of()));
        }
        if (containsAny(text, "隐藏", "关掉", "关闭")) {
            if (layers.layers().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new DialoguePlan("已隐藏" + labels(layers.layers()) + "。",
                    List.of(action("hide_layers", layers.layers(), null)), List.of()));
        }
        if (containsAny(text, "显示", "加载", "打开")) {
            if (layers.layers().isEmpty() && place == null) {
                return Optional.empty();
            }
            return Optional.of(showPlan(place, layers));
        }
        if (place != null && text.replace(place.name().toLowerCase(Locale.ROOT), "").isBlank()) {
            return Optional.of(flyPlan(place, new LayerMatch(List.of(), Map.of())));
        }
        return Optional.empty();
    }

    private DialoguePlan statsPlan(String text, Place place, LayerMatch layers) {
        List<MapAction> actions = new ArrayList<>();
        if (place != null) {
            actions.add(fly(place));
        }
        if (!layers.layers().isEmpty()) {
            actions.add(action("show_layers", layers.layers(), layers.filters()));
            return new DialoguePlan("正在统计" + labels(layers.layers()) + "。", actions,
                    List.of(new FollowUp("stats", layers.layers().get(0), null, null)));
        }
        String keyword = cleanup(text, layers, place);
        if (!keyword.isBlank()) {
            return searchPlan(keyword, layers, place);
        }
        return new DialoguePlan("正在统计各类数据。", actions, List.of(new FollowUp("layers", null, null, null)));
    }

    private DialoguePlan searchPlan(String keyword, LayerMatch layers, Place place) {
        List<MapAction> actions = new ArrayList<>();
        if (place != null) {
            actions.add(fly(place));
        }
        String layer = layers.layers().isEmpty() ? null : layers.layers().get(0);
        return new DialoguePlan("正在查找“" + keyword + "”。", actions, List.of(new FollowUp("search", layer, keyword, null)));
    }

    private DialoguePlan showPlan(Place place, LayerMatch layers) {
        List<MapAction> actions = new ArrayList<>();
        StringBuilder reply = new StringBuilder();
        if (!layers.layers().isEmpty()) {
            actions.add(action("show_layers", layers.layers(), layers.filters()));
            reply.append("已显示").append(labels(layers.layers())).append(filterText(layers));
        }
        if (place != null) {
            actions.add(fly(place));
            if (!reply.isEmpty()) {
                reply.append("，");
            }
            reply.append("已定位到").append(place.name());
        }
        reply.append("。");
        return new DialoguePlan(reply.toString(), actions, List.of());
    }

    private DialoguePlan flyPlan(Place place, LayerMatch layers) {
        List<MapAction> actions = new ArrayList<>();
        actions.add(fly(place));
        String extra = "";
        if (!layers.layers().isEmpty()) {
            actions.add(0, action("show_layers", layers.layers(), layers.filters()));
            extra = "，并显示" + labels(layers.layers());
        }
        return new DialoguePlan("已定位到" + place.name() + extra + "。", actions, List.of());
    }

    private static MapAction action(String type, List<String> layers, Map<String, List<String>> filters) {
        return new MapAction(type, layers, filters == null || filters.isEmpty() ? null : filters, null, null, null, null);
    }

    private static MapAction fly(Place place) {
        return new MapAction("fly_to", null, null, List.of(place.lon(), place.lat()), place.zoom(), place.name(), null);
    }

    private static LayerMatch findLayers(String text) {
        String rest = text;
        List<String> layers = new ArrayList<>();
        Map<String, List<String>> filters = new LinkedHashMap<>();
        List<LayerWord> words = new ArrayList<>(LAYER_WORDS);
        words.sort((a, b) -> Integer.compare(longest(b), longest(a)));
        for (LayerWord word : words) {
            for (String token : word.tokens()) {
                if (rest.contains(token)) {
                    for (String layer : word.layers()) {
                        if (!layers.contains(layer)) {
                            layers.add(layer);
                        }
                    }
                    if (word.filter() != null && word.layers().size() == 1) {
                        filters.put(word.layers().get(0), word.filter());
                    }
                    rest = rest.replace(token, " ");
                    break;
                }
            }
        }
        return new LayerMatch(layers, filters);
    }

    private static Place findPlace(String text) {
        return PLACES.stream()
                .sorted((a, b) -> Integer.compare(b.name().length(), a.name().length()))
                .filter(place -> text.contains(place.name().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private static Place place(String name) {
        return PLACES.stream().filter(item -> item.name().equals(name)).findFirst().orElseThrow();
    }

    private static String cleanup(String text, LayerMatch layers, Place place) {
        String rest = text;
        for (String token : List.of("统计", "有多少", "数量", "多少条", "多少个", "搜索", "查找", "找一下", "查询",
                "显示", "加载", "打开", "请", "帮我", "一下", "的", "吗", "呢", "地图", "数据", "当前")) {
            rest = rest.replace(token, " ");
        }
        for (LayerWord word : LAYER_WORDS) {
            for (String token : word.tokens()) {
                rest = rest.replace(token, " ");
            }
        }
        if (place != null) {
            rest = rest.replace(place.name().toLowerCase(Locale.ROOT), " ");
        }
        return rest.replaceAll("\\s+", "").trim();
    }

    private static String labels(List<String> layers) {
        return String.join("、", layers.stream().map(Layers::label).toList());
    }

    private static String filterText(LayerMatch layers) {
        if (layers.filters().isEmpty()) {
            return "";
        }
        return "（已按类型过滤）";
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static int longest(LayerWord word) {
        return word.tokens().stream().mapToInt(String::length).max().orElse(0);
    }

    private record Place(String name, double lon, double lat, double zoom) {
    }

    private record LayerWord(List<String> layers, List<String> tokens, List<String> filter) {
    }

    private record LayerMatch(List<String> layers, Map<String, List<String>> filters) {
    }
}
