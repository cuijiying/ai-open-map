package com.aomap.ai.service;

import com.aomap.ai.domain.ModelRecord;
import com.aomap.ai.mapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelConfigService {

    private final ModelMapper models;
    private final LlmClient llmClient;

    public ModelConfigService(ModelMapper models, LlmClient llmClient) {
        this.models = models;
        this.llmClient = llmClient;
    }

    public List<ModelView> list() {
        List<ModelView> views = new ArrayList<>();
        for (ModelRecord record : models.list()) {
            views.add(toView(record));
        }
        return views;
    }

    public ModelView create(ModelForm form) {
        validate(form);
        ModelRecord record = new ModelRecord();
        copy(form, record, form.apiKey() == null ? "" : form.apiKey());
        record.setEnabled(false);
        models.insert(record);
        return toView(models.findById(record.getId()));
    }

    public ModelView update(long id, ModelForm form) {
        ModelRecord existing = require(id);
        validate(form);
        String key = form.apiKey() == null || form.apiKey().isBlank() ? existing.getApiKey() : form.apiKey().trim();
        copy(form, existing, key);
        models.update(existing);
        return toView(models.findById(id));
    }

    public void delete(long id) {
        require(id);
        models.delete(id);
    }

    @Transactional
    public ModelView enable(long id) {
        require(id);
        models.disableAll();
        models.enable(id);
        return toView(models.findById(id));
    }

    public String test(ModelForm form) {
        String key = form.apiKey();
        if ((key == null || key.isBlank()) && form.id() != null) {
            key = require(form.id()).getApiKey();
        }
        if (form.baseUrl() == null || form.baseUrl().isBlank() || form.model() == null || form.model().isBlank()) {
            throw new IllegalArgumentException("请填写接口地址和模型名称");
        }
        String content = llmClient.complete(
                form.baseUrl(),
                key,
                form.model(),
                form.temperature() == null ? 0.2 : form.temperature(),
                List.of(new LlmClient.Message("user", "只回复OK"))
        );
        return content.length() > 80 ? content.substring(0, 80) : content;
    }

    public ModelRecord enabled() {
        return models.findEnabled();
    }

    private ModelRecord require(long id) {
        ModelRecord record = models.findById(id);
        if (record == null) {
            throw new IllegalArgumentException("模型配置不存在");
        }
        return record;
    }

    private static void validate(ModelForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("请填写配置名称");
        }
        if (form.baseUrl() == null || form.baseUrl().isBlank()) {
            throw new IllegalArgumentException("请填写接口地址");
        }
        if (form.model() == null || form.model().isBlank()) {
            throw new IllegalArgumentException("请填写模型名称");
        }
    }

    private static void copy(ModelForm form, ModelRecord record, String apiKey) {
        record.setName(form.name().trim());
        record.setBaseUrl(form.baseUrl().trim());
        record.setApiKey(apiKey == null ? "" : apiKey.trim());
        record.setModel(form.model().trim());
        record.setTemperature(form.temperature() == null ? 0.2 : form.temperature());
    }

    private static ModelView toView(ModelRecord record) {
        return new ModelView(
                record.getId(),
                record.getName(),
                record.getBaseUrl(),
                mask(record.getApiKey()),
                record.getApiKey() != null && !record.getApiKey().isBlank(),
                record.getModel(),
                record.getTemperature() == null ? 0.2 : record.getTemperature(),
                Boolean.TRUE.equals(record.getEnabled())
        );
    }

    static String mask(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }

    public record ModelForm(Long id, String name, String baseUrl, String apiKey, String model, Double temperature) {
    }

    public record ModelView(long id, String name, String baseUrl, String apiKeyMasked, boolean hasKey,
                            String model, double temperature, boolean enabled) {
    }
}
