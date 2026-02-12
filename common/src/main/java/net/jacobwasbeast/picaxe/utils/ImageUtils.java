package net.jacobwasbeast.picaxe.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.jacobwasbeast.picaxe.api.ImageFrameAlignment;
import net.jacobwasbeast.picaxe.api.RotationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_LOAD_TRIES = 3;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);
    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(128),
                    r -> {
                        Thread t = new Thread(r, "ImageUtils-IO-" + THREAD_ID.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.DiscardOldestPolicy()
            );

    private static final Set<String> blacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, SoftReference<byte[]>> rawDataCache = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedTextures = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedTextures = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedSideTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeLeft = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideDrapeLeft = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeRight = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideDrapeRight = new ConcurrentHashMap<>();
    private static final Set<String> pendingSideTransforms = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<String, ResourceLocation> cachedFrontBackTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedFrontBackTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedBackDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedBackDrape = new ConcurrentHashMap<>();
    private static final Set<String> pendingFrontBackTransforms = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<String, ResourceLocation> cachedSingleFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSingleFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSingleTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSingleTopper = new ConcurrentHashMap<>();
    private static final Set<String> pendingSingleFrontTransforms = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<String, ResourceLocation> cachedAllSidesToppers = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedAllSidesToppers = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedAllSidesLeft = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedAllSidesLeft = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedAllSidesRight = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedAllSidesRight = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedAllSidesFront = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedAllSidesFront = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedAllSidesBack = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedAllSidesBack = new ConcurrentHashMap<>();
    private static final Set<String> pendingAllSidesTransforms = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final ResourceLocation NOT_FOUND_TEXTURE;
    private static final ResourceLocation LOADING_TEXTURE;

    private record AnimatedTexture(ResourceLocation[] frames, int[] delays, int totalDuration) {
        public ResourceLocation getCurrentFrame() {
            if (frames == null || frames.length == 0) {
                return NOT_FOUND_TEXTURE;
            }
            if (frames.length <= 1) {
                return frames[0];
            }
            if (totalDuration == 0) return frames[0];
            long timeInAnimation = System.currentTimeMillis() % totalDuration;
            int accumulatedDelay = 0;
            for (int i = 0; i < delays.length; i++) {
                accumulatedDelay += delays[i];
                if (timeInAnimation < accumulatedDelay) {
                    return frames[i];
                }
            }
            return frames[frames.length - 1];
        }
    }

    private static final class DecodedGif {
        final BufferedImage[] frames;
        final int[] delays;
        final int totalDuration;
        DecodedGif(BufferedImage[] frames, int[] delays, int totalDuration) {
            this.frames = frames;
            this.delays = delays;
            this.totalDuration = totalDuration;
        }
    }

    static {
        BufferedImage notFoundImage = loadBufferedImageFromResource("picaxe", "textures/blocks/notfound.png");
        BufferedImage loadingImage = loadBufferedImageFromResource("picaxe", "textures/blocks/loadingimage.png");

        BufferedImage combinedLoadingImage = new BufferedImage(
                notFoundImage.getWidth() + loadingImage.getWidth(),
                Math.max(notFoundImage.getHeight(), loadingImage.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinedLoadingImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(loadingImage, 0, 0, null);
        g.drawImage(notFoundImage, loadingImage.getWidth(), 0, null);
        g.dispose();

        NOT_FOUND_TEXTURE = registerTextureFromImage("internal:notfound", notFoundImage, true);
        LOADING_TEXTURE = registerTextureFromImage("internal:loading", combinedLoadingImage, true);
    }

    private static BufferedImage loadBufferedImageFromResource(String namespace, String path) {
        ResourceLocation imageLoc = ResourceLocation.tryBuild(namespace, path);
        try (InputStream in = mc.getResourceManager().getResource(imageLoc).get().open()) {
            BufferedImage bi = ImageIO.read(in);
            if (bi != null) return bi;
            System.err.println("CRITICAL: ImageIO.read returned null for " + namespace + ":" + path);
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load resource image: " + namespace + ":" + path + " - " + e.getMessage());
        }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    private static String sanitize(String key) {
        return key == null ? "null" : key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private static NativeImage toNativeImage(BufferedImage img) {
        if (img == null) return null;
        final int w = img.getWidth();
        final int h = img.getHeight();

        BufferedImage src = img;
        if (img.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage conv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = conv.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(img, 0, 0, null);
            g.dispose();
            src = conv;
        }

        int[] argb = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        NativeImage ni = new NativeImage(w, h, false);

        for (int y = 0, yOff = 0; y < h; y++, yOff += w) {
            for (int x = 0; x < w; x++) {
                // Keep the original horizontal flip
                int a = argb[yOff + (w - 1 - x)];
                int abgr = (a & 0xFF00FF00) | ((a & 0xFF) << 16) | ((a >>> 16) & 0xFF);
                ni.setPixelRGBA(x, y, abgr);
            }
        }
        return ni;
    }

    public static ResourceLocation registerTextureFromImage(String key, BufferedImage img, boolean shouldCache) {
        if (img == null) {
            System.err.println("Attempted to register a null image for key: " + key);
            return NOT_FOUND_TEXTURE;
        }
        if (shouldCache) {
            ResourceLocation cached = cachedTextures.get(key);
            if (cached != null) return cached;
        }

        try {
            NativeImage ni = toNativeImage(img);
            if (ni == null) return NOT_FOUND_TEXTURE;

            DynamicTexture dyn = new DynamicTexture(ni);
            ResourceLocation loc = mc.getTextureManager().register("dynamic/" + sanitize(key), dyn);
            if (shouldCache) {
                cachedTextures.put(key, loc);
            }
            return loc;
        } catch (Exception e) {
            System.err.println("Failed to register texture for key: " + key + " – " + e.getMessage());
            return NOT_FOUND_TEXTURE;
        }
    }

    private static boolean isGif(byte[] bytes) {
        if (bytes == null || bytes.length < 3) return false;
        return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
    }

    private static DecodedGif decodeGif(byte[] data) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("No GIF ImageReader found");
            ImageReader reader = readers.next();
            reader.setInput(stream);

            int num = reader.getNumImages(true);
            int[] delays = new int[num];
            BufferedImage canvas = null;
            Graphics2D g = null;
            int total = 0;
            BufferedImage[] frames = new BufferedImage[num];

            for (int i = 0; i < num; i++) {
                BufferedImage frame = reader.read(i);
                if (canvas == null) {
                    canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                    g = canvas.createGraphics();
                    g.setComposite(AlphaComposite.SrcOver);
                    g.setBackground(new Color(0, 0, 0, 0));
                }

                IIOMetadata meta = reader.getImageMetadata(i);
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
                NodeList gceList = root.getElementsByTagName("GraphicControlExtension");
                IIOMetadataNode gce = (IIOMetadataNode) (gceList.getLength() > 0 ? gceList.item(0) : null);
                String disposal = gce != null ? gce.getAttribute("disposalMethod") : "none";
                int delayCs = 10;
                if (gce != null) {
                    try {
                        delayCs = Integer.parseInt(gce.getAttribute("delayTime"));
                    } catch (NumberFormatException ignored) {}
                }
                int delayMs = Math.max(10, delayCs * 10);
                delays[i] = delayMs;
                total += delayMs;

                IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                int x = desc != null ? Integer.parseInt(desc.getAttribute("imageLeftPosition")) : 0;
                int y = desc != null ? Integer.parseInt(desc.getAttribute("imageTopPosition")) : 0;
                g.drawImage(frame, x, y, null);

                BufferedImage snapshot = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = snapshot.createGraphics();
                gg.setComposite(AlphaComposite.Src);
                gg.drawImage(canvas, 0, 0, null);
                gg.dispose();
                frames[i] = snapshot;

                if ("restoreToBackgroundColor".equals(disposal)) {
                    g.clearRect(x, y, frame.getWidth(), frame.getHeight());
                }
            }
            if (g != null) g.dispose();
            return new DecodedGif(frames, delays, total);
        }
    }

    private static void loadAnimatedGif(String url, byte[] data) throws IOException {
        DecodedGif gif = decodeGif(data);
        List<NativeImage> nativeFrames = new ArrayList<>(gif.frames.length);
        for (BufferedImage bi : gif.frames) nativeFrames.add(toNativeImage(bi));

        mc.execute(() -> {
            ResourceLocation[] locs = new ResourceLocation[nativeFrames.size()];
            for (int i = 0; i < nativeFrames.size(); i++) {
                DynamicTexture dyn = new DynamicTexture(nativeFrames.get(i));
                locs[i] = mc.getTextureManager().register("dynamic/" + sanitize(url + "_frame_" + i), dyn);
            }
            cachedAnimatedTextures.put(url, new AnimatedTexture(locs, gif.delays, gif.totalDuration));
            nativeFrames.clear();
        });
    }

    private static void processUrl(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 10.0; WOW64) AppleWebKit/537.50 (KHTML, like Gecko) Chrome/49.0.1164.162 Safari/536")
                    .timeout(Duration.ofSeconds(20))
                    .build();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid URL, adding to blacklist: " + url);
            blacklist.add(url);
            return;
        }

        for (int i = 1; i <= MAX_LOAD_TRIES; i++) {
            try {
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    byte[] imageBytes = response.body();
                    rawDataCache.put(url, new SoftReference<>(imageBytes));

                    if (isGif(imageBytes)) {
                        loadAnimatedGif(url, imageBytes);
                    } else {
                        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
                            BufferedImage image = ImageIO.read(is);
                            if (image == null) {
                                throw new IOException("ImageIO.read returned null. Unsupported format or corrupt data.");
                            }
                            mc.execute(() -> registerTextureFromImage(url, image, true));
                        }
                    }
                    return; // Success
                } else {
                    throw new IOException("HTTP request failed with status code: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Attempt " + i + "/" + MAX_LOAD_TRIES + " failed to load " + url + ": " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    System.err.println("Image loading interrupted for " + url);
                    break;
                }
            }
        }
        System.err.println("Gave up loading image, adding to blacklist: " + url);
        blacklist.add(url);
    }

    public static ResourceLocation getOrLoadTexture(String url) {
        if (url == null || url.isEmpty()) {
            return NOT_FOUND_TEXTURE;
        }
        AnimatedTexture at = cachedAnimatedTextures.get(url);
        if (at != null) return at.getCurrentFrame();

        ResourceLocation loc = cachedTextures.get(url);
        if (loc != null) return loc;

        if (blacklist.contains(url)) {
            return NOT_FOUND_TEXTURE;
        }

        inFlight.computeIfAbsent(url, key ->
                CompletableFuture.runAsync(() -> processUrl(key), executor)
                        .whenComplete((v, t) -> inFlight.remove(key))
        );

        return LOADING_TEXTURE;
    }

    private static byte[] getRawDataFromCache(String url) {
        SoftReference<byte[]> ref = rawDataCache.get(url);
        return (ref != null) ? ref.get() : null;
    }

    // Rendering helpers

    private static void drawDoubleSidedQuad(
            PoseStack.Pose p, VertexConsumer buf,
            float x0, float y0, float x1, float y1,
            float u0, float v0, float u1, float v1,
            int uO, int vO, int uL, int vL,
            Vector3f nFront, Vector3f nBack
    ) {
        Matrix4f matrix = p.pose();

        // Front
        buf.vertex(matrix, x0, y0, 0f).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x1, y0, 0f).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x1, y1, 0f).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x0, y1, 0f).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();

        // Back
        buf.vertex(matrix, x0, y1, 0f).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x1, y1, 0f).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x1, y0, 0f).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x0, y0, 0f).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
    }

    // Custom UV mapping per vertex (needed for side drapes to match original rotation)
    private static void drawDoubleSidedQuadUV(
            PoseStack.Pose p, VertexConsumer buf,
            float x0, float y0, float x1, float y1,
            float u00, float v00, // UV at (x0,y0)
            float u10, float v10, // UV at (x1,y0)
            float u11, float v11, // UV at (x1,y1)
            float u01, float v01, // UV at (x0,y1)
            int uO, int vO, int uL, int vL,
            Vector3f nFront, Vector3f nBack
    ) {
        Matrix4f matrix = p.pose();

        // Front (x0,y0) -> (u00,v00), etc.
        buf.vertex(matrix, x0, y0, 0f).color(255, 255, 255, 255).uv(u00, v00).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x1, y0, 0f).color(255, 255, 255, 255).uv(u10, v10).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x1, y1, 0f).color(255, 255, 255, 255).uv(u11, v11).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();
        buf.vertex(matrix, x0, y1, 0f).color(255, 255, 255, 255).uv(u01, v01).overlayCoords(uO, vO).uv2(uL, vL).normal(nFront.x(), nFront.y(), nFront.z()).endVertex();

        // Back uses opposite vertex order with same UVs assigned to corresponding corners
        buf.vertex(matrix, x0, y1, 0f).color(255, 255, 255, 255).uv(u01, v01).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x1, y1, 0f).color(255, 255, 255, 255).uv(u11, v11).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x1, y0, 0f).color(255, 255, 255, 255).uv(u10, v10).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
        buf.vertex(matrix, x0, y0, 0f).color(255, 255, 255, 255).uv(u00, v00).overlayCoords(uO, vO).uv2(uL, vL).normal(nBack.x(), nBack.y(), nBack.z()).endVertex();
    }

    private static Vector3f poseNormal(PoseStack.Pose p, float x, float y, float z) {
        Vector3f v = new Vector3f(x, y, z);
        p.pose().transformDirection(v);
        return v;
    }

    // Image rotation helper for texture processing

    /**
     * Applies rotation and flipping to a BufferedImage.
     * This is used for pre-processing images before texture splitting.
     */
    private static BufferedImage applyRotationToImage(BufferedImage original, RotationConfig rotationConfig) {
        if (rotationConfig == null || rotationConfig.isDefault()) {
            return original;
        }

        int width = original.getWidth();
        int height = original.getHeight();

        // Create a new image with potentially different dimensions for rotation
        BufferedImage result;
        Graphics2D g2d;

        // For non-90-degree rotations, we need to calculate the bounding box
        double rotation = Math.toRadians(rotationConfig.getRotation());
        double cos = Math.abs(Math.cos(rotation));
        double sin = Math.abs(Math.sin(rotation));

        int newWidth = (int) Math.ceil(width * cos + height * sin);
        int newHeight = (int) Math.ceil(width * sin + height * cos);

        result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = result.createGraphics();

        // Set high quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Move to center of new image
        g2d.translate(newWidth / 2.0, newHeight / 2.0);

        // Apply rotation
        if (rotationConfig.getRotation() != 0) {
            g2d.rotate(rotation);
        }

        // Apply flipping
        double scaleX = rotationConfig.isFlipHorizontal() ? -1.0 : 1.0;
        double scaleY = rotationConfig.isFlipVertical() ? -1.0 : 1.0;
        if (scaleX != 1.0 || scaleY != 1.0) {
            g2d.scale(scaleX, scaleY);
        }

        // Draw the original image centered
        g2d.drawImage(original, -width / 2, -height / 2, null);
        g2d.dispose();

        return result;
    }

    // UV coordinate transformation for rotation and flipping

    /**
     * Transforms UV coordinates based on RotationConfig.
     * Returns array of 8 floats: [u00, v00, u10, v10, u11, v11, u01, v01] representing the four corners
     * in order expected by drawDoubleSidedQuadUV: (x0,y0), (x1,y0), (x1,y1), (x0,y1)
     */
    private static float[] transformUVCoordinates(RotationConfig rotationConfig) {
        if (rotationConfig == null || rotationConfig.isDefault()) {
            // Default UV mapping for drawDoubleSidedQuadUV: (x0,y0)->(0,1), (x1,y0)->(1,1), (x1,y1)->(1,0), (x0,y1)->(0,0)
            return new float[]{0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        }

        // Start with the four corners of the texture in UV space
        // Corner order: (x0,y0), (x1,y0), (x1,y1), (x0,y1) -> bottom-left, bottom-right, top-right, top-left
        float[][] corners = {{0.0f, 1.0f}, {1.0f, 1.0f}, {1.0f, 0.0f}, {0.0f, 0.0f}};

        // Apply rotation by rotating the UV coordinates
        int rotation = rotationConfig.getRotation();
        if (rotation != 0) {
            double radians = Math.toRadians(rotation);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);

            for (float[] corner : corners) {
                // Translate to center (0.5, 0.5)
                float u = corner[0] - 0.5f;
                float v = corner[1] - 0.5f;

                // Apply rotation
                float newU = (float)(u * cos - v * sin);
                float newV = (float)(u * sin + v * cos);

                // Translate back and store
                corner[0] = newU + 0.5f;
                corner[1] = newV + 0.5f;
            }
        }

        // Apply flipping
        if (rotationConfig.isFlipHorizontal()) {
            for (float[] corner : corners) {
                corner[0] = 1.0f - corner[0];
            }
        }

        if (rotationConfig.isFlipVertical()) {
            for (float[] corner : corners) {
                corner[1] = 1.0f - corner[1];
            }
        }

        // Keep UVs in bounds so arbitrary-angle rotation does not sample wrapped tiles.
        for (float[] corner : corners) {
            corner[0] = Math.max(0.0f, Math.min(1.0f, corner[0]));
            corner[1] = Math.max(0.0f, Math.min(1.0f, corner[1]));
        }

        // Return as flat array in the order expected by drawDoubleSidedQuadUV
        return new float[]{
            corners[0][0], corners[0][1], // (x0,y0) -> u00, v00
            corners[1][0], corners[1][1], // (x1,y0) -> u10, v10
            corners[2][0], corners[2][1], // (x1,y1) -> u11, v11
            corners[3][0], corners[3][1]  // (x0,y1) -> u01, v01
        };
    }

    // Basic image renderers

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url
    ) {
        renderImageFromURL(ps, bufSrc, packedLight, packedOverlay, partialTick, width, height, url, false);
    }

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, boolean keepAspectRatio
    ) {
        ResourceLocation tex = getOrLoadTexture(url);
        if (tex == null) {
            tex = NOT_FOUND_TEXTURE;
        }

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        ps.pushPose();
        ps.translate(0.5, 1.01, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose p = ps.last();
        VertexConsumer buf = bufSrc.getBuffer(RenderType.text(tex));

        float hw = width / 2f, hh = height / 2f;
        if (keepAspectRatio) {
            float aspectRatio = width / height;
            if (aspectRatio > 1) {
                hh /= aspectRatio;
            } else {
                hw *= aspectRatio;
            }
        }

        Matrix4f matrix = p.pose();

        Vector3f nUp = new Vector3f(0, 1, 0);
        matrix.transformDirection(nUp);
        Vector3f nDown = new Vector3f(0, -1, 0);
        matrix.transformDirection(nDown);

        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();

        ps.popPose();
    }

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, boolean keepAspectRatio, ImageFrameAlignment alignment, int offsetX, int offsetY, int offsetZ
    ) {
        ResourceLocation tex = getOrLoadTexture(url);
        if (tex == null) {
            tex = NOT_FOUND_TEXTURE;
        }

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        ps.pushPose();
        ps.translate(0.5, 1.01, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose p = ps.last();
        VertexConsumer buf = bufSrc.getBuffer(RenderType.text(tex));

        float hw = width / 2f, hh = height / 2f;
        if (keepAspectRatio) {
            float aspectRatio = width / height;
            if (aspectRatio > 1) {
                hh /= aspectRatio;
            } else {
                hw *= aspectRatio;
            }
        }

        Matrix4f matrix = p.pose();

        Vector3f nUp = new Vector3f(0, 1, 0);
        matrix.transformDirection(nUp);
        Vector3f nDown = new Vector3f(0, -1, 0);
        matrix.transformDirection(nDown);

        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();

        ps.popPose();
    }

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, boolean keepAspectRatio, boolean flipX, boolean flipY
    ) {
        ResourceLocation tex = getOrLoadTexture(url);
        if (tex == null) {
            tex = NOT_FOUND_TEXTURE;
        }
        final float u0 = flipX ? 1.0f : 0.0f;
        final float u1 = flipX ? 0.0f : 1.0f;
        final float v0 = flipY ? 1.0f : 0.0f;
        final float v1 = flipY ? 0.0f : 1.0f;

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;
        ps.pushPose();
        ps.translate(0.5, 1.01, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose p = ps.last();
        VertexConsumer buf = bufSrc.getBuffer(RenderType.text(tex));

        float hw = width / 2f;
        float hh = height / 2f;

        if (keepAspectRatio) {
            float aspectRatio = width / height;
            if (aspectRatio > 1f) {
                hh /= aspectRatio;
            } else {
                hw *= aspectRatio;
            }
        }

        Matrix4f matrix = p.pose();

        Vector3f nUp = new Vector3f(0, 1, 0);
        matrix.transformDirection(nUp);
        Vector3f nDown = new Vector3f(0, -1, 0);
        matrix.transformDirection(nDown);

        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(u0, v1).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(u1, v1).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(u1, v0).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(u0, v0).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

        buf.vertex(matrix, -hw, hh, 0f)
                .color(255, 255, 255, 255).uv(u0, v0).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, hh, 0f)
                .color(255, 255, 255, 255).uv(u1, v0).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(u1, v1).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        buf.vertex(matrix, -hw, -hh, 0f)
                .color(255, 255, 255, 255).uv(u0, v1).overlayCoords(uO, vO).uv2(uL, vL)
                .normal(nDown.x(), nDown.y(), nDown.z()).endVertex();

        ps.popPose();
    }

    // RotationConfig-enabled image renderers

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, RotationConfig rotationConfig
    ) {
        renderImageFromURL(ps, bufSrc, packedLight, packedOverlay, partialTick, width, height, url, false, rotationConfig);
    }

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, boolean keepAspectRatio, RotationConfig rotationConfig
    ) {
        ResourceLocation tex = getOrLoadTexture(url);
        if (tex == null) tex = NOT_FOUND_TEXTURE;

        // Get transformed UV coordinates based on rotation config
        float[] uvCoords = transformUVCoordinates(rotationConfig);

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        ps.pushPose();
        ps.translate(0.5, 1.01, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose p = ps.last();
        VertexConsumer buf = bufSrc.getBuffer(RenderType.text(tex));

        float hw = width / 2f;
        float hh = height / 2f;
        if (keepAspectRatio) {
            float aspectRatio = width / height;
            if (aspectRatio > 1f) {
                hh /= aspectRatio;
            } else {
                hw *= aspectRatio;
            }
        }

        Vector3f nUp = poseNormal(p, 0, 1, 0);
        Vector3f nDown = poseNormal(p, 0, -1, 0);

        // Use transformed UV coordinates for the quad
        drawDoubleSidedQuadUV(p, buf,
                -hw, -hh, hw, hh,
                uvCoords[0], uvCoords[1], // (x0,y0) -> u00, v00
                uvCoords[2], uvCoords[3], // (x1,y0) -> u10, v10
                uvCoords[4], uvCoords[5], // (x1,y1) -> u11, v11
                uvCoords[6], uvCoords[7], // (x0,y1) -> u01, v01
                uO, vO, uL, vL,
                nUp, nDown);

        ps.popPose();
    }

    // Splitting helpers

    private static int clampSplit(int v) { return Math.max(1, v); }
    private static float safeDiv(float a, float b) { return b <= 0 ? 0f : (a / b); }

    private static BufferedImage[] splitThreeVertical(BufferedImage src, int leftW, int midW, int rightW) {
        int W = src.getWidth(), H = src.getHeight();
        leftW = clampSplit(leftW);
        midW = clampSplit(midW);
        rightW = clampSplit(rightW);
        int sum = leftW + midW + rightW;
        if (sum > W) {
            int over = sum - W;
            if (rightW > 1) rightW = Math.max(1, rightW - over);
            else if (midW > 1) midW = Math.max(1, midW - over);
            else leftW = Math.max(1, leftW - over);
        }
        return new BufferedImage[]{
                src.getSubimage(0, 0, leftW, H),
                src.getSubimage(leftW, 0, midW, H),
                src.getSubimage(leftW + midW, 0, rightW, H)
        };
    }

    private static BufferedImage[] splitThreeHorizontal(BufferedImage src, int topH, int midH, int botH) {
        int W = src.getWidth(), H = src.getHeight();
        topH = clampSplit(topH);
        midH = clampSplit(midH);
        botH = clampSplit(botH);
        int sum = topH + midH + botH;
        if (sum > H) {
            int over = sum - H;
            if (botH > 1) botH = Math.max(1, botH - over);
            else if (midH > 1) midH = Math.max(1, midH - over);
            else topH = Math.max(1, topH - over);
        }
        return new BufferedImage[]{
                src.getSubimage(0, 0, W, topH),
                src.getSubimage(0, topH, W, midH),
                src.getSubimage(0, topH + midH, W, botH)
        };
    }

    private static BufferedImage[] splitTwoHorizontal(BufferedImage src, int topH) {
        int W = src.getWidth(), H = src.getHeight();
        topH = clampSplit(topH);
        int botH = Math.max(1, H - topH);
        return new BufferedImage[]{
                src.getSubimage(0, 0, W, topH),
                src.getSubimage(0, topH, W, botH)
        };
    }

    private static BufferedImage[] splitTwoVertical(BufferedImage src, int leftW) {
        int W = src.getWidth(), H = src.getHeight();
        leftW = clampSplit(leftW);
        int rightW = Math.max(1, W - leftW);
        return new BufferedImage[]{
                src.getSubimage(0, 0, leftW, H),
                src.getSubimage(leftW, 0, rightW, H)
        };
    }

    /**
     * Splits an image into 5 parts for all-sides bed drapes:
     * [0] = center topper, [1] = left drape, [2] = right drape, [3] = front drape, [4] = back drape
     */
    private static BufferedImage concatHorizontal(BufferedImage a, BufferedImage b) {
        int w = a.getWidth() + b.getWidth();
        int h = Math.min(a.getHeight(), b.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(a, 0, 0, null);
        g.drawImage(b, a.getWidth(), 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage[] splitImageForAllSides(BufferedImage src, float bedWidth, float bedLength, float drapeDepth) {
        int W = src.getWidth(), H = src.getHeight();

        // Calculate proportions
        float totalWidth = bedWidth + 2f * drapeDepth;
        float totalHeight = bedLength + 2f * drapeDepth;

        if (totalWidth <= 0 || totalHeight <= 0) return null;

        // Calculate pixel dimensions
        int leftW = Math.round(safeDiv(drapeDepth, totalWidth) * W);
        int centerW = Math.round(safeDiv(bedWidth, totalWidth) * W);
        int rightW = W - leftW - centerW;

        int topH = Math.round(safeDiv(drapeDepth, totalHeight) * H);
        int centerH = Math.round(safeDiv(bedLength, totalHeight) * H);
        int bottomH = H - topH - centerH;

        // Ensure minimum sizes
        leftW = clampSplit(leftW);
        centerW = clampSplit(centerW);
        rightW = clampSplit(rightW);
        topH = clampSplit(topH);
        centerH = clampSplit(centerH);
        bottomH = clampSplit(bottomH);

        try {
            return new BufferedImage[]{
                // [0] Center topper
                src.getSubimage(leftW, topH, centerW, centerH),
                // [1] Left drape - EXACT match to working original: right slice -> LEFT drape
                src.getSubimage(leftW + centerW, topH, rightW, centerH),
                // [2] Right drape - EXACT match to working original: left slice -> RIGHT drape
                src.getSubimage(0, topH, leftW, centerH),
                // [3] Front drape (full width strip from top)
                src.getSubimage(leftW, 0, centerW, topH),
                // [4] Back drape (full width strip from bottom)
                src.getSubimage(leftW, topH + centerH, centerW, bottomH)
            };
        } catch (Exception e) {
            System.err.println("Failed to split image for all sides: " + e.getMessage());
            return null;
        }
    }

    // Drapes: sides

    public static void renderImageSideDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url
    ) {
        String key = url + "_split_sides_" + bedWidth + "_" + drapeDepth;
        ResourceLocation topperTex, leftDrapeTex, rightDrapeTex;

        if (cachedAnimatedSideTopper.containsKey(key)) {
            topperTex = cachedAnimatedSideTopper.get(key).getCurrentFrame();
            leftDrapeTex = cachedAnimatedSideDrapeLeft.get(key).getCurrentFrame();
            rightDrapeTex = cachedAnimatedSideDrapeRight.get(key).getCurrentFrame();
        } else if (cachedSideTopper.containsKey(key)) {
            topperTex = cachedSideTopper.get(key);
            leftDrapeTex = cachedSideDrapeLeft.get(key);
            rightDrapeTex = cachedSideDrapeRight.get(key);
        } else if (pendingSideTransforms.contains(key)) {
            topperTex = leftDrapeTex = rightDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = leftDrapeTex = rightDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSideTransforms.add(key);
                topperTex = leftDrapeTex = rightDrapeTex = LOADING_TEXTURE;

                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeLefts = new ArrayList<>();
                            List<NativeImage> nativeRights = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                int W = frame.getWidth(), H = frame.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks <= 0) continue;
                                int leftW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                int midW = W - 2 * leftW;
                                if (leftW <= 0 || midW <= 0) continue;

                                BufferedImage[] parts = splitThreeVertical(frame, leftW, midW, leftW);
                                // parts[0] left slice -> RIGHT drape
                                nativeRights.add(toNativeImage(parts[0]));
                                nativeToppers.add(toNativeImage(parts[1]));
                                // parts[2] right slice -> LEFT drape
                                nativeLefts.add(toNativeImage(parts[2]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] leftLocs = uploadFrames(key + "_left_", nativeLefts);
                                ResourceLocation[] rightLocs = uploadFrames(key + "_right_", nativeRights);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeLeft.put(key, new AnimatedTexture(leftLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeRight.put(key, new AnimatedTexture(rightLocs, gif.delays, gif.totalDuration));
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                int W = full.getWidth(), H = full.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks > 0) {
                                    int leftW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                    int midW = W - 2 * leftW;
                                    if (leftW > 0 && midW > 0) {
                                        BufferedImage[] parts = splitThreeVertical(full, leftW, midW, leftW);
                                        BufferedImage intendedRightDrapeImg = parts[0];
                                        BufferedImage midImg = parts[1];
                                        BufferedImage intendedLeftDrapeImg = parts[2];

                                        mc.execute(() -> {
                                            cachedSideDrapeLeft.put(key, registerTextureFromImage(key + "_drape_left", intendedLeftDrapeImg, true));
                                            cachedSideTopper.put(key, registerTextureFromImage(key + "_topper", midImg, true));
                                            cachedSideDrapeRight.put(key, registerTextureFromImage(key + "_drape_right", intendedRightDrapeImg, true));
                                        });
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error splitting texture " + key + ": " + e.getMessage());
                        blacklist.add(url);
                    } finally {
                        pendingSideTransforms.remove(key);
                    }
                });
            }
        }

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        // Topper
        ps.pushPose();
        ps.translate(0, 1.02f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hw += 0.01f;
            Matrix4f matrix = p.pose();

            Vector3f nUp = new Vector3f(0, 1, 0);
            matrix.transformDirection(nUp);
            Vector3f nDown = new Vector3f(0, -1, 0);
            matrix.transformDirection(nDown);

            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        }
        ps.popPose();

        // Left drape (uses original rotated UV mapping)
        ps.pushPose();
        ps.translate(-bedWidth / 2f - 0.01f, 0.720f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XN.rotationDegrees(-180));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Matrix4f matrix = p.pose();

            Vector3f fn = new Vector3f(0, 0, 1);
            matrix.transformDirection(fn);
            Vector3f bn = new Vector3f(0, 0, -1);
            matrix.transformDirection(bn);

            buf.vertex(matrix, -hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, -hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();

            buf.vertex(matrix, -hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, -hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
        }
        ps.popPose();

        // Right drape (same UV pattern as original code)
        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Matrix4f matrix = p.pose();

            Vector3f fn = new Vector3f(0, 0, 1);
            matrix.transformDirection(fn);
            Vector3f bn = new Vector3f(0, 0, -1);
            matrix.transformDirection(bn);

            buf.vertex(matrix, -hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, -hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();

            buf.vertex(matrix, -hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hl, 0, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, -hl, -d, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
        }
        ps.popPose();
    }

    // RotationConfig-enabled side drapes renderer
    public static void renderImageSideDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // For bed drapes, we need to apply rotation to the entire bed image before splitting
        // This ensures the rotation affects the texture mapping, not the geometry
        String key = url + "_split_sides_" + bedWidth + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, leftDrapeTex, rightDrapeTex;

        if (cachedAnimatedSideTopper.containsKey(key)) {
            topperTex = cachedAnimatedSideTopper.get(key).getCurrentFrame();
            leftDrapeTex = cachedAnimatedSideDrapeLeft.get(key).getCurrentFrame();
            rightDrapeTex = cachedAnimatedSideDrapeRight.get(key).getCurrentFrame();
        } else if (cachedSideTopper.containsKey(key)) {
            topperTex = cachedSideTopper.get(key);
            leftDrapeTex = cachedSideDrapeLeft.get(key);
            rightDrapeTex = cachedSideDrapeRight.get(key);
        } else if (pendingSideTransforms.contains(key)) {
            topperTex = leftDrapeTex = rightDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = leftDrapeTex = rightDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSideTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            // Handle GIF with rotation - apply rotation to each frame before splitting
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeLeftDrapes = new ArrayList<>();
                            List<NativeImage> nativeRightDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks <= 0) continue;
                                int leftW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                int midW = W - 2 * leftW;
                                if (leftW <= 0 || midW <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitThreeVertical(rotatedFrame, leftW, midW, leftW);
                                // parts[0] left slice -> RIGHT drape, parts[2] right slice -> LEFT drape
                                nativeRightDrapes.add(toNativeImage(parts[0]));
                                nativeToppers.add(toNativeImage(parts[1]));
                                nativeLeftDrapes.add(toNativeImage(parts[2]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] leftLocs = uploadFrames(key + "_left_", nativeLeftDrapes);
                                ResourceLocation[] rightLocs = uploadFrames(key + "_right_", nativeRightDrapes);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeLeft.put(key, new AnimatedTexture(leftLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeRight.put(key, new AnimatedTexture(rightLocs, gif.delays, gif.totalDuration));
                                pendingSideTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks > 0) {
                                    int leftW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                    int midW = W - 2 * leftW;
                                    if (leftW > 0 && midW > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitThreeVertical(rotatedFull, leftW, midW, leftW);
                                        // parts[0] left slice -> RIGHT drape, parts[2] right slice -> LEFT drape
                                        BufferedImage rightDrapeImg = parts[0];
                                        BufferedImage topperImg = parts[1];
                                        BufferedImage leftDrapeImg = parts[2];

                                        mc.execute(() -> {
                                            cachedSideTopper.put(key, registerTextureFromImage(key + "_topper", topperImg, true));
                                            cachedSideDrapeLeft.put(key, registerTextureFromImage(key + "_drape_left", leftDrapeImg, true));
                                            cachedSideDrapeRight.put(key, registerTextureFromImage(key + "_drape_right", rightDrapeImg, true));
                                            pendingSideTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process side drapes with rotation: " + e.getMessage());
                        pendingSideTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts (same geometry as original, but with rotated textures)
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        // Topper
        ps.pushPose();
        ps.translate(0, 1.02f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hw += 0.01f; // bleed
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Left drape
        ps.pushPose();
        ps.translate(-bedWidth / 2f - 0.01f, 0.720f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XN.rotationDegrees(-180));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);

            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();

        // Right drape
        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);

            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();
    }

    // Drapes: front/back

    public static void renderImageFrontBackDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url
    ) {
        String key = url + "_split_frontback_" + bedLength + "_" + drapeDepth;
        ResourceLocation topperTex, frontDrapeTex, backDrapeTex;

        if (cachedAnimatedFrontBackTopper.containsKey(key)) {
            topperTex = cachedAnimatedFrontBackTopper.get(key).getCurrentFrame();
            frontDrapeTex = cachedAnimatedFrontDrape.get(key).getCurrentFrame();
            backDrapeTex = cachedAnimatedBackDrape.get(key).getCurrentFrame();
        } else if (cachedFrontBackTopper.containsKey(key)) {
            topperTex = cachedFrontBackTopper.get(key);
            frontDrapeTex = cachedFrontDrape.get(key);
            backDrapeTex = cachedBackDrape.get(key);
        } else if (pendingFrontBackTransforms.contains(key)) {
            topperTex = frontDrapeTex = backDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = frontDrapeTex = backDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingFrontBackTransforms.add(key);
                topperTex = frontDrapeTex = backDrapeTex = LOADING_TEXTURE;

                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeFronts = new ArrayList<>();
                            List<NativeImage> nativeBacks = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                int W = frame.getWidth(), H = frame.getHeight();
                                float totalLengthBlocks = bedLength + 2f * drapeDepth;
                                if (totalLengthBlocks <= 0) continue;
                                int frontH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                int midH = H - 2 * frontH;
                                if (frontH <= 0 || midH <= 0) continue;

                                BufferedImage[] parts = splitThreeHorizontal(frame, frontH, midH, frontH);
                                nativeBacks.add(toNativeImage(parts[0]));
                                nativeToppers.add(toNativeImage(parts[1]));
                                nativeFronts.add(toNativeImage(parts[2]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] frontLocs = uploadFrames(key + "_front_", nativeFronts);
                                ResourceLocation[] backLocs = uploadFrames(key + "_back_", nativeBacks);

                                cachedAnimatedFrontBackTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedFrontDrape.put(key, new AnimatedTexture(frontLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedBackDrape.put(key, new AnimatedTexture(backLocs, gif.delays, gif.totalDuration));
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                int W = full.getWidth(), H = full.getHeight();
                                float totalLengthBlocks = bedLength + 2f * drapeDepth;
                                if (totalLengthBlocks > 0) {
                                    int frontH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                    int midH = H - 2 * frontH;
                                    if (frontH > 0 && midH > 0) {
                                        BufferedImage[] parts = splitThreeHorizontal(full, frontH, midH, frontH);
                                        BufferedImage intendedBackDrapeImg = parts[0];
                                        BufferedImage midImg = parts[1];
                                        BufferedImage intendedFrontDrapeImg = parts[2];

                                        mc.execute(() -> {
                                            cachedFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", intendedFrontDrapeImg, true));
                                            cachedFrontBackTopper.put(key, registerTextureFromImage(key + "_topper_frontback", midImg, true));
                                            cachedBackDrape.put(key, registerTextureFromImage(key + "_drape_back", intendedBackDrapeImg, true));
                                        });
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error splitting texture " + key + ": " + e.getMessage());
                        blacklist.add(url);
                    } finally {
                        pendingFrontBackTransforms.remove(key);
                    }
                });
            }
        }

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float topperY = 1.02f;
        float frontDrapeZ = -bedLength / 2f - 0.01f;
        float backDrapeZ = bedLength / 2f + 0.01f;

        // Topper
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hl += 0.01f;
            Matrix4f matrix = p.pose();

            Vector3f nUp = new Vector3f(0, 1, 0);
            matrix.transformDirection(nUp);
            Vector3f nDown = new Vector3f(0, -1, 0);
            matrix.transformDirection(nDown);

            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        }
        ps.popPose();

        // Front drape
        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Matrix4f matrix = p.pose();

            Vector3f bn = new Vector3f(0, 0, -1);
            matrix.transformDirection(bn);
            Vector3f fn = new Vector3f(0, 0, 1);
            matrix.transformDirection(fn);

            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();

            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
        }
        ps.popPose();

        // Back drape
        ps.pushPose();
        ps.translate(0, topperY, backDrapeZ);
        ps.mulPose(Axis.XN.rotationDegrees(180));
        ps.translate(0, 0.30, 0);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(backDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Matrix4f matrix = p.pose();

            Vector3f fn = new Vector3f(0, 0, 1);
            matrix.transformDirection(fn);
            Vector3f bn = new Vector3f(0, 0, -1);
            matrix.transformDirection(bn);

            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();
            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(fn.x(), fn.y(), fn.z()).endVertex();

            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(bn.x(), bn.y(), bn.z()).endVertex();
        }
        ps.popPose();
    }

    // Drapes: single front

    public static void renderImageFrontDrapeFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url
    ) {
        String key = url + "_split_front_" + bedLength + "_" + drapeDepth;
        ResourceLocation topperTex, frontDrapeTex;

        if (cachedAnimatedSingleTopper.containsKey(key)) {
            topperTex = cachedAnimatedSingleTopper.get(key).getCurrentFrame();
            frontDrapeTex = cachedAnimatedSingleFrontDrape.get(key).getCurrentFrame();
        } else if (cachedSingleTopper.containsKey(key)) {
            topperTex = cachedSingleTopper.get(key);
            frontDrapeTex = cachedSingleFrontDrape.get(key);
        } else if (pendingSingleFrontTransforms.contains(key)) {
            topperTex = frontDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = frontDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSingleFrontTransforms.add(key);
                topperTex = frontDrapeTex = LOADING_TEXTURE;

                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                int W = frame.getWidth(), H = frame.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks <= 0) continue;
                                int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                int topperH = H - drapeH;
                                if (drapeH <= 0 || topperH <= 0) continue;

                                BufferedImage[] parts = splitTwoHorizontal(frame, topperH);
                                nativeToppers.add(toNativeImage(parts[0]));
                                nativeDrapes.add(toNativeImage(parts[1]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] drapeLocs = uploadFrames(key + "_drape_", nativeDrapes);

                                cachedAnimatedSingleTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSingleFrontDrape.put(key, new AnimatedTexture(drapeLocs, gif.delays, gif.totalDuration));
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                int W = full.getWidth(), H = full.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks > 0) {
                                    int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                    int topperH = H - drapeH;
                                    if (drapeH > 0 && topperH > 0) {
                                        BufferedImage[] parts = splitTwoHorizontal(full, topperH);
                                        BufferedImage topperImg = parts[0];
                                        BufferedImage drapeImg = parts[1];

                                        mc.execute(() -> {
                                            cachedSingleFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", drapeImg, true));
                                            cachedSingleTopper.put(key, registerTextureFromImage(key + "_topper_front", topperImg, true));
                                        });
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error splitting texture " + key + ": " + e.getMessage());
                        blacklist.add(url);
                    } finally {
                        pendingSingleFrontTransforms.remove(key);
                    }
                });
            }
        }

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float topperY = 1.02f;
        float frontDrapeZ = -bedLength / 2f - 0.01f;

        // Topper
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hl += 0.01f;
            Matrix4f matrix = p.pose();

            Vector3f nUp = new Vector3f(0, 1, 0);
            matrix.transformDirection(nUp);
            Vector3f nDown = new Vector3f(0, -1, 0);
            matrix.transformDirection(nDown);

            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();
            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nUp.x(), nUp.y(), nUp.z()).endVertex();

            buf.vertex(matrix, -hw, hl, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, hl, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, hw, -hl, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
            buf.vertex(matrix, -hw, -hl, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nDown.x(), nDown.y(), nDown.z()).endVertex();
        }
        ps.popPose();

        // Front drape
        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Matrix4f matrix = p.pose();

            Vector3f nOut = new Vector3f(0, 0, -1);
            matrix.transformDirection(nOut);
            Vector3f nIn = new Vector3f(0, 0, 1);
            matrix.transformDirection(nIn);

            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nOut.x(), nOut.y(), nOut.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nOut.x(), nOut.y(), nOut.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nOut.x(), nOut.y(), nOut.z()).endVertex();
            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nOut.x(), nOut.y(), nOut.z()).endVertex();

            buf.vertex(matrix, -hw, 0, 0f).color(255, 255, 255, 255).uv(0f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nIn.x(), nIn.y(), nIn.z()).endVertex();
            buf.vertex(matrix, hw, 0, 0f).color(255, 255, 255, 255).uv(1f, 0f).overlayCoords(uO, vO).uv2(uL, vL).normal(nIn.x(), nIn.y(), nIn.z()).endVertex();
            buf.vertex(matrix, hw, -d, 0f).color(255, 255, 255, 255).uv(1f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nIn.x(), nIn.y(), nIn.z()).endVertex();
            buf.vertex(matrix, -hw, -d, 0f).color(255, 255, 255, 255).uv(0f, 1f).overlayCoords(uO, vO).uv2(uL, vL).normal(nIn.x(), nIn.y(), nIn.z()).endVertex();
        }
        ps.popPose();
    }

    // RotationConfig-enabled front/back drapes renderer
    public static void renderImageFrontBackDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // For bed drapes, we need to apply rotation to the entire bed image before splitting
        String key = url + "_split_frontback_" + bedLength + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, frontDrapeTex, backDrapeTex;

        if (cachedAnimatedFrontBackTopper.containsKey(key)) {
            topperTex = cachedAnimatedFrontBackTopper.get(key).getCurrentFrame();
            frontDrapeTex = cachedAnimatedFrontDrape.get(key).getCurrentFrame();
            backDrapeTex = cachedAnimatedBackDrape.get(key).getCurrentFrame();
        } else if (cachedFrontBackTopper.containsKey(key)) {
            topperTex = cachedFrontBackTopper.get(key);
            frontDrapeTex = cachedFrontDrape.get(key);
            backDrapeTex = cachedBackDrape.get(key);
        } else if (pendingFrontBackTransforms.contains(key)) {
            topperTex = frontDrapeTex = backDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = frontDrapeTex = backDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingFrontBackTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeFronts = new ArrayList<>();
                            List<NativeImage> nativeBacks = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalLengthBlocks = bedLength + 2f * drapeDepth;
                                if (totalLengthBlocks <= 0) continue;
                                int frontH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                int midH = H - 2 * frontH;
                                if (frontH <= 0 || midH <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitThreeHorizontal(rotatedFrame, frontH, midH, frontH);
                                nativeBacks.add(toNativeImage(parts[0]));
                                nativeToppers.add(toNativeImage(parts[1]));
                                nativeFronts.add(toNativeImage(parts[2]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] frontLocs = uploadFrames(key + "_front_", nativeFronts);
                                ResourceLocation[] backLocs = uploadFrames(key + "_back_", nativeBacks);

                                cachedAnimatedFrontBackTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedFrontDrape.put(key, new AnimatedTexture(frontLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedBackDrape.put(key, new AnimatedTexture(backLocs, gif.delays, gif.totalDuration));
                                pendingFrontBackTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalLengthBlocks = bedLength + 2f * drapeDepth;
                                if (totalLengthBlocks > 0) {
                                    int frontH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                    int midH = H - 2 * frontH;
                                    if (frontH > 0 && midH > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitThreeHorizontal(rotatedFull, frontH, midH, frontH);
                                        BufferedImage intendedBackDrapeImg = parts[0];
                                        BufferedImage midImg = parts[1];
                                        BufferedImage intendedFrontDrapeImg = parts[2];

                                        mc.execute(() -> {
                                            cachedFrontBackTopper.put(key, registerTextureFromImage(key + "_topper", midImg, true));
                                            cachedFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", intendedFrontDrapeImg, true));
                                            cachedBackDrape.put(key, registerTextureFromImage(key + "_drape_back", intendedBackDrapeImg, true));
                                            pendingFrontBackTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process front/back drapes with rotation: " + e.getMessage());
                        pendingFrontBackTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts (same geometry as original, but with rotated textures)
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float topperY = 1.02f;
        float frontDrapeZ = -bedLength / 2f - 0.01f;
        float backDrapeZ = bedLength / 2f + 0.01f;

        // Topper
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hl += 0.01f; // bleed
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Front drape
        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f nOut = poseNormal(p, 0, 0, -1);
            Vector3f nIn = poseNormal(p, 0, 0, 1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nOut, nIn);
        }
        ps.popPose();

        // Back drape
        ps.pushPose();
        ps.translate(0, topperY, backDrapeZ);
        ps.mulPose(Axis.XN.rotationDegrees(180));
        ps.translate(0, 0.30, 0);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(backDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f nFront = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nFront, nBack);
        }
        ps.popPose();
    }

    // RotationConfig-enabled single front drape renderer
    public static void renderImageFrontDrapeFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // For bed drapes, we need to apply rotation to the entire bed image before splitting
        String key = url + "_split_front_" + bedLength + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, frontDrapeTex;

        if (cachedAnimatedSingleTopper.containsKey(key)) {
            topperTex = cachedAnimatedSingleTopper.get(key).getCurrentFrame();
            frontDrapeTex = cachedAnimatedSingleFrontDrape.get(key).getCurrentFrame();
        } else if (cachedSingleTopper.containsKey(key)) {
            topperTex = cachedSingleTopper.get(key);
            frontDrapeTex = cachedSingleFrontDrape.get(key);
        } else if (pendingSingleFrontTransforms.contains(key)) {
            topperTex = frontDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = frontDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSingleFrontTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks <= 0) continue;
                                int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                int topperH = H - drapeH;
                                if (drapeH <= 0 || topperH <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitTwoHorizontal(rotatedFrame, topperH);
                                nativeToppers.add(toNativeImage(parts[0]));
                                nativeDrapes.add(toNativeImage(parts[1]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] drapeLocs = uploadFrames(key + "_drape_", nativeDrapes);

                                cachedAnimatedSingleTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSingleFrontDrape.put(key, new AnimatedTexture(drapeLocs, gif.delays, gif.totalDuration));
                                pendingSingleFrontTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks > 0) {
                                    int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                    int topperH = H - drapeH;
                                    if (drapeH > 0 && topperH > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitTwoHorizontal(rotatedFull, topperH);
                                        BufferedImage topperImg = parts[0];
                                        BufferedImage drapeImg = parts[1];

                                        mc.execute(() -> {
                                            cachedSingleFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", drapeImg, true));
                                            cachedSingleTopper.put(key, registerTextureFromImage(key + "_topper_front", topperImg, true));
                                            pendingSingleFrontTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process single front drape with rotation: " + e.getMessage());
                        pendingSingleFrontTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts (same geometry as original, but with rotated textures)
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float topperY = 1.02f;
        float frontDrapeZ = -bedLength / 2f - 0.01f;

        // Topper
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hl += 0.01f;
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Front drape
        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f nOut = poseNormal(p, 0, 0, -1);
            Vector3f nIn = poseNormal(p, 0, 0, 1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nOut, nIn);
        }
        ps.popPose();
    }

    // New render methods for additional bed types

    public static void renderImageBackDrapeFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // Similar to front drape but positioned at the back
        String key = url + "_split_back_" + bedLength + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, backDrapeTex;

        if (cachedAnimatedSingleTopper.containsKey(key)) {
            topperTex = cachedAnimatedSingleTopper.get(key).getCurrentFrame();
            backDrapeTex = cachedAnimatedSingleFrontDrape.get(key).getCurrentFrame();
        } else if (cachedSingleTopper.containsKey(key)) {
            topperTex = cachedSingleTopper.get(key);
            backDrapeTex = cachedSingleFrontDrape.get(key);
        } else if (pendingSingleFrontTransforms.contains(key)) {
            topperTex = backDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = backDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSingleFrontTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks <= 0) continue;
                                int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                int topperH = H - drapeH;
                                if (drapeH <= 0 || topperH <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitTwoHorizontal(rotatedFrame, drapeH); // Back drape first
                                nativeDrapes.add(toNativeImage(parts[0]));
                                nativeToppers.add(toNativeImage(parts[1]));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] drapeLocs = uploadFrames(key + "_drape_", nativeDrapes);

                                cachedAnimatedSingleTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSingleFrontDrape.put(key, new AnimatedTexture(drapeLocs, gif.delays, gif.totalDuration));
                                pendingSingleFrontTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalLengthBlocks = bedLength + drapeDepth;
                                if (totalLengthBlocks > 0) {
                                    int drapeH = Math.round(safeDiv(drapeDepth, totalLengthBlocks) * H);
                                    int topperH = H - drapeH;
                                    if (drapeH > 0 && topperH > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitTwoHorizontal(rotatedFull, drapeH); // Back drape first
                                        BufferedImage drapeImg = parts[0];
                                        BufferedImage topperImg = parts[1];

                                        mc.execute(() -> {
                                            cachedSingleFrontDrape.put(key, registerTextureFromImage(key + "_drape_back", drapeImg, true));
                                            cachedSingleTopper.put(key, registerTextureFromImage(key + "_topper_back", topperImg, true));
                                            pendingSingleFrontTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process back drape with rotation: " + e.getMessage());
                        pendingSingleFrontTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float topperY = 1.02f;
        float backDrapeZ = bedLength / 2f + 0.01f;

        // Topper
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hl += 0.01f;
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Back drape
        ps.pushPose();
        ps.translate(0, topperY, backDrapeZ);
        ps.mulPose(Axis.XN.rotationDegrees(180));
        ps.translate(0, 0.30, 0);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(backDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f nFront = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nFront, nBack);
        }
        ps.popPose();
    }



    public static void renderImageLeftDrapeOnlyFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // Similar to side drapes but only render left side
        String key = url + "_split_left_only_" + bedWidth + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, leftDrapeTex;

        if (cachedAnimatedSideTopper.containsKey(key)) {
            topperTex = cachedAnimatedSideTopper.get(key).getCurrentFrame();
            leftDrapeTex = cachedAnimatedSideDrapeLeft.get(key).getCurrentFrame();
        } else if (cachedSideTopper.containsKey(key)) {
            topperTex = cachedSideTopper.get(key);
            leftDrapeTex = cachedSideDrapeLeft.get(key);
        } else if (pendingSideTransforms.contains(key)) {
            topperTex = leftDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = leftDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSideTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeLeftDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth; // match original proportions
                                if (totalWidthBlocks <= 0) continue;
                                int sliceW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                int midW = W - 2 * sliceW;
                                if (sliceW <= 0 || midW <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitThreeVertical(rotatedFrame, sliceW, midW, sliceW);
                                BufferedImage topperSource = concatHorizontal(parts[0], parts[1]); // keep full image on top when only left drape is shown
                                nativeLeftDrapes.add(toNativeImage(parts[2])); // right slice -> left drape
                                nativeToppers.add(toNativeImage(topperSource));
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] leftLocs = uploadFrames(key + "_left_", nativeLeftDrapes);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeLeft.put(key, new AnimatedTexture(leftLocs, gif.delays, gif.totalDuration));
                                pendingSideTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks > 0) {
                                    int sliceW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                    int midW = W - 2 * sliceW;
                                    if (sliceW > 0 && midW > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitThreeVertical(rotatedFull, sliceW, midW, sliceW);
                                        BufferedImage topperSource = concatHorizontal(parts[0], parts[1]);
                                        BufferedImage leftDrapeImg = parts[2]; // right slice -> left drape
                                        BufferedImage topperImg = topperSource;

                                        mc.execute(() -> {
                                            cachedSideTopper.put(key, registerTextureFromImage(key + "_topper", topperImg, true));
                                            cachedSideDrapeLeft.put(key, registerTextureFromImage(key + "_drape_left", leftDrapeImg, true));
                                            pendingSideTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process left drape with rotation: " + e.getMessage());
                        pendingSideTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        // Topper
        ps.pushPose();
        ps.translate(0, 1.02f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hw += 0.01f;
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Left drape only
        ps.pushPose();
        ps.translate(-bedWidth / 2f - 0.01f, 0.720f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XN.rotationDegrees(-180));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);

            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();
    }

    public static void renderImageRightDrapeOnlyFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // Similar to left drape but only render right side
        String key = url + "_split_right_only_" + bedWidth + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, rightDrapeTex;

        if (cachedAnimatedSideTopper.containsKey(key)) {
            topperTex = cachedAnimatedSideTopper.get(key).getCurrentFrame();
            rightDrapeTex = cachedAnimatedSideDrapeRight.get(key).getCurrentFrame();
        } else if (cachedSideTopper.containsKey(key)) {
            topperTex = cachedSideTopper.get(key);
            rightDrapeTex = cachedSideDrapeRight.get(key);
        } else if (pendingSideTransforms.contains(key)) {
            topperTex = rightDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = rightDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingSideTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeRightDrapes = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);

                                int W = rotatedFrame.getWidth(), H = rotatedFrame.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth; // match original proportions
                                if (totalWidthBlocks <= 0) continue;
                                int sliceW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                int midW = W - 2 * sliceW;
                                if (sliceW <= 0 || midW <= 0) continue;

                                // Split the rotated frame to maintain texture continuity
                                BufferedImage[] parts = splitThreeVertical(rotatedFrame, sliceW, midW, sliceW);
                                BufferedImage topperSource = concatHorizontal(parts[1], parts[2]); // keep full image on top when only right drape is shown
                                nativeToppers.add(toNativeImage(topperSource));
                                nativeRightDrapes.add(toNativeImage(parts[0])); // left slice -> right drape
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] rightLocs = uploadFrames(key + "_right_", nativeRightDrapes);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedSideDrapeRight.put(key, new AnimatedTexture(rightLocs, gif.delays, gif.totalDuration));
                                pendingSideTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);

                                int W = rotatedFull.getWidth(), H = rotatedFull.getHeight();
                                float totalWidthBlocks = bedWidth + 2f * drapeDepth;
                                if (totalWidthBlocks > 0) {
                                    int sliceW = Math.round(safeDiv(drapeDepth, totalWidthBlocks) * W);
                                    int midW = W - 2 * sliceW;
                                    if (sliceW > 0 && midW > 0) {
                                        // Split the rotated image to maintain texture continuity
                                        BufferedImage[] parts = splitThreeVertical(rotatedFull, sliceW, midW, sliceW);
                                        BufferedImage topperSource = concatHorizontal(parts[1], parts[2]);
                                        BufferedImage topperImg = topperSource;
                                        BufferedImage rightDrapeImg = parts[0]; // left slice -> right drape

                                        mc.execute(() -> {
                                            cachedSideTopper.put(key, registerTextureFromImage(key + "_topper", topperImg, true));
                                            cachedSideDrapeRight.put(key, registerTextureFromImage(key + "_drape_right", rightDrapeImg, true));
                                            pendingSideTransforms.remove(key);
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process right drape with rotation: " + e.getMessage());
                        pendingSideTransforms.remove(key);
                    }
                });
            }
        }

        // Render the parts
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        // Topper
        ps.pushPose();
        ps.translate(0, 1.02f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hw += 0.01f;
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Right drape only
        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);

            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();
    }

    public static void renderImageAllSideDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url, RotationConfig rotationConfig
    ) {
        // Render all 4 sides with seamless texture continuity
        // Split image into 5 parts: center topper + 4 drapes (left, right, front, back)
        String key = url + "_split_all_sides_" + bedWidth + "_" + bedLength + "_" + drapeDepth + "_rot_" +
                     (rotationConfig != null ? rotationConfig.getRotation() + "_" +
                      rotationConfig.isFlipHorizontal() + "_" + rotationConfig.isFlipVertical() : "0_false_false");

        ResourceLocation topperTex, leftDrapeTex, rightDrapeTex, frontDrapeTex, backDrapeTex;

        if (cachedAnimatedAllSidesToppers.containsKey(key)) {
            topperTex = cachedAnimatedAllSidesToppers.get(key).getCurrentFrame();
            leftDrapeTex = cachedAnimatedAllSidesLeft.get(key).getCurrentFrame();
            rightDrapeTex = cachedAnimatedAllSidesRight.get(key).getCurrentFrame();
            frontDrapeTex = cachedAnimatedAllSidesFront.get(key).getCurrentFrame();
            backDrapeTex = cachedAnimatedAllSidesBack.get(key).getCurrentFrame();
        } else if (cachedAllSidesToppers.containsKey(key)) {
            topperTex = cachedAllSidesToppers.get(key);
            leftDrapeTex = cachedAllSidesLeft.get(key);
            rightDrapeTex = cachedAllSidesRight.get(key);
            frontDrapeTex = cachedAllSidesFront.get(key);
            backDrapeTex = cachedAllSidesBack.get(key);
        } else if (pendingAllSidesTransforms.contains(key)) {
            topperTex = leftDrapeTex = rightDrapeTex = frontDrapeTex = backDrapeTex = LOADING_TEXTURE;
        } else {
            topperTex = leftDrapeTex = rightDrapeTex = frontDrapeTex = backDrapeTex = getOrLoadTexture(url);
            byte[] data = getRawDataFromCache(url);
            if (data != null) {
                pendingAllSidesTransforms.add(key);
                executor.execute(() -> {
                    try {
                        if (isGif(data)) {
                            DecodedGif gif = decodeGif(data);
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeLefts = new ArrayList<>();
                            List<NativeImage> nativeRights = new ArrayList<>();
                            List<NativeImage> nativeFronts = new ArrayList<>();
                            List<NativeImage> nativeBacks = new ArrayList<>();

                            for (BufferedImage frame : gif.frames) {
                                // Apply rotation to entire frame FIRST to maintain texture continuity
                                BufferedImage rotatedFrame = applyRotationToImage(frame, rotationConfig);
                                BufferedImage[] parts = splitImageForAllSides(rotatedFrame, bedWidth, bedLength, drapeDepth);
                                if (parts != null) {
                                    nativeToppers.add(toNativeImage(parts[0]));
                                    nativeLefts.add(toNativeImage(parts[1]));
                                    nativeRights.add(toNativeImage(parts[2]));
                                    // Match original front/back assignment: top strip -> back, bottom strip -> front
                                    nativeFronts.add(toNativeImage(parts[4]));
                                    nativeBacks.add(toNativeImage(parts[3]));
                                }
                            }

                            mc.execute(() -> {
                                ResourceLocation[] topperLocs = uploadFrames(key + "_topper_", nativeToppers);
                                ResourceLocation[] leftLocs = uploadFrames(key + "_left_", nativeLefts);
                                ResourceLocation[] rightLocs = uploadFrames(key + "_right_", nativeRights);
                                ResourceLocation[] frontLocs = uploadFrames(key + "_front_", nativeFronts);
                                ResourceLocation[] backLocs = uploadFrames(key + "_back_", nativeBacks);

                                cachedAnimatedAllSidesToppers.put(key, new AnimatedTexture(topperLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedAllSidesLeft.put(key, new AnimatedTexture(leftLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedAllSidesRight.put(key, new AnimatedTexture(rightLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedAllSidesFront.put(key, new AnimatedTexture(frontLocs, gif.delays, gif.totalDuration));
                                cachedAnimatedAllSidesBack.put(key, new AnimatedTexture(backLocs, gif.delays, gif.totalDuration));
                                pendingAllSidesTransforms.remove(key);
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            if (full != null) {
                                // Apply rotation to entire image FIRST to maintain texture continuity
                                BufferedImage rotatedFull = applyRotationToImage(full, rotationConfig);
                                BufferedImage[] parts = splitImageForAllSides(rotatedFull, bedWidth, bedLength, drapeDepth);
                                if (parts != null) {
                                    BufferedImage topperImg = parts[0];
                                    BufferedImage leftImg = parts[1];
                                    BufferedImage rightImg = parts[2];
                                    // Match original front/back assignment
                                    BufferedImage frontImg = parts[4];
                                    BufferedImage backImg = parts[3];

                                    mc.execute(() -> {
                                        cachedAllSidesToppers.put(key, registerTextureFromImage(key + "_topper", topperImg, true));
                                        cachedAllSidesLeft.put(key, registerTextureFromImage(key + "_left", leftImg, true));
                                        cachedAllSidesRight.put(key, registerTextureFromImage(key + "_right", rightImg, true));
                                        cachedAllSidesFront.put(key, registerTextureFromImage(key + "_front", frontImg, true));
                                        cachedAllSidesBack.put(key, registerTextureFromImage(key + "_back", backImg, true));
                                        pendingAllSidesTransforms.remove(key);
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process all sides drapes with rotation: " + e.getMessage());
                        pendingAllSidesTransforms.remove(key);
                    }
                });
            }
        }

        // Render all parts with proper positioning to avoid z-fighting
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        // Topper (center) - exact match to working front/back drapes
        float topperY = 1.02f;
        ps.pushPose();
        ps.translate(0, topperY, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(topperTex));
            float hw = bedWidth / 2f, hl = bedLength / 2f;
            hw += 0.01f; // bleed like working side drapes
            hl += 0.01f; // bleed like working front/back drapes
            Vector3f nUp = poseNormal(p, 0, 1, 0);
            Vector3f nDown = poseNormal(p, 0, -1, 0);
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
        }
        ps.popPose();

        // Left drape (extended to close corner seams without flaps)
        ps.pushPose();
        ps.translate(-bedWidth / 2f - 0.01f, 0.720f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.mulPose(Axis.XN.rotationDegrees(-180));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftDrapeTex));
            float hl = bedLength / 2f + 0.0105f; // Moderate extension to close seams
            float d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);
            // Use the same UVs as the working side-drapes renderer
            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();

        // Right drape (extended to close corner seams without flaps)
        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f + 0.0105f; // Moderate extension to close seams
            float d = drapeDepth;
            Vector3f fn = poseNormal(p, 0, 0, 1);
            Vector3f bn = poseNormal(p, 0, 0, -1);
            // Use the same UVs as the working side-drapes renderer
            drawDoubleSidedQuadUV(
                    p, buf, -hl, -d, hl, 0f,
                    1f, 0f,
                    1f, 1f,
                    0f, 1f,
                    0f, 0f,
                    uO, vO, uL, vL,
                    fn, bn
            );
        }
        ps.popPose();

        // Front drape (positioned to close corner seams)
        float frontDrapeZ = -bedLength / 2f - 0.01f;
        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f + 0.01f; // Balanced extension to meet side drapes
            float d = drapeDepth;
            Vector3f nOut = poseNormal(p, 0, 0, -1);
            Vector3f nIn = poseNormal(p, 0, 0, 1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nOut, nIn);
        }
        ps.popPose();

        // Back drape (positioned to close corner seams)
        float backDrapeZ = bedLength / 2f + 0.01f;
        ps.pushPose();
        ps.translate(0, topperY, backDrapeZ);
        ps.mulPose(Axis.XN.rotationDegrees(180));
        ps.translate(0, 0.30, 0);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(backDrapeTex));
            float hw = bedWidth / 2f + 0.01f; // Balanced extension to meet side drapes
            float d = drapeDepth;
            Vector3f nFront = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nFront, nBack);
        }
        ps.popPose();

        // Corner pieces are no longer needed since drapes now overlap properly
        // renderCornerPieces(ps, bufSrc, packedLight, packedOverlay, bedWidth, bedLength, drapeDepth,
        //                   leftDrapeTex, rightDrapeTex, frontDrapeTex, backDrapeTex, topperY);
    }

    /**
     * Renders small corner pieces to close the seams where drapes meet at corners
     */
    private static void renderCornerPieces(PoseStack ps, MultiBufferSource bufSrc,
                                         int packedLight, int packedOverlay,
                                         float bedWidth, float bedLength, float drapeDepth,
                                         ResourceLocation leftTex, ResourceLocation rightTex,
                                         ResourceLocation frontTex, ResourceLocation backTex,
                                         float topperY) {
        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        float cornerSize = 0.02f; // Small corner piece size
        float hw = bedWidth / 2f;
        float hl = bedLength / 2f;

        // Front-left corner
        ps.pushPose();
        ps.translate(-hw - cornerSize/2, topperY - drapeDepth/2, -hl - cornerSize/2);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftTex));
            Vector3f n = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -cornerSize, -cornerSize, cornerSize, cornerSize,
                              0f, 0f, 1f, 1f, uO, vO, uL, vL, n, nBack);
        }
        ps.popPose();

        // Front-right corner
        ps.pushPose();
        ps.translate(hw + cornerSize/2, topperY - drapeDepth/2, -hl - cornerSize/2);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightTex));
            Vector3f n = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -cornerSize, -cornerSize, cornerSize, cornerSize,
                              0f, 0f, 1f, 1f, uO, vO, uL, vL, n, nBack);
        }
        ps.popPose();

        // Back-left corner
        ps.pushPose();
        ps.translate(-hw - cornerSize/2, topperY - drapeDepth/2, hl + cornerSize/2);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(leftTex));
            Vector3f n = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -cornerSize, -cornerSize, cornerSize, cornerSize,
                              0f, 0f, 1f, 1f, uO, vO, uL, vL, n, nBack);
        }
        ps.popPose();

        // Back-right corner
        ps.pushPose();
        ps.translate(hw + cornerSize/2, topperY - drapeDepth/2, hl + cornerSize/2);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightTex));
            Vector3f n = poseNormal(p, 0, 0, 1);
            Vector3f nBack = poseNormal(p, 0, 0, -1);
            drawDoubleSidedQuad(p, buf, -cornerSize, -cornerSize, cornerSize, cornerSize,
                              0f, 0f, 1f, 1f, uO, vO, uL, vL, n, nBack);
        }
        ps.popPose();
    }

    // Utility to upload multiple NativeImage frames to DynamicTexture(s)
    private static ResourceLocation[] uploadFrames(String baseKey, List<NativeImage> frames) {
        ResourceLocation[] locs = new ResourceLocation[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            DynamicTexture dyn = new DynamicTexture(frames.get(i));
            locs[i] = mc.getTextureManager().register("dynamic/" + sanitize(baseKey + i), dyn);
        }
        return locs;
    }
}
