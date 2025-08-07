package net.jacobwasbeast.picaxe.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
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
        for (int i = 1; i <= MAX_LOAD_TRIES; i++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "Picaxe-ImageUtils");
                conn.connect();

                int code = conn.getResponseCode();
                if (code >= 400) throw new IOException("HTTP " + code);

                int contentLength = conn.getContentLength();
                ByteArrayOutputStream bout = new ByteArrayOutputStream(contentLength > 0 ? contentLength : 8192);
                try (InputStream in = conn.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) bout.write(buf, 0, r);
                }
                byte[] imageBytes = bout.toByteArray();
                rawDataCache.put(url, new SoftReference<>(imageBytes));

                if (isGif(imageBytes)) {
                    loadAnimatedGif(url, imageBytes);
                } else {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (image == null) throw new IOException("ImageIO.read returned null");
                    mc.execute(() -> registerTextureFromImage(url, image, true));
                }
                return;
            } catch (Exception e) {
                System.err.println("Attempt " + i + "/" + MAX_LOAD_TRIES + " failed to load " + url + ": " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
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
        // Front
        buf.addVertex(p.pose(), x0, y0, 0f).setColor(255, 255, 255, 255).setUv(u0, v1).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x1, y0, 0f).setColor(255, 255, 255, 255).setUv(u1, v1).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x1, y1, 0f).setColor(255, 255, 255, 255).setUv(u1, v0).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x0, y1, 0f).setColor(255, 255, 255, 255).setUv(u0, v0).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());

        // Back
        buf.addVertex(p.pose(), x0, y1, 0f).setColor(255, 255, 255, 255).setUv(u0, v0).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x1, y1, 0f).setColor(255, 255, 255, 255).setUv(u1, v0).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x1, y0, 0f).setColor(255, 255, 255, 255).setUv(u1, v1).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x0, y0, 0f).setColor(255, 255, 255, 255).setUv(u0, v1).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
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
        // Front (x0,y0) -> (u00,v00), etc.
        buf.addVertex(p.pose(), x0, y0, 0f).setColor(255,255,255,255).setUv(u00, v00).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x1, y0, 0f).setColor(255,255,255,255).setUv(u10, v10).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x1, y1, 0f).setColor(255,255,255,255).setUv(u11, v11).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());
        buf.addVertex(p.pose(), x0, y1, 0f).setColor(255,255,255,255).setUv(u01, v01).setUv1(uO, vO).setUv2(uL, vL).setNormal(nFront.x(), nFront.y(), nFront.z());

        // Back uses opposite vertex order with same UVs assigned to corresponding corners
        buf.addVertex(p.pose(), x0, y1, 0f).setColor(255,255,255,255).setUv(u01, v01).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x1, y1, 0f).setColor(255,255,255,255).setUv(u11, v11).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x1, y0, 0f).setColor(255,255,255,255).setUv(u10, v10).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
        buf.addVertex(p.pose(), x0, y0, 0f).setColor(255,255,255,255).setUv(u00, v00).setUv1(uO, vO).setUv2(uL, vL).setNormal(nBack.x(), nBack.y(), nBack.z());
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
        renderImageFromURL(ps, bufSrc, packedLight, packedOverlay, partialTick, width, height, url, keepAspectRatio, false, false);
    }

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url, boolean keepAspectRatio, boolean flipX, boolean flipY
    ) {
        ResourceLocation tex = getOrLoadTexture(url);
        if (tex == null) tex = NOT_FOUND_TEXTURE;

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

        Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
        Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());

        drawDoubleSidedQuad(p, buf,
                -hw, -hh, hw, hh,
                u0, v0, u1, v1,
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
            hw += 0.01f; // bleed
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());
            drawDoubleSidedQuad(p, buf, -hw, -hl, hw, hl, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nUp, nDown);
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
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());

            // Original mapping:
            // (x0,y0)->(1,0), (x1,y0)->(1,1), (x1,y1)->(0,1), (x0,y1)->(0,0)
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

        // Right drape (same UV pattern as original code)
        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());

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
            hl += 0.01f; // bleed
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());
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
            Vector3f nOut = p.transformNormal(0, 0, -1, new Vector3f());
            Vector3f nIn = p.transformNormal(0, 0, 1, new Vector3f());
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
            Vector3f nFront = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f nBack = p.transformNormal(0, 0, -1, new Vector3f());
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nFront, nBack);
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
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());
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
            Vector3f nOut = p.transformNormal(0, 0, -1, new Vector3f());
            Vector3f nIn = p.transformNormal(0, 0, 1, new Vector3f());
            drawDoubleSidedQuad(p, buf, -hw, -d, hw, 0f, 0f, 0f, 1f, 1f, uO, vO, uL, vL, nOut, nIn);
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