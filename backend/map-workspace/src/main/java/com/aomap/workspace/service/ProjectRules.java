package com.aomap.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

public final class ProjectRules {

    public static final int MAX_STATE_CHARS = 200_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ProjectRules() {
    }

    public static String name(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("项目名称需要 1 到 128 个字");
        }
        return value;
    }

    public static String state(ObjectMapper mapper, JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("请提供地图状态");
        }
        try {
            String json = mapper.writeValueAsString(node);
            if (json.length() > MAX_STATE_CHARS) {
                throw new IllegalArgumentException("地图状态过大");
            }
            return json;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("地图状态无法保存");
        }
    }

    public static void requireWriter(String role) {
        if (!"OWNER".equals(role) && !"EDITOR".equals(role)) {
            throw new IllegalArgumentException("当前角色只能查看项目");
        }
    }

    public static void requireDelete(String role, long userId, long ownerUserId) {
        if ("OWNER".equals(role) || userId == ownerUserId) {
            return;
        }
        throw new IllegalArgumentException("只能删除自己的项目");
    }

    public static OffsetDateTime expiry(Integer days) {
        int value = days == null ? 7 : days;
        if (value < 1 || value > 30) {
            throw new IllegalArgumentException("分享有效期为 1 到 30 天");
        }
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(value);
    }

    public static String token() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
