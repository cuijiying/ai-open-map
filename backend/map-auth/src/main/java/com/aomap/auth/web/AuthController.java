package com.aomap.auth.web;

import com.aomap.auth.service.AuthService;
import com.aomap.auth.service.AuthService.Profile;
import com.aomap.auth.service.AuthService.Session;
import com.aomap.common.api.ApiResult;
import com.aomap.common.auth.AuthToken;
import com.aomap.common.auth.AuthTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final String jwtSecret;

    public AuthController(AuthService auth, @Value("${auth.jwt-secret}") String jwtSecret) {
        this.auth = auth;
        this.jwtSecret = jwtSecret;
    }

    @PostMapping("/register")
    public ApiResult<Session> register(@RequestBody AccountRequest request) {
        return ApiResult.ok(auth.register(request.email(), request.name(), request.password()));
    }

    @PostMapping("/login")
    public ApiResult<Session> login(@RequestBody AccountRequest request) {
        return ApiResult.ok(auth.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    public ApiResult<Profile> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthToken token = bearer(authorization);
        return ApiResult.ok(new Profile(token.userId(), token.orgId(), token.role(), token.name(), null));
    }

    private AuthToken bearer(String authorization) {
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

    public record AccountRequest(String email, String name, String password) {
    }
}
