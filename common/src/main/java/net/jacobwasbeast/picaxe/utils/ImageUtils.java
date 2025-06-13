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
import org.joml.Matrix4f;
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
import java.awt.image.RasterFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_LOAD_TRIES = 3;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    private static final Set<String> blacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> loading = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, SoftReference<byte[]>> rawDataCache = new ConcurrentHashMap<>();

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

    static {
        BufferedImage notFoundImage = loadBufferedImageFromResource("picaxe", "textures/blocks/notfound.png");
        BufferedImage loadingImage = loadBufferedImageFromResource("picaxe", "textures/blocks/loadingimage.png");

        BufferedImage combinedLoadingImage = new BufferedImage(
                notFoundImage.getWidth() + loadingImage.getWidth(),
                Math.max(notFoundImage.getHeight(), loadingImage.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinedLoadingImage.createGraphics();
        g.drawImage(loadingImage, 0, 0, null);
        g.drawImage(notFoundImage, loadingImage.getWidth(), 0, null);
        g.dispose();

        NOT_FOUND_TEXTURE = registerTextureFromImage("internal:notfound", notFoundImage, true);
        LOADING_TEXTURE = registerTextureFromImage("internal:loading", combinedLoadingImage, true);
    }

    private static BufferedImage loadBufferedImageFromResource(String namespace, String path) {
        try {
            ResourceLocation imageLoc = ResourceLocation.tryBuild(namespace, path);
            InputStream in = mc.getResourceManager().getResource(imageLoc).get().open();
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load resource image: " + namespace + ":" + path + " - " + e.getMessage());
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private static String sanitize(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static NativeImage toNativeImage(BufferedImage img) {
        if (img == null) return null;
        int width = img.getWidth();
        int height = img.getHeight();

        BufferedImage convertedImg = img;
        if (img.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            convertedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            convertedImg.getGraphics().drawImage(img, 0, 0, null);
        }

        int[] argbPixels = ((DataBufferInt) convertedImg.getRaster().getDataBuffer()).getData();

        NativeImage ni = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = argbPixels[(y * width) + (width - 1 - x)];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
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
        if (shouldCache && cachedTextures.containsKey(key)) {
            return cachedTextures.get(key);
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

    private static void loadAnimatedGif(String url, byte[] data) throws IOException {
        List<NativeImage> nativeFrames = new ArrayList<>();
        int[] delayMillis;
        int totalDuration = 0;

        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("No GIF ImageReader found");
            ImageReader reader = readers.next();
            reader.setInput(stream);

            int numFrames = reader.getNumImages(true);
            delayMillis = new int[numFrames];
            BufferedImage canvas = null;
            Graphics2D g = null;

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frameImage = reader.read(i);
                if (canvas == null) {
                    canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                    g = canvas.createGraphics();
                    g.setBackground(new Color(0, 0, 0, 0));
                }

                IIOMetadata metadata = reader.getImageMetadata(i);
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

                NodeList gces = root.getElementsByTagName("GraphicControlExtension");
                int delay = 10;
                String disposal = "none";
                if (gces.getLength() > 0) {
                    IIOMetadataNode gce = (IIOMetadataNode) gces.item(0);
                    delay = Integer.parseInt(gce.getAttribute("delayTime"));
                    disposal = gce.getAttribute("disposalMethod");
                }
                delayMillis[i] = delay * 10 > 0 ? delay * 10 : 100;
                totalDuration += delayMillis[i];

                IIOMetadataNode imageDescriptor = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                int x = Integer.parseInt(imageDescriptor.getAttribute("imageLeftPosition"));
                int y = Integer.parseInt(imageDescriptor.getAttribute("imageTopPosition"));

                g.drawImage(frameImage, x, y, null);

                BufferedImage finalFrame = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                finalFrame.createGraphics().drawImage(canvas, 0, 0, null);
                nativeFrames.add(toNativeImage(finalFrame));

                if (disposal.equals("restoreToBackgroundColor")) {
                    g.clearRect(x, y, frameImage.getWidth(), frameImage.getHeight());
                }
            }
            if (g != null) g.dispose();
        }

        final int[] finalDelays = delayMillis;
        final int finalTotalDuration = totalDuration;
        mc.execute(() -> {
            ResourceLocation[] frameLocations = new ResourceLocation[nativeFrames.size()];
            for (int j = 0; j < nativeFrames.size(); j++) {
                NativeImage ni = nativeFrames.get(j);
                if (ni != null) {
                    DynamicTexture dyn = new DynamicTexture(ni);
                    frameLocations[j] = mc.getTextureManager().register("dynamic/" + sanitize(url + "_frame_" + j), dyn);
                } else {
                    frameLocations[j] = NOT_FOUND_TEXTURE;
                }
            }
            nativeFrames.clear();
            cachedAnimatedTextures.put(url, new AnimatedTexture(frameLocations, finalDelays, finalTotalDuration));
        });
    }

    private static boolean isGif(byte[] bytes) {
        if (bytes == null || bytes.length < 3) return false;
        return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
    }

    public static ResourceLocation getOrLoadTexture(String url) {
        if (url == null || url.isEmpty()) {
            return NOT_FOUND_TEXTURE;
        }
        if (cachedAnimatedTextures.containsKey(url)) {
            return cachedAnimatedTextures.get(url).getCurrentFrame();
        }
        if (cachedTextures.containsKey(url)) {
            return cachedTextures.get(url);
        }
        if (blacklist.contains(url)) {
            return NOT_FOUND_TEXTURE;
        }
        if (loading.contains(url)) {
            return LOADING_TEXTURE;
        }

        loading.add(url);
        executor.submit(() -> {
            try {
                processUrl(url);
            } finally {
                loading.remove(url);
            }
        });

        return LOADING_TEXTURE;
    }

    private static byte[] getRawDataFromCache(String url) {
        SoftReference<byte[]> ref = rawDataCache.get(url);
        return (ref != null) ? ref.get() : null;
    }

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
                executor.submit(() -> {
                    try {
                        if (isGif(data)) {
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeLefts = new ArrayList<>();
                            List<NativeImage> nativeRights = new ArrayList<>();
                            final int[] delays;
                            final int totalDuration;

                            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                                if (!readers.hasNext()) throw new IOException("No GIF reader");
                                ImageReader reader = readers.next();
                                reader.setInput(stream);
                                int numFrames = reader.getNumImages(true);
                                delays = new int[numFrames];
                                int duration = 0;
                                BufferedImage canvas = null;
                                Graphics2D g = null;

                                for (int i = 0; i < numFrames; i++) {
                                    BufferedImage frameImage = reader.read(i);
                                    if (canvas == null) {
                                        canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                                        g = canvas.createGraphics();
                                    }

                                    IIOMetadata metadata = reader.getImageMetadata(i);
                                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                                    IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                                    delays[i] = Integer.parseInt(gce.getAttribute("delayTime")) * 10;
                                    if (delays[i] <= 0) delays[i] = 100;
                                    duration += delays[i];
                                    String disposal = gce.getAttribute("disposalMethod");

                                    IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                                    int x = Integer.parseInt(desc.getAttribute("imageLeftPosition"));
                                    int y = Integer.parseInt(desc.getAttribute("imageTopPosition"));
                                    g.drawImage(frameImage, x, y, null);

                                    BufferedImage currentFrameToSplit = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                    currentFrameToSplit.createGraphics().drawImage(canvas, 0, 0, null);

                                    int W = currentFrameToSplit.getWidth(), H = currentFrameToSplit.getHeight();
                                    float totalWidthBlocks = bedWidth + 2 * drapeDepth;
                                    if (totalWidthBlocks <= 0) continue;
                                    int leftW = Math.round((drapeDepth / totalWidthBlocks) * W);
                                    int midW = W - (2 * leftW);
                                    if (leftW <= 0 || midW <= 0 || (leftW + midW) > W) continue;

                                    nativeRights.add(toNativeImage(currentFrameToSplit.getSubimage(0, 0, leftW, H)));
                                    nativeToppers.add(toNativeImage(currentFrameToSplit.getSubimage(leftW, 0, midW, H)));
                                    nativeLefts.add(toNativeImage(currentFrameToSplit.getSubimage(leftW + midW, 0, leftW, H)));

                                    if (disposal.equals("restoreToBackgroundColor")) {
                                        g.clearRect(x, y, frameImage.getWidth(), frameImage.getHeight());
                                    }
                                }
                                totalDuration = duration;
                                if (g != null) g.dispose();
                            }

                            mc.execute(() -> {
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] leftLocs = nativeLefts.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_left_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] rightLocs = nativeRights.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_right_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, delays, totalDuration));
                                cachedAnimatedSideDrapeLeft.put(key, new AnimatedTexture(leftLocs, delays, totalDuration));
                                cachedAnimatedSideDrapeRight.put(key, new AnimatedTexture(rightLocs, delays, totalDuration));
                                nativeToppers.clear();
                                nativeLefts.clear();
                                nativeRights.clear();
                            });

                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            int W = full.getWidth(), H = full.getHeight();
                            float totalWidthBlocks = bedWidth + 2 * drapeDepth;
                            if (totalWidthBlocks <= 0) return;
                            int leftW = Math.round((drapeDepth / totalWidthBlocks) * W);
                            int rightW = leftW;
                            int midW = W - leftW - rightW;
                            if (leftW <= 0 || rightW <= 0 || midW <= 0 || (leftW + midW) > W) return;

                            BufferedImage intendedRightDrapeImg = full.getSubimage(0, 0, leftW, H);
                            BufferedImage midImg = full.getSubimage(leftW, 0, midW, H);
                            BufferedImage intendedLeftDrapeImg = full.getSubimage(leftW + midW, 0, rightW, H);

                            mc.execute(() -> {
                                cachedSideDrapeLeft.put(key, registerTextureFromImage(key + "_drape_left", intendedLeftDrapeImg, true));
                                cachedSideTopper.put(key, registerTextureFromImage(key + "_topper", midImg, true));
                                cachedSideDrapeRight.put(key, registerTextureFromImage(key + "_drape_right", intendedRightDrapeImg, true));
                            });
                        }
                    } catch (IOException | RasterFormatException e) {
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
                executor.submit(() -> {
                    try {
                        if (isGif(data)) {
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeFronts = new ArrayList<>();
                            List<NativeImage> nativeBacks = new ArrayList<>();
                            final int[] delays;
                            final int totalDuration;

                            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                                if (!readers.hasNext()) throw new IOException("No GIF reader");
                                ImageReader reader = readers.next();
                                reader.setInput(stream);
                                int numFrames = reader.getNumImages(true);
                                delays = new int[numFrames];
                                int duration = 0;
                                BufferedImage canvas = null;
                                Graphics2D g = null;

                                for (int i = 0; i < numFrames; i++) {
                                    BufferedImage frameImage = reader.read(i);
                                    if (canvas == null) {
                                        canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                                        g = canvas.createGraphics();
                                    }

                                    IIOMetadata metadata = reader.getImageMetadata(i);
                                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                                    IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                                    delays[i] = Integer.parseInt(gce.getAttribute("delayTime")) * 10;
                                    if (delays[i] <= 0) delays[i] = 100;
                                    duration += delays[i];
                                    String disposal = gce.getAttribute("disposalMethod");

                                    IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                                    int x = Integer.parseInt(desc.getAttribute("imageLeftPosition"));
                                    int y = Integer.parseInt(desc.getAttribute("imageTopPosition"));
                                    g.drawImage(frameImage, x, y, null);

                                    BufferedImage currentFrameToSplit = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                    currentFrameToSplit.createGraphics().drawImage(canvas, 0, 0, null);

                                    int W = currentFrameToSplit.getWidth(), H = currentFrameToSplit.getHeight();
                                    float totalLengthBlocks = bedLength + 2 * drapeDepth;
                                    if (totalLengthBlocks <= 0) continue;
                                    int frontH = Math.round((drapeDepth / totalLengthBlocks) * H);
                                    int midH = H - (2 * frontH);
                                    if (frontH <= 0 || midH <= 0 || (frontH + midH > H)) continue;

                                    nativeBacks.add(toNativeImage(currentFrameToSplit.getSubimage(0, 0, W, frontH)));
                                    nativeToppers.add(toNativeImage(currentFrameToSplit.getSubimage(0, frontH, W, midH)));
                                    nativeFronts.add(toNativeImage(currentFrameToSplit.getSubimage(0, frontH + midH, W, frontH)));

                                    if (disposal.equals("restoreToBackgroundColor")) {
                                        g.clearRect(x, y, frameImage.getWidth(), frameImage.getHeight());
                                    }
                                }
                                totalDuration = duration;
                                if (g != null) g.dispose();
                            }

                            mc.execute(() -> {
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] frontLocs = nativeFronts.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_front_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] backLocs = nativeBacks.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_back_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);

                                cachedAnimatedFrontBackTopper.put(key, new AnimatedTexture(topperLocs, delays, totalDuration));
                                cachedAnimatedFrontDrape.put(key, new AnimatedTexture(frontLocs, delays, totalDuration));
                                cachedAnimatedBackDrape.put(key, new AnimatedTexture(backLocs, delays, totalDuration));
                                nativeToppers.clear();
                                nativeFronts.clear();
                                nativeBacks.clear();
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            int W = full.getWidth(), H = full.getHeight();
                            float totalLengthBlocks = bedLength + 2 * drapeDepth;
                            if (totalLengthBlocks <= 0) return;
                            int frontH = Math.round((drapeDepth / totalLengthBlocks) * H);
                            int backH = frontH;
                            int midH = H - frontH - backH;
                            if (frontH <= 0 || backH <= 0 || midH <= 0 || (frontH + midH) > H) return;

                            BufferedImage intendedBackDrapeImg = full.getSubimage(0, 0, W, frontH);
                            BufferedImage midImg = full.getSubimage(0, frontH, W, midH);
                            BufferedImage intendedFrontDrapeImg = full.getSubimage(0, frontH + midH, W, backH);

                            mc.execute(() -> {
                                cachedFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", intendedFrontDrapeImg, true));
                                cachedFrontBackTopper.put(key, registerTextureFromImage(key + "_topper_frontback", midImg, true));
                                cachedBackDrape.put(key, registerTextureFromImage(key + "_drape_back", intendedBackDrapeImg, true));
                            });
                        }
                    } catch (IOException | RasterFormatException e) {
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
                executor.submit(() -> {
                    try {
                        if (isGif(data)) {
                            List<NativeImage> nativeToppers = new ArrayList<>();
                            List<NativeImage> nativeDrapes = new ArrayList<>();
                            final int[] delays;
                            final int totalDuration;

                            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                                if (!readers.hasNext()) throw new IOException("No GIF reader");
                                ImageReader reader = readers.next();
                                reader.setInput(stream);
                                int numFrames = reader.getNumImages(true);
                                delays = new int[numFrames];
                                int duration = 0;
                                BufferedImage canvas = null;
                                Graphics2D g = null;

                                for (int i = 0; i < numFrames; i++) {
                                    BufferedImage frameImage = reader.read(i);
                                    if (canvas == null) {
                                        canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                                        g = canvas.createGraphics();
                                    }

                                    IIOMetadata metadata = reader.getImageMetadata(i);
                                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                                    IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                                    delays[i] = Integer.parseInt(gce.getAttribute("delayTime")) * 10;
                                    if (delays[i] <= 0) delays[i] = 100;
                                    duration += delays[i];
                                    String disposal = gce.getAttribute("disposalMethod");

                                    IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                                    int x = Integer.parseInt(desc.getAttribute("imageLeftPosition"));
                                    int y = Integer.parseInt(desc.getAttribute("imageTopPosition"));
                                    g.drawImage(frameImage, x, y, null);

                                    BufferedImage currentFrameToSplit = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                    currentFrameToSplit.createGraphics().drawImage(canvas, 0, 0, null);

                                    int W = currentFrameToSplit.getWidth(), H = currentFrameToSplit.getHeight();
                                    float totalLengthBlocks = bedLength + drapeDepth;
                                    if (totalLengthBlocks <= 0) continue;
                                    int drapeH = Math.round((drapeDepth / totalLengthBlocks) * H);
                                    int topperH = H - drapeH;
                                    if (drapeH <= 0 || topperH <= 0) continue;

                                    nativeToppers.add(toNativeImage(currentFrameToSplit.getSubimage(0, 0, W, topperH)));
                                    nativeDrapes.add(toNativeImage(currentFrameToSplit.getSubimage(0, topperH, W, drapeH)));

                                    if (disposal.equals("restoreToBackgroundColor")) {
                                        g.clearRect(x, y, frameImage.getWidth(), frameImage.getHeight());
                                    }
                                }
                                totalDuration = duration;
                                if (g != null) g.dispose();
                            }
                            mc.execute(() -> {
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] drapeLocs = nativeDrapes.stream().map(ni -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_drape_" + index.getAndIncrement()), new DynamicTexture(ni))).toArray(ResourceLocation[]::new);

                                cachedAnimatedSingleTopper.put(key, new AnimatedTexture(topperLocs, delays, totalDuration));
                                cachedAnimatedSingleFrontDrape.put(key, new AnimatedTexture(drapeLocs, delays, totalDuration));
                                nativeToppers.clear();
                                nativeDrapes.clear();
                            });
                        } else {
                            BufferedImage full = ImageIO.read(new ByteArrayInputStream(data));
                            int W = full.getWidth(), H = full.getHeight();
                            float totalLengthBlocks = bedLength + drapeDepth;
                            if (totalLengthBlocks <= 0) return;
                            int drapeH = Math.round((drapeDepth / totalLengthBlocks) * H);
                            int topperH = H - drapeH;
                            if (drapeH <= 0 || topperH <= 0) return;

                            BufferedImage topperImg = full.getSubimage(0, 0, W, topperH);
                            BufferedImage drapeImg = full.getSubimage(0, topperH, W, drapeH);

                            mc.execute(() -> {
                                cachedSingleFrontDrape.put(key, registerTextureFromImage(key + "_drape_front", drapeImg, true));
                                cachedSingleTopper.put(key, registerTextureFromImage(key + "_topper_front", topperImg, true));
                            });
                        }
                    } catch (IOException | RasterFormatException e) {
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
}
