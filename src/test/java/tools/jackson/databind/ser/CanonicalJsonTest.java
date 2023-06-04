package tools.jackson.databind.ser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test; // TODO JUnit 4 or 5 for tests?

import com.google.common.collect.Lists;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.BaseTest;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.json.JsonMapper.Builder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

class CanonicalJsonTest extends BaseTest {

    private static final ObjectMapper MAPPER = newJsonMapper();
    private static final double NEGATIVE_ZERO = -0.;

    // TODO There are several ways to make sure we really have a negative sign.
    // Double.toString(NEGATIVE_ZERO) seems to be the most simple.
    @Test
    void testSignOfNegativeZero() {
        assertEquals("-0.0", Double.toString(Math.signum(NEGATIVE_ZERO)));
    }

    @Test
    void testSignOfNegativeZero2() {
        long bits = Double.doubleToRawLongBits(NEGATIVE_ZERO);
        assertTrue(bits < 0);
    }

    @Test
    void testSignOfNegativeZero3() {
        long sign = 1L << (Double.SIZE - 1); // Highest bit represents the sign
        long bits = Double.doubleToRawLongBits(NEGATIVE_ZERO);
        assertEquals(sign, bits & sign);
    }

    @Test
    void testSignOfNegativeZero4() {
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
    void testCanonicalNegativeZeroDouble() throws Exception {
        assertSerialized("0", NEGATIVE_ZERO, newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalDecimalHandling() throws Exception {
        assertSerialized("1.01E1", new BigDecimal("10.1000"), newCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalHugeDecimalHandling() throws Exception {
        BigDecimal actual = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
        assertSerialized("1.23456789123456789123456789123456789123456789123456789123456789123456789123456789E35", actual, newCanonicalMapperBuilder());
    }

    @Test
    void testPrettyDecimalHandling() throws Exception {
        assertSerialized("10.1", new BigDecimal("10.1000"), newPrettyCanonicalMapperBuilder());
    }

    @Test
    void testPrettyHugeDecimalHandling() throws Exception {
        BigDecimal actual = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
        assertSerialized("123456789123456789123456789123456789.123456789123456789123456789123456789123456789", actual, newPrettyCanonicalMapperBuilder());
    }

    @Test
    void testCanonicalJsonSerialization() throws Exception {
        JsonNode expected = loadData("canonical-1.json");
        JsonNode actual = buildTestData();

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testCanonicalJsonSerializationRandomizedChildren() throws Exception {
        JsonNode expected = loadData("canonical-1.json");
        JsonNode actual = randomize(buildTestData());

        assertCanonicalJson(expected, actual);
    }

    @Test
    void testPrettyJsonSerialization() throws Exception {
        JsonNode expected = loadData("canonical-1.json");
        JsonNode actual = buildTestData();

        assertPrettyJson(expected, actual);
    }

    @Test
    void testPrettyJsonSerializationRandomizedChildren() throws Exception {
        JsonNode expected = loadData("canonical-1.json");
        JsonNode actual = randomize(buildTestData());

        assertPrettyJson(expected, actual);
    }

    private void assertSerialized(String expected, Object input, JsonMapper.Builder builder) {
        ObjectMapper mapper = builder.build();

        String actual = mapper.writeValueAsString(input);
        assertEquals(expected, actual);
    }

    private Builder newCanonicalMapperBuilder() {
        CanonicalBigDecimalSerializer serializer = new CanonicalBigDecimalSerializer();
        JacksonModule bigDecimalModule = new BigDecimalModule(serializer);

        JsonFactory factory = new CanonicalJsonFactory(serializer);
        return sharedConfig(jsonMapperBuilder(factory))
                .addModules(bigDecimalModule);
    }

    private Builder newPrettyCanonicalMapperBuilder() {
        PrettyBigDecimalSerializer serializer = new PrettyBigDecimalSerializer();
        JacksonModule bigDecimalModule = new BigDecimalModule(serializer);

        JsonFactory factory = new CanonicalJsonFactory(serializer);
        return sharedConfig(jsonMapperBuilder(factory)) //
                .enable(SerializationFeature.INDENT_OUTPUT) // 
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) //
                .defaultPrettyPrinter(CanonicalPrettyPrinter.INSTANCE) //
                .addModules(bigDecimalModule);
    }

    private JsonMapper.Builder sharedConfig(JsonMapper.Builder builder) {
        return builder.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
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
        ObjectMapper mapper = newCanonicalMapperBuilder().build();
        assertEquals(serialize(expected, mapper), serialize(actual, mapper));
    }

    private void assertPrettyJson(JsonNode expected, JsonNode actual) {
        ObjectMapper mapper = newPrettyCanonicalMapperBuilder().build();
        assertEquals(serialize(expected, mapper), serialize(actual, mapper));
    }

    private String serialize(JsonNode input, ObjectMapper mapper) {
        // TODO Is there a better way to sort the keys than deserializing the whole tree?
        Object obj = mapper.treeToValue(input, Object.class);
        return mapper.writeValueAsString(obj);
    }

    private JsonNode loadData(String fileName) throws IOException {
        String resource = "/data/" + fileName;
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            // TODO Formatting ok? JUnit 4 or 5 here?
            assertNotNull("Missing resource " + resource, stream);

            return MAPPER.readTree(stream);
        }
    }

    private JsonNode buildTestData() {
        return new ObjectNode(JsonNodeFactory.instance) //
                .put("-0", NEGATIVE_ZERO) //
                .put("-1", -1) //
                .put("0.1", new BigDecimal("0.100")) //
                .put("1", new BigDecimal("1")) //
                .put("10.1", new BigDecimal("10.100")) //
                .put("emoji", "\uD83D\uDE03") //
                .put("escape", "\u001B") //
                .put("lone surrogate", "\uDEAD") //
                .put("whitespace", " \t\n\r") //
                ;
    }

    public static class PrettyBigDecimalSerializer extends StdSerializer<BigDecimal>
            implements ValueToString<BigDecimal> {

        protected PrettyBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
                throws JacksonException {
            CanonicalNumberGenerator.verifyBigDecimalRange(value, provider);

            String output = convert(value);
            gen.writeNumber(output);
        }

        @Override
        public String convert(BigDecimal value) {
            return value.stripTrailingZeros().toPlainString();
        }
    }

    public static class BigDecimalModule extends SimpleModule {
        private static final long serialVersionUID = 1L;

        public BigDecimalModule(StdSerializer<BigDecimal> serializer) {
            addSerializer(BigDecimal.class, serializer);
        }
    }
}
