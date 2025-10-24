package web;

import com.google.gson.Gson;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * Custom JSON Mapper for Javalin to correctly use Gson.
 */
public class GsonJavalinJsonMapper implements JsonMapper {
    private final Gson gson;

    public GsonJavalinJsonMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    public @NotNull String toJsonString(@NotNull Object obj, @NotNull Type type) {
        return gson.toJson(obj, type);
    }

    @Override
    public @NotNull <T> T fromJsonStream(@NotNull InputStream jsonStream, @NotNull Type targetType) {
        return gson.fromJson(new InputStreamReader(jsonStream), targetType);
    }
}
