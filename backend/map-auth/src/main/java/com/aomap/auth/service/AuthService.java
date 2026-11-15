package com.aomap.auth.service;

import com.aomap.auth.domain.OrgRow;
import com.aomap.auth.domain.UserRow;
import com.aomap.auth.mapper.AuthMapper;
import com.aomap.common.auth.AuthToken;
import com.aomap.common.auth.AuthTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthMapper mapper;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String jwtSecret;

    public AuthService(AuthMapper mapper, @Value("${auth.jwt-secret}") String jwtSecret) {
        this.mapper = mapper;
        this.jwtSecret = jwtSecret;
    }

    @Transactional
    public Session register(String email, String name, String password) {
        String normalized = normalizeEmail(email);
        checkPassword(password);
        String display = name == null ? "" : name.trim();
        if (display.isBlank() || display.length() > 64) {
            throw new IllegalArgumentException("请填写不超过 64 字的名称");
        }
        UserRow user = new UserRow();
        user.setEmail(normalized);
        user.setName(display);
        user.setPasswordHash(passwords.encode(password));
        try {
            mapper.insertUser(user);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        OrgRow org = new OrgRow();
        org.setName(display + "的空间");
        mapper.insertOrg(org);
        mapper.insertMember(org.getId(), user.getId(), "OWNER");
        return session(user.getId(), org.getId(), "OWNER", display, normalized);
    }

    public Session login(String email, String password) {
        UserRow user = mapper.findByEmail(normalizeEmail(email));
        if (user == null || password == null || !passwords.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("邮箱或密码不正确");
        }
        Long orgId = null;
        String role = null;
        var member = mapper.findMembership(user.getId());
        if (member != null) {
            orgId = member.getOrgId();
            role = member.getRole();
        }
        if (orgId == null || role == null) {
            throw new IllegalStateException("账号还没有加入组织");
        }
        return session(user.getId(), orgId, role, user.getName(), user.getEmail());
    }

    public static String normalizeEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1 || value.contains(" ")) {
            throw new IllegalArgumentException("请填写有效邮箱");
        }
        return value;
    }

    public static void checkPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("密码需要 8 到 72 位");
        }
    }

    private Session session(long userId, long orgId, String role, String name, String email) {
        String token = AuthTokens.sign(jwtSecret, new AuthToken(userId, orgId, role, name));
        return new Session(token, new Profile(userId, orgId, role, name, email));
    }

    public record Session(String token, Profile profile) {
    }

    public record Profile(long userId, long orgId, String role, String name, String email) {
    }
}
