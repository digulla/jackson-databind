package com.fasterxml.jackson.databind.ser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JsonAssert {

    private JsonMapper _mapper; // TODO If we don't want to allow people to configure this, then everything in here can be static which would make it look a lot like JUnit Assertions.

    public JsonAssert() {
        _mapper = CanonicalJsonMapper.builder() //
                .prettyPrint() // Always pretty print to make the diff in failed tests easier to use
                .build();
    }

    public void assertJson(JsonTestResource expected, Object actual) {
        assertJson(loadResource(expected), actual);
    }

    public void assertJson(JsonNode expected, Object actual) {
        assertEquals(serialize(expected), serialize(actual));
    }

    public void assertSerialized(String expectedJson, Object actual) {
        assertEquals(expectedJson, serialize(actual));
    }

    public String serialize(JsonNode input) {
        try {
            return _mapper.writeValueAsString(input);
        } catch(JacksonException e) {
            throw new AssertionError("Serializing failed: " + input, e);
        }
    }

    public String serialize(Object data) {
        if (data instanceof JsonNode) {
            return serialize((JsonNode)data);
        }

        // TODO Sorting mostly works except for properties defined by @JsonTypeInfo
        try {
            return _mapper.writeValueAsString(data);
        } catch(JacksonException e) {
            throw new AssertionError("Serializing failed: " + data, e);
        }
    }

    public JsonNode loadResource(JsonTestResource resource) {
        try (InputStream stream = resource.getInputStream()) {
            // TODO Formatting ok? JUnit 4 or 5 here?
            assertNotNull("Missing resource " + resource, stream);

            return _mapper.readTree(stream);
        } catch (IOException e) {
            throw new AssertionError("Error loading " + resource, e);
        }
    }
}
