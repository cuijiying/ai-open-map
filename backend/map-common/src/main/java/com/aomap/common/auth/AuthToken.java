package com.aomap.common.auth;

public record AuthToken(long userId, long orgId, String role, String name) {
}
