package fun.xiantiao.mcpacker.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static fun.xiantiao.mcpacker.Main.getLogger;

public class Tool {

    private static final Logger logger = getLogger();

    public static void extractResourceFile(Class clazz, String resourcePath, String extractPath) {
        Path targetPath = new File(extractPath).toPath().resolve(resourcePath.substring(1));

        if (Files.exists(targetPath)) {
            logger.info("{} file already exists.", resourcePath);
            return;
        }

        try (InputStream inputStream = clazz.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.error("Resource file not found: {}", resourcePath);
                return;
            }

            Files.copy(inputStream, targetPath);
            logger.info("Resource file extracted: {}", resourcePath);
        } catch (IOException e) {
            logger.error("Error extracting resource file: {}", resourcePath, e);
            throw new RuntimeException(e);
        }
    }

    public static void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
            logger.info("Directory created: [{}]", path);
        } catch (IOException e) {
            logger.error("Failed to create directory: [{}]", path, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除指定路径的文件夹及其所有内容
     *
     * @param folderPath 要删除的文件夹路径
     * @throws IOException 如果删除失败
     */
    public static void deleteFolder(Path folderPath) throws IOException {
        if (Files.notExists(folderPath)) {
            System.out.println("Folder does not exist: " + folderPath);
            return;
        }

        // 使用 Files.walk 遍历文件夹，深度优先删除内容
        Files.walk(folderPath)
                .sorted(Comparator.reverseOrder()) // 先删除子目录或文件，再删除父目录
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Deleted: " + path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete: " + path, e);
                    }
                });
    }

    /**
     * 递归复制文件夹和文件
     *
     * @param source      源文件夹或文件路径
     * @param destination 目标文件夹路径
     * @throws IOException 如果发生 I/O 错误
     */
    public static void copyDirectory(Path source, Path destination) throws IOException {
        File sourceFile = source.toFile();
        File destinationFile = destination.toFile();

        // 如果源是文件，直接复制文件
        if (sourceFile.isFile()) {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // 如果源是文件夹，创建目标文件夹
        if (!destinationFile.exists() && !destinationFile.mkdirs()) {
            throw new IOException("Failed to create directory: " + destinationFile);
        }

        // 遍历源文件夹中的所有文件和子文件夹
        File[] files = sourceFile.listFiles();
        if (files == null) return; // 空文件夹

        for (File file : files) {
            Path subSource = file.toPath();
            Path subDestination = destination.resolve(file.getName());

            // 递归处理子文件或文件夹
            copyDirectory(subSource, subDestination);
        }
    }

    /**
     * Retrieves a value from the JSON object using a dot-separated path.
     *
     * @param path Dot-separated path (e.g., "key.subkey")
     * @return The value corresponding to the path
     */
    public static @NotNull JsonElement getValueByPath(JsonObject current, @NotNull String path) {
        String[] keys = (path).split("\\.");

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            JsonElement element = current.get(key);

            if (element == null) {
                logger.error("Failed to retrieve path [{}]: not found in current.", path);
                throw new IllegalArgumentException("Invalid path: " + path);
            }

            if (i == keys.length - 1) { // Last key in path
                return element;
            }

            if (!element.isJsonObject()) {
                logger.error("Invalid structure for path [{}]: expected an object at key [{}]", path, key);
                throw new IllegalArgumentException("Invalid structure: " + path);
            }

            current = element.getAsJsonObject();
        }
        logger.error("getValueByPath() error [{}, {}]", current, path);
        throw new RuntimeException();
    }

    /**
     * 获取指定路径下的所有子文件夹，并根据后缀过滤
     *
     * @param folderPath 目标文件夹路径
     * @param recursive  是否递归获取子文件夹
     * @param suffixes   仅返回名称以这些后缀结尾的文件夹
     * @return 符合条件的子文件夹列表
     * @throws IOException 如果读取目录失败
     */
    public static List<Path> getSubfolders(Path folderPath, boolean recursive, List<String> suffixes) throws IOException {
        List<Path> subfolders = new ArrayList<>();

        if (!Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("The provided path is not a directory: " + folderPath);
        }

        if (recursive) {
            // 使用 Files.walk 遍历文件夹，深度优先
            Files.walk(folderPath)
                    .filter(path -> !path.toFile().isDirectory())
                    .filter(path -> !path.equals(folderPath)) // 排除自身
                    .filter(path -> matchesSuffix(path.getFileName().toString(), suffixes)) // 按后缀过滤
                    .forEach(subfolders::add);
        } else {
            // 使用 Files.newDirectoryStream 获取直接子文件夹
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) && matchesSuffix(path.getFileName().toString(), suffixes)) {
                        subfolders.add(path);
                    }
                }
            }
        }

        return subfolders;
    }
    /**
     * 检查文件夹名称是否以指定的后缀结尾
     *
     * @param folderName 文件夹名称
     * @param suffixes   后缀列表
     * @return 如果名称以任意一个后缀结尾，则返回 true
     */
    private static boolean matchesSuffix(String folderName, List<String> suffixes) {
        if (suffixes == null || suffixes.isEmpty()) {
            return true; // 如果没有指定后缀，默认匹配所有
        }

        for (String suffix : suffixes) {
            if (folderName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取文件内容为 String
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 如果文件读取失败
     */
    public static String readFileToString(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        return Files.readString(filePath);
    }
    /**
     * 将文本内容全覆盖写入指定文件
     *
     * @param filePath 文件路径
     * @param content  要写入的内容
     * @throws IOException 如果写入失败
     */
    public static void writeFileOverwrite(Path filePath, String content) throws IOException {
        // 使用 try-with-resources 自动关闭资源
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(content);
        }
    }


    /**
     * 从文本中提取以 $(mcp. 开头并以 ) 结尾的中间值
     *
     * @param text 输入文本
     * @return 匹配的中间值列表
     */
    public static Set<String> extractPlaceholderValues(String text) {
        Set<String> values = new HashSet<>();
        // 正则表达式匹配 $(mcp.开头和)结尾，中间内容捕获
        String regex = "\\$\\(mcp\\.([^)]+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // 查找所有匹配项并提取中间值
        while (matcher.find()) {
            values.add(matcher.group(1)); // 获取捕获组中的值
        }

        return values;
    }

    /**
     * 压缩整个文件夹
     * @param sourceFolder 文件夹
     * @param zipFileName 输出位置
     * @throws IOException 失败
     */
    public static void compressFolder(Path sourceFolder, Path zipFileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFileName.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File folder = new File(sourceFolder.toUri());
            addFolderToZip(folder, folder.getName(), zos);
        }
    }
    // 递归添加文件夹内容到Zip
    private static void addFolderToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentFolder + "/" + file.getName(), zos);
            } else {
                addFileToZip(file, parentFolder + "/" + file.getName(), zos);
            }
        }
    }
    // 添加单个文件到Zip
    private static void addFileToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }

    public static @NotNull String getStringTime() {
        // 获取当前时间
        Date now = new Date();
        // 格式化时间：yyyy-MM-dd HH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        return sdf.format(now);
    }
}
