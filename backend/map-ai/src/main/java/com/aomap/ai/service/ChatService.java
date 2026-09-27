package com.aomap.ai.service;

import com.aomap.ai.dialogue.DialoguePlan;
import com.aomap.ai.dialogue.FollowUp;
import com.aomap.ai.dialogue.MapAction;
import com.aomap.ai.dialogue.RuleDialogueEngine;
import com.aomap.ai.domain.ModelRecord;
import com.aomap.common.dto.GeoDtos.AnalysisResult;
import com.aomap.common.dto.GeoDtos.LayerCount;
import com.aomap.common.dto.GeoDtos.SearchHit;
import com.aomap.common.dto.GeoDtos.StatItem;
import com.aomap.common.geo.Layers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ChatService {

    private static final String SYSTEM = """
            你是 WebGIS 地图助手，只能用 JSON 控制地图，不要输出其它文字。
            可用图层：highway 道路，railway 铁路，waterway 水系线，water 水体，building 建筑，landuse 土地利用，boundary 行政区划，poi 兴趣点，place 地名。
            输出格式：{"reply":"中文说明","actions":[{"type":"show_layers","layers":["highway"]}],"followUps":[{"kind":"search","keyword":"大学","layer":"poi"}]}
            action.type 只能是 show_layers、hide_layers、only_layers、fly_to、clear_markers。
            fly_to 需要 center:[经度,纬度] 和 zoom。
            followUps.kind 只能是 stats、layers、search、analyze。analyze 需要 layer 和 metric（count、length、area）。
            """;

    private final RuleDialogueEngine rules = new RuleDialogueEngine();
    private final ModelConfigService models;
    private final LlmClient llmClient;
    private final GeoApi geoApi;
    private final ObjectMapper objectMapper;

    public ChatService(ModelConfigService models, LlmClient llmClient, GeoApi geoApi, ObjectMapper objectMapper) {
        this.models = models;
        this.llmClient = llmClient;
        this.geoApi = geoApi;
        this.objectMapper = objectMapper;
    }

    public ChatResponse chat(ChatRequest request) {
        String text = lastUserText(request);
        if (text.isBlank()) {
            throw new IllegalArgumentException("请输入内容");
        }
        var ruled = rules.plan(text);
        if (ruled.isPresent()) {
            return finish(ruled.get(), "rule", request);
        }
        ModelRecord model = models.enabled();
        if (model == null) {
            return new ChatResponse(RuleDialogueEngine.HELP, "rule", List.of(), List.of());
        }
        String content = llmClient.complete(model.getBaseUrl(), model.getApiKey(), model.getModel(),
                model.getTemperature() == null ? 0.2 : model.getTemperature(), prompt(request, text));
        return finish(parseModel(content), "model", request);
    }

    private ChatResponse finish(DialoguePlan plan, String source, ChatRequest request) {
        List<MapAction> actions = new ArrayList<>(plan.actions() == null ? List.of() : plan.actions());
        List<DataTable> tables = new ArrayList<>();
        StringBuilder reply = new StringBuilder(plan.reply() == null ? "" : plan.reply());
        for (FollowUp followUp : plan.followUps() == null ? List.<FollowUp>of() : plan.followUps()) {
            apply(followUp, request, actions, tables, reply);
        }
        return new ChatResponse(reply.toString().trim(), source, actions, tables);
    }

    private void apply(FollowUp followUp, ChatRequest request, List<MapAction> actions, List<DataTable> tables, StringBuilder reply) {
        switch (followUp.kind()) {
            case "layers" -> {
                List<LayerCount> layers = geoApi.layers();
                List<List<Object>> rows = new ArrayList<>();
                long total = 0;
                for (LayerCount layer : layers) {
                    rows.add(List.of(layer.label(), layer.count()));
                    total += layer.count();
                }
                tables.add(new DataTable("图层数量", List.of("图层", "数量"), rows));
                reply.append("\n库内共 ").append(total).append(" 条要素。");
            }
            case "stats" -> {
                List<StatItem> items = geoApi.stats(followUp.layer());
                List<List<Object>> rows = new ArrayList<>();
                for (StatItem item : items) {
                    rows.add(List.of(item.subtype(), item.count()));
                }
                tables.add(new DataTable(Layers.label(followUp.layer()) + "分类", List.of("类型", "数量"), rows));
                reply.append("\n").append(Layers.label(followUp.layer())).append("共 ")
                        .append(items.stream().mapToLong(StatItem::count).sum()).append(" 条。");
            }
            case "search" -> {
                List<SearchHit> hits = geoApi.search(followUp.keyword(), followUp.layer());
                if (hits.isEmpty()) {
                    reply.append("\n没有找到“").append(followUp.keyword()).append("”。");
                    return;
                }
                List<MapAction.Marker> markers = new ArrayList<>();
                List<List<Object>> rows = new ArrayList<>();
                for (SearchHit hit : hits) {
                    markers.add(new MapAction.Marker(hit.name(), hit.layer(), hit.subtype(), hit.lon(), hit.lat()));
                    rows.add(List.of(hit.name(), Layers.label(hit.layer()), hit.subtype() == null ? "" : hit.subtype()));
                }
                actions.add(new MapAction("markers", null, null, null, null, null, markers));
                SearchHit first = hits.get(0);
                actions.add(new MapAction("fly_to", null, null, List.of(first.lon(), first.lat()), 13.0, first.name(), null));
                tables.add(new DataTable("查找结果", List.of("名称", "图层", "类型"), rows));
                reply.append("\n找到 ").append(hits.size()).append(" 条，已定位到").append(first.name()).append("。");
            }
            case "analyze" -> {
                double[] bbox = bbox(request);
                AnalysisResult result = geoApi.analyze(followUp.layer(), bbox[0], bbox[1], bbox[2], bbox[3]);
                tables.add(new DataTable("视野统计", List.of("指标", "数值"), List.of(
                        List.of("数量", result.count()),
                        List.of("长度", formatLength(result.lengthMeters())),
                        List.of("面积", formatArea(result.areaSquareMeters()))
                )));
                reply.append("\n当前视野内").append(Layers.label(result.layer()))
                        .append(" ").append(result.count()).append(" 条");
                if ("length".equals(followUp.metric())) {
                    reply.append("，长度约 ").append(formatLength(result.lengthMeters()));
                } else if ("area".equals(followUp.metric())) {
                    reply.append("，面积约 ").append(formatArea(result.areaSquareMeters()));
                }
                reply.append("。统计的是与视野相交的完整要素。");
            }
            default -> {
            }
        }
    }

    private DialoguePlan parseModel(String content) {
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            String reply = root.path("reply").asText(content);
            List<MapAction> actions = new ArrayList<>();
            for (JsonNode node : root.path("actions")) {
                actions.add(readAction(node));
            }
            List<FollowUp> followUps = new ArrayList<>();
            for (JsonNode node : root.path("followUps")) {
                String kind = node.path("kind").asText("");
                if (!kind.isBlank()) {
                    followUps.add(new FollowUp(kind, text(node, "layer"), text(node, "keyword"), text(node, "metric")));
                }
            }
            return new DialoguePlan(reply, actions, followUps);
        } catch (Exception ex) {
            return new DialoguePlan(content, List.of(), List.of());
        }
    }

    private MapAction readAction(JsonNode node) {
        String type = node.path("type").asText("");
        List<String> layers = new ArrayList<>();
        node.path("layers").forEach(item -> {
            if (Layers.known(item.asText())) {
                layers.add(item.asText());
            }
        });
        List<Double> center = null;
        if (node.path("center").size() == 2) {
            center = List.of(node.path("center").get(0).asDouble(), node.path("center").get(1).asDouble());
        }
        Double zoom = node.has("zoom") ? node.path("zoom").asDouble() : null;
        return new MapAction(type, layers.isEmpty() ? null : layers, null, center, zoom, text(node, "label"), null);
    }

    private List<LlmClient.Message> prompt(ChatRequest request, String text) {
        List<LlmClient.Message> messages = new ArrayList<>();
        messages.add(new LlmClient.Message("system", SYSTEM + "\n当前视野：" + bboxText(request)));
        List<ChatRequest.Message> history = request.messages() == null ? List.of() : request.messages();
        int from = Math.max(0, history.size() - 8);
        for (int i = from; i < history.size(); i++) {
            ChatRequest.Message message = history.get(i);
            if (message != null && message.content() != null && !message.content().isBlank()) {
                String role = "assistant".equals(message.role()) ? "assistant" : "user";
                messages.add(new LlmClient.Message(role, message.content()));
            }
        }
        if (messages.size() == 1) {
            messages.add(new LlmClient.Message("user", text));
        }
        return messages;
    }

    private static String lastUserText(ChatRequest request) {
        if (request == null || request.messages() == null) {
            return "";
        }
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            ChatRequest.Message message = request.messages().get(i);
            if (message != null && (message.role() == null || "user".equals(message.role())) && message.content() != null) {
                return message.content().trim();
            }
        }
        return "";
    }

    private static double[] bbox(ChatRequest request) {
        if (request != null && request.map() != null && request.map().bbox() != null && request.map().bbox().size() == 4) {
            List<Double> box = request.map().bbox();
            return new double[]{box.get(0), box.get(1), box.get(2), box.get(3)};
        }
        return new double[]{114.88, 29.39, 119.65, 34.65};
    }

    private static String bboxText(ChatRequest request) {
        double[] box = bbox(request);
        return String.format(Locale.ROOT, "[%.4f, %.4f, %.4f, %.4f]", box[0], box[1], box[2], box[3]);
    }

    private static String text(JsonNode node, String field) {
        return node.has(field) && !node.path(field).isNull() ? node.path(field).asText() : null;
    }

    static String formatLength(double meters) {
        if (meters >= 1000) {
            return String.format(Locale.ROOT, "%.1f 公里", meters / 1000);
        }
        return String.format(Locale.ROOT, "%.0f 米", meters);
    }

    static String formatArea(double squareMeters) {
        if (squareMeters >= 1_000_000) {
            return String.format(Locale.ROOT, "%.2f 平方公里", squareMeters / 1_000_000);
        }
        return String.format(Locale.ROOT, "%.0f 平方米", squareMeters);
    }

    public record ChatRequest(List<Message> messages, MapContext map) {
        public record Message(String role, String content) {
        }

        public record MapContext(List<Double> center, Double zoom, List<Double> bbox, List<String> visibleLayers) {
        }
    }

    public record ChatResponse(String reply, String source, List<MapAction> actions, List<DataTable> tables) {
    }

    public record DataTable(String title, List<String> columns, List<List<Object>> rows) {
    }
}
