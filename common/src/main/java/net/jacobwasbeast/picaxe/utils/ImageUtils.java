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
import java.awt.image.RasterFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ImageUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_LOAD_TRIES = 3;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final Set<String> blacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> loading = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, byte[]> rawDataCache = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedTextures = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedTextures = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedSideTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeLeft = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideDrapeLeft = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeRight = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSideDrapeRight = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedFrontBackTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedFrontBackTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedBackDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedBackDrape = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> cachedSingleFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSingleFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSingleTopper = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTexture> cachedAnimatedSingleTopper = new ConcurrentHashMap<>();


    private static final ResourceLocation NOT_FOUND_TEXTURE;
    private static final ResourceLocation LOADING_TEXTURE;

    private record AnimatedTexture(ResourceLocation[] frames, int[] delays, int totalDuration) {
        public ResourceLocation getCurrentFrame() {
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
        java.awt.Graphics2D g = combinedLoadingImage.createGraphics();
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

        BufferedImage convertedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        convertedImg.getGraphics().drawImage(img, 0, 0, null);

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
        for (int i = 1; i <= MAX_LOAD_TRIES; i++) {
            try (InputStream in = new URL(url).openStream()) {
                byte[] imageBytes = in.readAllBytes();
                rawDataCache.put(url, imageBytes);

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
            }
        }
        System.err.println("Gave up loading image, adding to blacklist: " + url);
        blacklist.add(url);
    }

    private static void loadAnimatedGif(String url, byte[] data) throws IOException {
        List<BufferedImage> finalFrames = getAnimationFrames(data);
        int[] delayMillis;
        int totalDuration = 0;

        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            ImageReader reader = ImageIO.getImageReaders(stream).next();
            reader.setInput(stream);
            int numFrames = reader.getNumImages(true);
            delayMillis = new int[numFrames];
            for (int i = 0; i < numFrames; i++) {
                IIOMetadata metadata = reader.getImageMetadata(i);
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                NodeList gces = root.getElementsByTagName("GraphicControlExtension");
                int delay = 10;
                if (gces.getLength() > 0) {
                    delay = Integer.parseInt(((IIOMetadataNode) gces.item(0)).getAttribute("delayTime"));
                }
                delayMillis[i] = delay * 10 > 0 ? delay * 10 : 100;
                totalDuration += delayMillis[i];
            }
        }

        final int[] finalDelays = delayMillis;
        final int finalTotalDuration = totalDuration;
        mc.execute(() -> {
            ResourceLocation[] frameLocations = new ResourceLocation[finalFrames.size()];
            for (int j = 0; j < finalFrames.size(); j++) {
                frameLocations[j] = registerTextureFromImage(url + "_frame_" + j, finalFrames.get(j), true);
            }
            cachedAnimatedTextures.put(url, new AnimatedTexture(frameLocations, finalDelays, finalTotalDuration));
        });
    }

    private static List<BufferedImage> getAnimationFrames(byte[] data) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("No GIF reader found");

            ImageReader reader = readers.next();
            reader.setInput(stream);

            int numFrames = reader.getNumImages(true);
            List<BufferedImage> finalFrames = new ArrayList<>(numFrames);
            BufferedImage canvas = null;
            Graphics2D g = null;

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frameImage = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                IIOMetadataNode imageDescriptor = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                int x = Integer.parseInt(imageDescriptor.getAttribute("imageLeftPosition"));
                int y = Integer.parseInt(imageDescriptor.getAttribute("imageTopPosition"));

                if (canvas == null) {
                    canvas = new BufferedImage(reader.getWidth(0), reader.getHeight(0), BufferedImage.TYPE_INT_ARGB);
                    g = canvas.createGraphics();
                    g.setBackground(new Color(0, 0, 0, 0));
                }

                g.drawImage(frameImage, x, y, null);

                BufferedImage finalFrame = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                finalFrame.createGraphics().drawImage(canvas, 0, 0, null);
                finalFrames.add(finalFrame);

                NodeList gces = root.getElementsByTagName("GraphicControlExtension");
                if (gces.getLength() > 0) {
                    IIOMetadataNode gce = (IIOMetadataNode) gces.item(0);
                    if (gce.getAttribute("disposalMethod").equals("restoreToBackgroundColor")) {
                        g.clearRect(x, y, frameImage.getWidth(), frameImage.getHeight());
                    }
                }
            }
            if (g != null) g.dispose();
            return finalFrames;
        }
    }

    private static boolean isGif(byte[] bytes) {
        if (bytes.length < 3) return false;
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

        Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
        Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());

        buf.addVertex(p.pose(), -hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());
        buf.addVertex(p.pose(), hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());
        buf.addVertex(p.pose(), hw, hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL);
        buf.addVertex(p.pose(), -hw, hh, 0f)
                .setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());

        buf.addVertex(p.pose(), -hw, hh, 0f)
                .setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nDown.x(), nDown.y(), nDown.z());
        buf.addVertex(p.pose(), hw, hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nDown.x(), nDown.y(), nDown.z());
        buf.addVertex(p.pose(), hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nDown.x(), nDown.y(), nDown.z());
        buf.addVertex(p.pose(), -hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nDown.x(), nDown.y(), nDown.z());
        ps.popPose();
    }

    public static void renderImageSideDrapesFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick,
            float bedWidth, float bedLength, float drapeDepth,
            String url
    ) {
        String key = url + "_split_" + bedLength + "_" + drapeDepth;
        ResourceLocation topperTex, leftDrapeTex, rightDrapeTex;

        if (cachedAnimatedSideTopper.containsKey(key)) {
            topperTex = cachedAnimatedSideTopper.get(key).getCurrentFrame();
            leftDrapeTex = cachedAnimatedSideDrapeLeft.get(key).getCurrentFrame();
            rightDrapeTex = cachedAnimatedSideDrapeRight.get(key).getCurrentFrame();
        } else if (cachedSideTopper.containsKey(key)) {
            topperTex = cachedSideTopper.get(key);
            leftDrapeTex = cachedSideDrapeLeft.get(key);
            rightDrapeTex = cachedSideDrapeRight.get(key);
        } else {
            topperTex = leftDrapeTex = rightDrapeTex = getOrLoadTexture(url);
            if (rawDataCache.containsKey(url)) {
                executor.submit(() -> {
                    try {
                        byte[] data = rawDataCache.get(url);
                        if (isGif(data)) {
                            List<BufferedImage> frames = getAnimationFrames(data);
                            int W = frames.get(0).getWidth(), H = frames.get(0).getHeight();
                            float totalWidthBlocks = bedWidth + 2 * drapeDepth;
                            if (totalWidthBlocks <= 0) return;

                            int leftW = Math.round((drapeDepth / totalWidthBlocks) * W);
                            int midW = W - (2 * leftW);
                            if (leftW <= 0 || midW <= 0) return;

                            List<NativeImage> nativeToppers = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(leftW, 0, midW, H))).filter(Objects::nonNull).collect(Collectors.toList());
                            List<NativeImage> nativeLefts = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(leftW + midW, 0, leftW, H))).filter(Objects::nonNull).collect(Collectors.toList());
                            List<NativeImage> nativeRights = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, 0, leftW, H))).filter(Objects::nonNull).collect(Collectors.toList());

                            mc.execute(() -> {
                                AnimatedTexture originalAnim = cachedAnimatedTextures.get(url);
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] leftLocs = nativeLefts.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_left_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] rightLocs = nativeRights.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_right_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);

                                cachedAnimatedSideTopper.put(key, new AnimatedTexture(topperLocs, originalAnim.delays, originalAnim.totalDuration));
                                cachedAnimatedSideDrapeLeft.put(key, new AnimatedTexture(leftLocs, originalAnim.delays, originalAnim.totalDuration));
                                cachedAnimatedSideDrapeRight.put(key, new AnimatedTexture(rightLocs, originalAnim.delays, originalAnim.totalDuration));
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
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());

            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
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
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());

            buf.addVertex(p.pose(), -hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), -hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
        }
        ps.popPose();

        ps.pushPose();
        ps.translate(bedWidth / 2f + 0.01f, 1.020f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(rightDrapeTex));
            float hl = bedLength / 2f, d = drapeDepth;
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());

            buf.addVertex(p.pose(), -hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hl, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), -hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
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
        } else {
            topperTex = frontDrapeTex = backDrapeTex = getOrLoadTexture(url);
            if (rawDataCache.containsKey(url)) {
                executor.submit(() -> {
                    try {
                        byte[] data = rawDataCache.get(url);
                        if (isGif(data)) {
                            List<BufferedImage> frames = getAnimationFrames(data);
                            int W = frames.get(0).getWidth(), H = frames.get(0).getHeight();
                            float totalLengthBlocks = bedLength + 2 * drapeDepth;
                            if (totalLengthBlocks <= 0) return;

                            int frontH = Math.round((drapeDepth / totalLengthBlocks) * H);
                            int midH = H - (2 * frontH);
                            if (frontH <= 0 || midH <= 0) return;

                            List<NativeImage> nativeToppers = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, frontH, W, midH))).filter(Objects::nonNull).collect(Collectors.toList());
                            List<NativeImage> nativeFronts = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, frontH + midH, W, frontH))).filter(Objects::nonNull).collect(Collectors.toList());
                            List<NativeImage> nativeBacks = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, 0, W, frontH))).filter(Objects::nonNull).collect(Collectors.toList());

                            mc.execute(() -> {
                                AnimatedTexture originalAnim = cachedAnimatedTextures.get(url);
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] frontLocs = nativeFronts.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_front_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] backLocs = nativeBacks.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_back_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);

                                cachedAnimatedFrontBackTopper.put(key, new AnimatedTexture(topperLocs, originalAnim.delays, originalAnim.totalDuration));
                                cachedAnimatedFrontDrape.put(key, new AnimatedTexture(frontLocs, originalAnim.delays, originalAnim.totalDuration));
                                cachedAnimatedBackDrape.put(key, new AnimatedTexture(backLocs, originalAnim.delays, originalAnim.totalDuration));
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
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());
            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
        }
        ps.popPose();

        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
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
            Vector3f fn = p.transformNormal(0, 0, 1, new Vector3f());
            Vector3f bn = p.transformNormal(0, 0, -1, new Vector3f());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
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
        } else {
            topperTex = frontDrapeTex = getOrLoadTexture(url);

            if (rawDataCache.containsKey(url)) {
                executor.submit(() -> {
                    try {
                        byte[] data = rawDataCache.get(url);
                        if (isGif(data)) {
                            List<BufferedImage> frames = getAnimationFrames(data);
                            int W = frames.get(0).getWidth(), H = frames.get(0).getHeight();
                            float totalLengthBlocks = bedLength + drapeDepth;
                            if (totalLengthBlocks <= 0) return;

                            int drapeH = Math.round((drapeDepth / totalLengthBlocks) * H);
                            int topperH = H - drapeH;
                            if (drapeH <= 0 || topperH <= 0) return;

                            List<NativeImage> nativeToppers = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, 0, W, topperH))).filter(Objects::nonNull).collect(Collectors.toList());
                            List<NativeImage> nativeDrapes = frames.parallelStream().map(f -> toNativeImage(f.getSubimage(0, topperH, W, drapeH))).filter(Objects::nonNull).collect(Collectors.toList());

                            mc.execute(() -> {
                                AnimatedTexture originalAnim = cachedAnimatedTextures.get(url);
                                AtomicInteger index = new AtomicInteger(0);
                                ResourceLocation[] topperLocs = nativeToppers.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_topper_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);
                                index.set(0);
                                ResourceLocation[] drapeLocs = nativeDrapes.stream().map(ni -> new DynamicTexture(ni)).map(dyn -> mc.getTextureManager().register("dynamic/" + sanitize(key + "_drape_" + index.getAndIncrement()), dyn)).toArray(ResourceLocation[]::new);

                                cachedAnimatedSingleTopper.put(key, new AnimatedTexture(topperLocs, originalAnim.delays, originalAnim.totalDuration));
                                cachedAnimatedSingleFrontDrape.put(key, new AnimatedTexture(drapeLocs, originalAnim.delays, originalAnim.totalDuration));
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
            Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
            Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());
            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nUp.x(), nUp.y(), nUp.z());
            buf.addVertex(p.pose(), -hw, hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
            buf.addVertex(p.pose(), -hw, -hl, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nDown.x(), nDown.y(), nDown.z());
        }
        ps.popPose();

        ps.pushPose();
        ps.translate(0, topperY, frontDrapeZ);
        {
            PoseStack.Pose p = ps.last();
            VertexConsumer buf = bufSrc.getBuffer(RenderType.text(frontDrapeTex));
            float hw = bedWidth / 2f, d = drapeDepth;
            Vector3f nOut = p.transformNormal(0, 0, -1, new Vector3f());
            Vector3f nIn = p.transformNormal(0, 0, 1, new Vector3f());

            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), -hw, 0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(), hw, 0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(), hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
        }
        ps.popPose();
    }
}