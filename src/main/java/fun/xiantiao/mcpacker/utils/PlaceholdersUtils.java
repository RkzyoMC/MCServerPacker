package fun.xiantiao.mcpacker.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

import static fun.xiantiao.mcpacker.utils.Tool.getValueByPath;

public class PlaceholdersUtils {

    private final JsonObject jsonObject;

    public PlaceholdersUtils(@NotNull JsonObject object) {
        if (!object.has("placeholder")) {
            throw new IllegalArgumentException("JsonObject must contain 'placeholders'");
        }
        this.jsonObject = object;
    }

    public String get(@NotNull String path) {
        JsonElement valueByPath = getValueByPath(this.jsonObject, "placeholder.data." + path);
        return resolveValue(valueByPath.getAsString());
    }

    private @NotNull String resolveValue(@NotNull String value) {
        if ("$(velocity.secret)".equals(value)) {
            return generateRandomString(16);
        }
        return value;
    }

    public String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }
}
