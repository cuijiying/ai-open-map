package com.aomap.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRulesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rejectsBlankNameAndNonObjectState() {
        assertThrows(IllegalArgumentException.class, () -> ProjectRules.name("  "));
        assertThrows(IllegalArgumentException.class, () -> ProjectRules.state(mapper, mapper.createArrayNode()));
    }

    @Test
    void keepsObjectStateAndLimitsShareLifetime() throws Exception {
        ObjectNode state = mapper.createObjectNode().put("zoom", 4);
        assertTrue(ProjectRules.state(mapper, state).contains("\"zoom\":4"));
        OffsetDateTime expiry = ProjectRules.expiry(30);
        assertTrue(Duration.between(OffsetDateTime.now(), expiry).toDays() >= 29);
        assertThrows(IllegalArgumentException.class, () -> ProjectRules.expiry(31));
    }

    @Test
    void viewersCannotWriteAndOnlyOwnerOrAuthorCanDelete() {
        assertThrows(IllegalArgumentException.class, () -> ProjectRules.requireWriter("VIEWER"));
        ProjectRules.requireWriter("EDITOR");
        ProjectRules.requireDelete("OWNER", 2, 9);
        ProjectRules.requireDelete("EDITOR", 9, 9);
        assertThrows(IllegalArgumentException.class, () -> ProjectRules.requireDelete("EDITOR", 2, 9));
    }

    @Test
    void tokensAreUrlSafe() {
        String token = ProjectRules.token();
        assertEquals(24, token.length());
        assertTrue(token.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-' || ch == '_'));
    }
}
