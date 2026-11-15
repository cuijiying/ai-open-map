package com.aomap.workspace.service;

import com.aomap.common.auth.AuthToken;
import com.aomap.workspace.domain.ProjectRow;
import com.aomap.workspace.domain.ShareRow;
import com.aomap.workspace.mapper.ProjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectMapper projects;
    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public ProjectService(ProjectMapper projects, ObjectMapper mapper) {
        this.projects = projects;
        this.mapper = mapper;
    }

    public List<ProjectView> list(AuthToken actor) {
        return projects.list(actor.orgId()).stream().map(ProjectService::view).toList();
    }

    public ProjectView get(AuthToken actor, long id) {
        return view(require(actor.orgId(), id));
    }

    @Transactional
    public ProjectView create(AuthToken actor, String name, JsonNode state) {
        ProjectRules.requireWriter(actor.role());
        ProjectRow row = new ProjectRow();
        row.setOrgId(actor.orgId());
        row.setOwnerUserId(actor.userId());
        row.setName(ProjectRules.name(name));
        row.setState(ProjectRules.state(mapper, state));
        projects.insert(row);
        return get(actor, row.getId());
    }

    @Transactional
    public ProjectView update(AuthToken actor, long id, String name, JsonNode state) {
        ProjectRules.requireWriter(actor.role());
        require(actor.orgId(), id);
        int changed = projects.update(actor.orgId(), id, ProjectRules.name(name), ProjectRules.state(mapper, state));
        if (changed != 1) {
            throw new IllegalArgumentException("项目不存在");
        }
        return get(actor, id);
    }

    @Transactional
    public void delete(AuthToken actor, long id) {
        ProjectRow row = require(actor.orgId(), id);
        ProjectRules.requireDelete(actor.role(), actor.userId(), row.getOwnerUserId());
        projects.delete(actor.orgId(), id);
    }

    @Transactional
    public ShareLink share(AuthToken actor, long id, Integer days, String password) {
        ProjectRules.requireWriter(actor.role());
        require(actor.orgId(), id);
        String token = ProjectRules.token();
        String hash = password == null || password.isBlank() ? null : passwords.encode(password);
        if (password != null && !password.isBlank() && (password.length() < 4 || password.length() > 72)) {
            throw new IllegalArgumentException("访问密码需要 4 到 72 位");
        }
        projects.insertShare(id, token, hash, ProjectRules.expiry(days));
        return new ShareLink(token, hash != null);
    }

    public ProjectView openShare(String token, String password) {
        ShareRow share = projects.findShare(token == null ? "" : token.trim());
        if (share == null) {
            throw new IllegalArgumentException("分享不存在或已失效");
        }
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("分享已过期");
        }
        if (share.getPasswordHash() != null && !passwords.matches(password == null ? "" : password, share.getPasswordHash())) {
            throw new IllegalArgumentException("请填写正确的访问密码");
        }
        return new ProjectView(share.getProjectId(), share.getName(), share.getState(), null, true);
    }

    private ProjectRow require(long orgId, long id) {
        ProjectRow row = projects.find(orgId, id);
        if (row == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return row;
    }

    private static ProjectView view(ProjectRow row) {
        return new ProjectView(row.getId(), row.getName(), row.getState(), row.getUpdatedAt(), false);
    }

    public record ProjectView(Long id, String name, String state, OffsetDateTime updatedAt, boolean shared) {
    }

    public record ShareLink(String token, boolean passwordRequired) {
    }
}
