package com.boxing.bracket.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDataSerializerTest {

    private final AuditDataSerializer serializer = new AuditDataSerializer(new ObjectMapper());

    @Test
    void masksSensitiveFieldsAtEveryDepth() {
        String serialized = serializer.serialize(Map.of(
                "password", "secret-password",
                "profile", Map.of("accessToken", "secret-token"),
                "name", "Manager"
        ));

        assertThat(serialized).contains("\"password\":\"***\"");
        assertThat(serialized).contains("\"accessToken\":\"***\"");
        assertThat(serialized).contains("\"name\":\"Manager\"");
        assertThat(serialized).doesNotContain("secret-password", "secret-token");
    }

    @Test
    void excludesRawStringArgumentsSuchAsAuthorizationHeader() {
        String serialized = serializer.serializeRequestArguments(new Object[]{
                "Bearer private-session-token",
                10L,
                Map.of("loginId", "manager01", "password", "secret-password")
        });

        assertThat(serialized).contains("manager01", "\"password\":\"***\"");
        assertThat(serialized).doesNotContain("private-session-token", "secret-password");
    }
}
