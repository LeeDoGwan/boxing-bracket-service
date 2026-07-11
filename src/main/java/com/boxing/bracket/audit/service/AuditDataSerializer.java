package com.boxing.bracket.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AuditDataSerializer {

    private static final String REDACTED = "***";

    private final ObjectMapper objectMapper;

    public AuditDataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(toNode(value));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return "{\"unavailable\":true}";
        }
    }

    public String serializeRequestArguments(Object[] arguments) {
        JsonNode requestNode = toRequestNode(arguments);
        if (requestNode == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(requestNode);
        } catch (JsonProcessingException exception) {
            return "{\"unavailable\":true}";
        }
    }

    public JsonNode toRequestNode(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return null;
        }

        List<Object> requestBodies = new ArrayList<>();
        for (Object argument : arguments) {
            if (argument != null && !(argument instanceof String) && !(argument instanceof Number)) {
                requestBodies.add(normalize(argument));
            }
        }
        return requestBodies.isEmpty() ? null : toNode(requestBodies);
    }

    public JsonNode toNode(Object value) {
        JsonNode node = objectMapper.valueToTree(normalize(value));
        redact(node);
        return node;
    }

    public Long findLong(JsonNode node, String fieldName) {
        JsonNode value = find(node, fieldName);
        return value != null && value.canConvertToLong() ? value.longValue() : null;
    }

    public String findText(JsonNode node, String fieldName) {
        JsonNode value = find(node, fieldName);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private Object normalize(Object value) {
        if (value instanceof MultipartFile) {
            MultipartFile file = (MultipartFile) value;
            return Map.of("filename", file.getOriginalFilename(), "size", file.getSize());
        }
        if (value instanceof Object[]) {
            List<Object> values = new ArrayList<>();
            for (Object item : Arrays.asList((Object[]) value)) {
                values.add(normalize(item));
            }
            return values;
        }
        return value;
    }

    private JsonNode find(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(fieldName);
            if (direct != null) {
                return direct;
            }
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                JsonNode found = find(children.next(), fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = find(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void redact(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, REDACTED);
                } else {
                    redact(objectNode.get(fieldName));
                }
            }
        }
        if (node instanceof ArrayNode) {
            for (JsonNode child : node) {
                redact(child);
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("session");
    }
}
