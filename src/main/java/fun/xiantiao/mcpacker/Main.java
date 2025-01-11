package fun.xiantiao.mcpacker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.xiantiao.mcpacker.enums.PlaceholderType;
import fun.xiantiao.mcpacker.records.Placeholder;
import fun.xiantiao.mcpacker.utils.PlaceholdersUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static fun.xiantiao.mcpacker.utils.Tool.*;
import static fun.xiantiao.mcpacker.utils.Tool.copyDirectory;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    private static final Path PATH_DEFAULT = getDataFolder().resolve("default");
    private static final Path PATH_DEFAULT_FILES = PATH_DEFAULT.resolve("files");
    private static final Path PATH_DEFAULT_SERVERS = PATH_DEFAULT.resolve("servers");

    private static final Path PATH_BUILT = getDataFolder().resolve("built");
    private static final Path PATH_BUILT_FILES = PATH_BUILT.resolve("files");
    private static final Path PATH_BUILT_SERVERS = PATH_BUILT.resolve("servers");

    private static final Path PATH_BACKUP = getDataFolder().resolve("backup");
    private static final Path PATH_BACKUP_DEFAULT = PATH_BACKUP.resolve("default");
    private static final Path PATH_BACKUP_BUILT = PATH_BACKUP.resolve("built");

    public static void main(String[] args) throws IOException {
        logger.info("Starting...");

        initDirectories();
        extractResourceFile(Main.class, "/mcp.build.setting.json", getDataFolder().toString());

        JsonObject settings = loadSettings();
        PlaceholdersUtils placeholdersUtils = new PlaceholdersUtils(settings);

        //backup
        compressFolder(PATH_DEFAULT, PATH_BACKUP_DEFAULT.resolve(getZipFileName()));
        compressFolder(PATH_BUILT, PATH_BACKUP_BUILT.resolve(getZipFileName()));

        // 清空built
        deleteFolder(PATH_BUILT);
        // 复制到built
        copyDirectory(PATH_DEFAULT, PATH_BUILT);

        List<String> suffixes = new ArrayList<>();
        for (JsonElement element : Objects.requireNonNull(getValueByPath(settings, "placeholder.suffixes")).getAsJsonArray()) {
            suffixes.add(element.getAsString());
        }

        // placeholder
        {
            getSubfolders(PATH_BUILT, true, suffixes).forEach(path -> {
                logger.info("placeholder {}", path);
                try {
                    String body = readFileToString(path); // 文件内容
                    String newBody = body;                // 新文件内容
                    {
                        Set<Placeholder> strings = extractPlaceholderValues(body, PlaceholderType.Text); // body内的papi
                        for (Placeholder placeholder : strings) {
                            String papied = placeholdersUtils.get(placeholder.text()); // 获取值
                            newBody = newBody.replaceAll("\\$\\(mcp\\."+placeholder.text()+"\\)", papied);
                            writeFileOverwrite(path, newBody);
                        }
                    }

                    {
                        Set<Placeholder> strings = extractPlaceholderValues(body, PlaceholderType.Number); // body内的papi
                        for (Placeholder placeholder : strings) {
                            String papied = placeholdersUtils.get(placeholder.text()); // 获取值
                            newBody = newBody.replaceAll("\"\\$\\(mcp\\."+placeholder.text()+"\\)\\(number\\)\"", papied);
                            writeFileOverwrite(path, newBody);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        logger.info("Stopped.");
    }

    private static void initDirectories() {
        createDirectory(PATH_DEFAULT_FILES);
        createDirectory(PATH_DEFAULT_SERVERS);

        createDirectory(PATH_BUILT_FILES);
        createDirectory(PATH_BUILT_SERVERS);

        createDirectory(PATH_BACKUP_DEFAULT);
        createDirectory(PATH_BACKUP_BUILT);
    }

    private static @NotNull JsonObject loadSettings() throws IOException {
        copyDirectory( getDataFolder().resolve("mcp.build.setting.json"), getDataFolder().resolve(".mcp.build.setting.json"));

        Path tmpSettingsPath = getDataFolder().resolve(".mcp.build.setting.json");
        BufferedReader reader = Files.newBufferedReader(tmpSettingsPath);
        JsonObject asJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        PlaceholdersUtils placeholdersUtils = new PlaceholdersUtils(asJsonObject); // 获取源papi
        String body = readFileToString(tmpSettingsPath); // 文件内容
        String newBody = body;                // 新文件内容

        {
            Set<Placeholder> strings = extractPlaceholderValues(body, PlaceholderType.Text); // body内的papi
            for (Placeholder placeholder : strings) {
                String papied = placeholdersUtils.get(placeholder.text()); // 获取值
                newBody = newBody.replaceAll("\\$\\(mcp\\."+placeholder.text()+"\\)", papied);
                writeFileOverwrite(tmpSettingsPath, newBody);
            }
        }

        {
            Set<Placeholder> strings = extractPlaceholderValues(body, PlaceholderType.Number); // body内的papi
            for (Placeholder placeholder : strings) {
                String papied = placeholdersUtils.get(placeholder.text()); // 获取值
                newBody = newBody.replaceAll("\"\\$\\(mcp\\."+placeholder.text()+"\\)\\(number\\)\"", papied);
                writeFileOverwrite(tmpSettingsPath, newBody);
            }
        }


        BufferedReader reader2 = Files.newBufferedReader(tmpSettingsPath);
        return JsonParser.parseReader(reader2).getAsJsonObject();
    }

    private static @NotNull Path getDataFolder() {
        try {
            URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
            Path jarPath = Paths.get(url.toURI()).getParent();
            return jarPath != null ? jarPath : Paths.get("");
        } catch (Exception e) {
            logger.error("Error retrieving data folder.", e);
            throw new RuntimeException(e);
        }
    }

    private static @NotNull String getZipFileName() {
        return getStringTime()+".zip";
    }

    public static @NotNull Logger getLogger() {
        return logger;
    }
}
