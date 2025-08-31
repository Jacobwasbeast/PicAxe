package net.jacobwasbeast.picaxe.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ImgurUploadAPI {
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";
    private static final String CLIENT_ID = "546c25a59c58ad7"; // Anonymous upload client ID

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

            long fileSize = Files.size(path);
            if (fileSize > 10 * 1024 * 1024) { // 10 MB Imgur anonymous limit
                System.err.println("Image file too large: " + fileSize + " bytes (max 10MB)");
                return null;
            }

            String fileName = path.getFileName().toString().toLowerCase();
            if (!isValidImageFile(fileName)) {
                System.err.println("Invalid image file type: " + fileName);
                return null;
            }

            byte[] imageBytes = Files.readAllBytes(path);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            URL url = new URL(IMGUR_UPLOAD_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Client-ID " + CLIENT_ID);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "PicAxe-Mod/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            String postData = "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            if (responseCode >= 200 && responseCode < 300) {
                try {
                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        JsonObject data = jsonResponse.getAsJsonObject("data");
                        String imageUrl = data.get("link").getAsString();
                        System.out.println("Uploaded to Imgur: " + imageUrl);
                        return imageUrl;
                    } else {
                        System.err.println("Imgur API returned success=false: " + response);
                    }
                } catch (Exception jsonEx) {
                    System.err.println("Failed to parse Imgur response: " + jsonEx.getMessage());
                    System.err.println("Response was: " + response);
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
