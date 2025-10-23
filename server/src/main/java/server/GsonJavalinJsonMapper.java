package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.json.JsonMapper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Custom JsonMapper for Javalin to use Gson.
 * Implements all necessary methods for modern Javalin JsonMapper interface (including string and stream based fromJson and toJson).
 */
public class GsonJavalinJsonMapper implements JsonMapper {

    private final Gson gson;

    public GsonJavalinJsonMapper(Gson gson) {
        this.gson = gson;
    }

    // --- JSON Serialization (Sending Data Out) ---

    // Required by the Java interface for sending JSON back to the client.
    public String toJson(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    // CRITICAL FIX: Implement the toJsonString method, which modern Javalin versions (6.x)
    // call internally to serialize objects. This resolves the 'kotlin.NotImplementedError'.
    public String toJsonString(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    // --- JSON Deserialization (Reading Data In) ---

    // Required for reading JSON body from an InputStream (Javalin default).
    public <T> T fromJson(InputStream inputStream, Type targetType) {
        if (inputStream == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, targetType);
        } catch (Exception e) {
            // Convert to a RuntimeException to be caught by the standard exception handler
            throw new RuntimeException("Error processing input stream for JSON: " + e.getMessage(), e);
        }
    }

    // CRITICAL FIX: Implement the string-based fromJson, which Javalin often calls internally.
    public <T> T fromJsonString(String json, Type targetType) {
        try {
            return gson.fromJson(json, targetType);
        } catch (JsonSyntaxException e) {
            // Throw a runtime exception if the string is malformed
            throw new RuntimeException("Error parsing JSON string: " + e.getMessage(), e);
        }
    }
}
