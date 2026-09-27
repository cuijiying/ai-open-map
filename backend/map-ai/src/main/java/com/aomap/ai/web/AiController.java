package com.aomap.ai.web;

import com.aomap.ai.service.ChatService;
import com.aomap.ai.service.ChatService.ChatRequest;
import com.aomap.ai.service.ChatService.ChatResponse;
import com.aomap.ai.service.ModelConfigService;
import com.aomap.ai.service.ModelConfigService.ModelForm;
import com.aomap.ai.service.ModelConfigService.ModelView;
import com.aomap.common.api.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ModelConfigService models;
    private final ChatService chatService;

    public AiController(ModelConfigService models, ChatService chatService) {
        this.models = models;
        this.chatService = chatService;
    }

    @GetMapping("/models")
    public ApiResult<List<ModelView>> list() {
        return ApiResult.ok(models.list());
    }

    @PostMapping("/models")
    public ApiResult<ModelView> create(@RequestBody ModelForm form) {
        return ApiResult.ok(models.create(form));
    }

    @PutMapping("/models/{id}")
    public ApiResult<ModelView> update(@PathVariable long id, @RequestBody ModelForm form) {
        return ApiResult.ok(models.update(id, form));
    }

    @DeleteMapping("/models/{id}")
    public ApiResult<Void> delete(@PathVariable long id) {
        models.delete(id);
        return ApiResult.ok(null);
    }

    @PostMapping("/models/{id}/enable")
    public ApiResult<ModelView> enable(@PathVariable long id) {
        return ApiResult.ok(models.enable(id));
    }

    @PostMapping("/models/test")
    public ApiResult<Map<String, String>> test(@RequestBody ModelForm form) {
        return ApiResult.ok(Map.of("reply", models.test(form)));
    }

    @PostMapping("/chat")
    public ApiResult<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ApiResult.ok(chatService.chat(request));
    }
}
