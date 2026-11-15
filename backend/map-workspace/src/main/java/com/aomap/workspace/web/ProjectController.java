package com.aomap.workspace.web;

import com.aomap.common.api.ApiResult;
import com.aomap.common.auth.AuthToken;
import com.aomap.common.auth.AuthTokens;
import com.aomap.workspace.service.ProjectService;
import com.aomap.workspace.service.ProjectService.ProjectView;
import com.aomap.workspace.service.ProjectService.ShareLink;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projects;
    private final String jwtSecret;

    public ProjectController(ProjectService projects, @Value("${auth.jwt-secret}") String jwtSecret) {
        this.projects = projects;
        this.jwtSecret = jwtSecret;
    }

    @GetMapping("/projects")
    public ApiResult<List<ProjectView>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResult.ok(projects.list(actor(authorization)));
    }

    @PostMapping("/projects")
    public ApiResult<ProjectView> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody ProjectRequest request) {
        return ApiResult.ok(projects.create(actor(authorization), request.name(), request.state()));
    }

    @GetMapping("/projects/{id}")
    public ApiResult<ProjectView> get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable long id) {
        return ApiResult.ok(projects.get(actor(authorization), id));
    }

    @PutMapping("/projects/{id}")
    public ApiResult<ProjectView> update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable long id,
                                         @RequestBody ProjectRequest request) {
        return ApiResult.ok(projects.update(actor(authorization), id, request.name(), request.state()));
    }

    @DeleteMapping("/projects/{id}")
    public ApiResult<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable long id) {
        projects.delete(actor(authorization), id);
        return ApiResult.ok(null);
    }

    @PostMapping("/projects/{id}/shares")
    public ApiResult<ShareLink> share(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable long id,
                                      @RequestBody(required = false) ShareRequest request) {
        Integer days = request == null ? null : request.days();
        String password = request == null ? null : request.password();
        return ApiResult.ok(projects.share(actor(authorization), id, days, password));
    }

    @PostMapping("/shares/{token}/open")
    public ApiResult<ProjectView> open(@PathVariable String token, @RequestBody(required = false) OpenRequest request) {
        String password = request == null ? null : request.password();
        return ApiResult.ok(projects.openShare(token, password));
    }

    private AuthToken actor(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }
        try {
            return AuthTokens.parse(jwtSecret, authorization.substring(7).trim());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
    }

    public record ProjectRequest(String name, JsonNode state) {
    }

    public record ShareRequest(Integer days, String password) {
    }

    public record OpenRequest(String password) {
    }
}
