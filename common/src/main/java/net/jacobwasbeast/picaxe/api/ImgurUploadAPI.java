package net.jacobwasbeast.picaxe.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ImgurUploadAPI {
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";
    private static final String CLIENT_ID = "546c25a59c58ad7"; // Anonymous upload client ID
    private static final long MAX_STATIC_IMAGE_BYTES = 10L * 1024L * 1024L; // 10 MB
    private static final long MAX_GIF_IMAGE_BYTES = 20L * 1024L * 1024L; // 20 MB

    /* --------------------------
       PUBLIC API
       -------------------------- */

    /** Async: open native file dialog, then upload, then callback with URL (or null). */
    public static void promptAndUploadImageAsync(Consumer<String> callback) {
        // 1) Ask the OS for a file path on the MC client thread
        promptForImagePathAsync().thenCompose(imagePath -> {
            if (imagePath == null || imagePath.isBlank()) return CompletableFuture.completedFuture(null);
            // 2) Upload off the render thread
            return CompletableFuture.supplyAsync(() -> uploadImage(imagePath));
        }).whenComplete((url, err) -> {
            if (err != null) err.printStackTrace();
            if (callback != null) callback.accept(url);
        });
    }

    /** Sync helper (not recommended on the render thread). */
    public static String promptAndUploadImage() {
        try {
            String path = promptForImagePathAsync().get(); // will not block the MC thread because we schedule dialog on it
            return (path == null || path.isBlank()) ? null : uploadImage(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Upload a known local image path to Imgur; returns direct link or null. */
    public static String uploadImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            System.err.println("Image path is null or empty");
            return null;
        }

        try {
            Path path = Paths.get(imagePath.trim());
            if (!Files.exists(path)) {
                System.err.println("Image file not found: " + imagePath);
                return null;
            }

            String fileName = path.getFileName().toString().toLowerCase();
            if (!isValidImageFile(fileName)) {
                System.err.println("Invalid image file type: " + fileName);
                return null;
            }

            long fileSize = Files.size(path);
            long maxSize = maxAllowedBytes(fileName);
            if (fileSize > maxSize) {
                System.err.println("Image file too large: " + fileSize + " bytes (max " + (maxSize / (1024 * 1024)) + "MB)");
                return null;
            }

            HttpURLConnection conn = openMultipartUploadConnection();
            String boundary = "----PicAxeBoundary" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("User-Agent", "PicAxe-Mod/1.1.2");

            writeMultipartBody(conn, boundary, path, fileName);

            int responseCode = conn.getResponseCode();
            String response = readResponseBody(conn, responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                String imageUrl = parseImgurUrlFromResponse(response, fileName);
                if (imageUrl != null) {
                    System.out.println("Uploaded to Imgur: " + imageUrl);
                    return imageUrl;
                }
            } else {
                System.err.println("HTTP error " + responseCode + ": " + response);
            }
        } catch (Exception e) {
            System.err.println("Failed to upload image to Imgur: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /* --------------------------
       NATIVE FILE DIALOG (LWJGL TinyFD)
       -------------------------- */

    /**
     * Opens a native OS file dialog on the Minecraft client thread and completes with the selected path or null.
     * Uses LWJGL Tiny File Dialogs (no AWT/Swing, works in full-screen).
     */
    public static CompletableFuture<String> promptForImagePathAsync() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String[] exts = { "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp" };

                // Allocate a PointerBuffer for the extension strings
                PointerBuffer filterPatterns = stack.mallocPointer(exts.length);
                for (String ext : exts) {
                    filterPatterns.put(stack.UTF8(ext));
                }
                filterPatterns.flip();

                String defaultPath = guessPicturesDir();
                if (defaultPath == null) defaultPath = System.getProperty("user.home");

                String selected = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select Image to Upload",
                        defaultPath,
                        filterPatterns,
                        "Image files",
                        false
                );

                future.complete((selected == null || selected.isBlank()) ? null : selected);
            } catch (Throwable t) {
                t.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    /* --------------------------
       HELPERS
       -------------------------- */

    private static boolean isValidImageFile(String fileName) {
        String f = fileName.toLowerCase();
        return f.endsWith(".png") || f.endsWith(".jpg") || f.endsWith(".jpeg")
                || f.endsWith(".gif") || f.endsWith(".bmp") || f.endsWith(".webp");
    }

    private static long maxAllowedBytes(String fileName) {
        return fileName.endsWith(".gif") ? MAX_GIF_IMAGE_BYTES : MAX_STATIC_IMAGE_BYTES;
    }

    private static HttpURLConnection openMultipartUploadConnection() throws IOException {
        URL url = new URL(IMGUR_UPLOAD_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Client-ID " + CLIENT_ID);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        return conn;
    }

    private static void writeMultipartBody(HttpURLConnection conn, String boundary, Path path, String fileName) throws IOException {
        byte[] lineBreak = "\r\n".getBytes("UTF-8");
        try (OutputStream output = conn.getOutputStream()) {
            output.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            output.write("Content-Disposition: form-data; name=\"type\"\r\n\r\n".getBytes("UTF-8"));
            output.write("file".getBytes("UTF-8"));
            output.write(lineBreak);

            output.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            output.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n").getBytes("UTF-8"));
            output.write("Content-Type: application/octet-stream\r\n\r\n".getBytes("UTF-8"));

            Files.copy(path, output);
            output.write(lineBreak);
            output.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
            output.flush();
        }
    }

    private static String readResponseBody(HttpURLConnection conn, int responseCode) throws IOException {
        InputStream stream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private static String parseImgurUrlFromResponse(String responseBody, String originalFileName) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!jsonResponse.has("success") || !jsonResponse.get("success").getAsBoolean()) {
                System.err.println("Imgur API returned success=false: " + responseBody);
                return null;
            }

            JsonObject data = jsonResponse.getAsJsonObject("data");
            if (data == null || !data.has("link")) {
                System.err.println("Imgur API response missing data.link: " + responseBody);
                return null;
            }

            String imageUrl = data.get("link").getAsString();
            if (originalFileName.endsWith(".gif")) {
                return normalizeAnimatedImgurLink(data, imageUrl);
            }
            return imageUrl;
        } catch (Exception jsonEx) {
            System.err.println("Failed to parse Imgur response: " + jsonEx.getMessage());
            System.err.println("Response was: " + responseBody);
            return null;
        }
    }

    private static String normalizeAnimatedImgurLink(JsonObject data, String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.endsWith(".gifv")) {
            return imageUrl.substring(0, imageUrl.length() - 5) + ".gif";
        }
        if (lower.endsWith(".mp4") && data.has("id")) {
            return "https://i.imgur.com/" + data.get("id").getAsString() + ".gif";
        }
        return imageUrl;
    }

    private static String guessPicturesDir() {
        try {
            String home = System.getProperty("user.home");
            if (home == null) return null;
            File pics = new File(home, "Pictures");
            return pics.isDirectory() ? pics.getAbsolutePath() : home;
        } catch (Exception ignored) {
            return null;
        }
    }
}
