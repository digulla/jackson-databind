package com.fasterxml.jackson.databind.ser;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

class CanonicalJsonTest {

    private static final BigDecimal TEN_POINT_1_WITH_TRAILING_ZEROES = new BigDecimal("10.1000");
    private static final BigDecimal VERY_BIG_DECIMAL = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
    private static final double NEGATIVE_ZERO = -0.;
    private static final JsonAssert JSON_ASSERT = new JsonAssert();
    private static final JsonTestResource CANONICAL_1 = new JsonTestResource("/data/canonical-1.json");

    @Test
    void testSignOfNegativeZero() {
        assertEquals("-0.0", Double.toString(NEGATIVE_ZERO));
    }

    @Test
    void testNegativeZeroIsEqualToZero() {
        assertEquals(0.0, NEGATIVE_ZERO, 1e-9);
    }

    @Test
    void testCanonicalBigDecimalSerializationTrailingZeros() throws Exception {
        assertSerialized("1", new BigDecimal("1.0000"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroBigDecimal() throws Exception {
        assertSerialized("0", new BigDecimal("-0"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroBigDecimal2() throws Exception {
        assertSerialized("0", new BigDecimal(NEGATIVE_ZERO), newCanonicalMapperBuilder());
    }
    
    @Test
    void testCanonicalNegativeZeroBigDecimal3() throws Exception {
        assertSerialized("0", BigDecimal.valueOf(NEGATIVE_ZERO), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalNegativeZeroDouble() throws Exception {
        assertSerialized("0", NEGATIVE_ZERO, newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalDecimalHandling() throws Exception {
        assertSerialized("1.01E1", TEN_POINT_1_WITH_TRAILING_ZEROES, newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalHugeDecimalHandling() throws Exception {
        assertSerialized("1.23456789123456789123456789123456789123456789123456789123456789123456789123456789E35", VERY_BIG_DECIMAL, newCanonicalMapperBuilder());
    }

    @Test
    void testPrettyDecimalHandling() throws Exception {
        JSON_ASSERT.assertSerialized("10.1", TEN_POINT_1_WITH_TRAILING_ZEROES);
    }

    @Test
    void testPrettyHugeDecimalHandling() throws Exception {
        JSON_ASSERT.assertSerialized("123456789123456789123456789123456789.123456789123456789123456789123456789123456789", VERY_BIG_DECIMAL);
    }

    @Test
    void testCanonicalJsonSerialization() throws Exception {
        JsonNode expected = JSON_ASSERT.loadResource(CANONICAL_1);
        JsonNode actual = buildTestData();

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testCanonicalJsonSerializationRandomizedChildren() throws Exception {
        JsonNode expected = JSON_ASSERT.loadResource(CANONICAL_1);
        JsonNode actual = randomize(buildTestData());

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testPrettyJsonSerialization() throws Exception {
        JsonNode actual = buildTestData();

        JSON_ASSERT.assertJson(CANONICAL_1, actual);
    }

    @Test
    void testPrettyJsonSerializationRandomizedChildren() throws Exception {
        JsonNode actual = randomize(buildTestData());

        JSON_ASSERT.assertJson(CANONICAL_1, actual);
    }

    private void assertSerialized(String expected, Object input, JsonMapper mapper) {
        String actual;
        try {
            actual = mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to serialize " + input, e);
        }
        assertEquals(expected, actual);
    }

    private JsonMapper newCanonicalMapperBuilder() {
        return CanonicalJsonMapper.builder().build();
    }
    
    private JsonNode randomize(JsonNode input) {
        if (input instanceof ObjectNode) {
            List<Map.Entry<String, JsonNode>> copy = Lists.newArrayList(input.fields());
            Collections.shuffle(copy);

            Map<String, JsonNode> randomized = new LinkedHashMap<>();
            copy.forEach(entry -> {
                randomized.put(entry.getKey(), randomize(entry.getValue()));
            });

            return new ObjectNode(JsonNodeFactory.instance, randomized);
        } else {
            return input;
        }
    }

    private void assertCanonicalJson(JsonNode expected, JsonNode actual) {
        ObjectMapper mapper = newCanonicalMapperBuilder();
        assertEquals(serialize(expected, mapper), serialize(actual, mapper));
    }

    private String serialize(JsonNode input, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(input);
        } catch (JacksonException e) {
            throw new AssertionError("Unable to serialize " + input, e);
        }
    }

    private JsonNode buildTestData() {
        return new ObjectNode(JsonNodeFactory.instance) //
                .put("-0", NEGATIVE_ZERO) //
                .put("-1", -1) //
                .put("0.1", new BigDecimal("0.100")) //
                .put("1", new BigDecimal("1")) //
                .put("10.1", TEN_POINT_1_WITH_TRAILING_ZEROES) //
                .put("emoji", "\uD83D\uDE03") //
                .put("escape", "\u001B") //
                .put("lone surrogate", "\uDEAD") //
                .put("whitespace", " \t\n\r") //
                ;
    }
}
