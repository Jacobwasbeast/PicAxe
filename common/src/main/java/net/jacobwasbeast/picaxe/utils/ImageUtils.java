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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_LOAD_TRIES = 3;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final Set<String> blacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> loading = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<String, ResourceLocation> cachedTextures = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeLeft = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSideDrapeRight = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedFrontBackTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSingleFrontDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedSingleTopper = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cachedBackDrape = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> cacheFourSided = new ConcurrentHashMap<>();

    private static final ResourceLocation NOT_FOUND_TEXTURE;
    private static final ResourceLocation LOADING_TEXTURE;

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

        NOT_FOUND_TEXTURE = registerTextureFromImage("internal:notfound", notFoundImage);
        LOADING_TEXTURE = registerTextureFromImage("internal:loading", combinedLoadingImage);
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

    private static BufferedImage downloadImage(String url) {
        if (blacklist.contains(url)) {
            return null;
        }
        for (int i = 1; i <= MAX_LOAD_TRIES; i++) {
            try (InputStream in = new URL(url).openStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("ImageIO.read returned null");
                }
                return image;
            } catch (Exception e) {
                System.err.println("Attempt " + i + "/" + MAX_LOAD_TRIES + " failed to load " + url + ": " + e.getMessage());
            }
        }
        System.err.println("Gave up loading image, adding to blacklist: " + url);
        blacklist.add(url);
        return null;
    }

    private static String sanitize(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    /**
     * Registers a BufferedImage using the original user-provided logic for flipping and color conversion.
     * This is used for ALL textures to ensure they are processed identically.
     */
    public static ResourceLocation registerTextureFromImage(String key, BufferedImage img) {
        if (cachedTextures.containsKey(key)) return cachedTextures.get(key);
        try {
            NativeImage ni = new NativeImage(img.getWidth(), img.getHeight(), false);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(img.getWidth() - 1 - x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (b << 16) | (g << 8) | r | (a << 24);
                    ni.setPixelRGBA(x, y, abgr);
                }
            }
            DynamicTexture dyn = new DynamicTexture(ni);
            String safe = sanitize(key);
            ResourceLocation loc = ResourceLocation.tryBuild("dynamic", safe);
            mc.getTextureManager().register(loc, dyn);
            cachedTextures.put(key, loc);
            return loc;
        } catch (Exception e) {
            System.err.println("Failed to register texture for key: " + key + " – " + e.getMessage());
            return NOT_FOUND_TEXTURE;
        }
    }

    public static ResourceLocation getOrLoadTexture(String url) {
        if (url == null || url.isEmpty()) {
            return NOT_FOUND_TEXTURE;
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
                BufferedImage img = downloadImage(url);
                if (img != null) {
                    mc.execute(() -> registerTextureFromImage(url, img));
                }
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

    public static void renderImageFromURL(
            PoseStack ps, MultiBufferSource bufSrc,
            int packedLight, int packedOverlay,
            float partialTick, float width, float height,
            String url
    ) {
        ResourceLocation tex = getOrLoadTexture(url);

        int uL = packedLight & 0xFFFF, vL = (packedLight >> 16) & 0xFFFF;
        int uO = packedOverlay & 0xFFFF, vO = (packedOverlay >> 16) & 0xFFFF;

        ps.pushPose();
        ps.translate(0.5, 1.01, 0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose p = ps.last();
        VertexConsumer buf = bufSrc.getBuffer(RenderType.text(tex));

        float hw = width / 2f, hh = height / 2f;
        Vector3f nUp = p.transformNormal(0, 1, 0, new Vector3f());
        Vector3f nDown = p.transformNormal(0, -1, 0, new Vector3f());

        buf.addVertex(p.pose(), -hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());
        buf.addVertex(p.pose(), hw, -hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());
        buf.addVertex(p.pose(), hw, hh, 0f)
                .setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL)
                .setNormal(nUp.x(), nUp.y(), nUp.z());
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
        ResourceLocation topperTex = cachedSideTopper.get(key);
        ResourceLocation leftDrapeTex = cachedSideDrapeLeft.get(key);
        ResourceLocation rightDrapeTex = cachedSideDrapeRight.get(key);

        if (topperTex == null || leftDrapeTex == null || rightDrapeTex == null) {
            topperTex = leftDrapeTex = rightDrapeTex = getOrLoadTexture(url);

            if (!loading.contains(url) && !blacklist.contains(url) && cachedTextures.containsKey(url)) {
                executor.submit(() -> {
                    BufferedImage full = downloadImage(url);
                    if (full == null) {
                        blacklist.add(url);
                        return;
                    }

                    int W = full.getWidth(), H = full.getHeight();
                    if (W <= 0 || H <= 0) return;

                    float totalWidthBlocks = bedWidth + 2 * drapeDepth;
                    if (totalWidthBlocks <= 0) return;

                    try {
                        int leftW = Math.round((drapeDepth / totalWidthBlocks) * W);
                        int rightW = leftW;
                        int midW = W - leftW - rightW;

                        if (leftW <= 0 || rightW <= 0 || midW <= 0 || (leftW + midW) > W) return;

                        BufferedImage intendedRightDrapeImg = full.getSubimage(0, 0, leftW, H);
                        BufferedImage midImg = full.getSubimage(leftW, 0, midW, H);
                        BufferedImage intendedLeftDrapeImg = full.getSubimage(leftW + midW, 0, rightW, H);

                        mc.execute(() -> {
                            ResourceLocation finalLeftDrapeTex = registerTextureFromImage(key + "_drape_left", intendedLeftDrapeImg);
                            ResourceLocation finalTopperTex = registerTextureFromImage(key + "_topper", midImg);
                            ResourceLocation finalRightDrapeTex = registerTextureFromImage(key + "_drape_right", intendedRightDrapeImg);

                            cachedSideDrapeLeft.put(key, finalLeftDrapeTex);
                            cachedSideTopper.put(key, finalTopperTex);
                            cachedSideDrapeRight.put(key, finalRightDrapeTex);
                        });

                    } catch (RasterFormatException e) {
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
            buf.addVertex(p.pose(),  hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(),  hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(),  hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(),  hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
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
            buf.addVertex(p.pose(),  hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(),  hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(fn.x(), fn.y(), fn.z());
            buf.addVertex(p.pose(), -hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(),  hl,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
            buf.addVertex(p.pose(),  hl, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(bn.x(), bn.y(), bn.z());
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
        ResourceLocation topperTex = cachedFrontBackTopper.get(key);
        ResourceLocation frontDrapeTex = cachedFrontDrape.get(key);
        ResourceLocation backDrapeTex = cachedBackDrape.get(key);

        if (topperTex == null || frontDrapeTex == null || backDrapeTex == null) {
            topperTex = frontDrapeTex = backDrapeTex = getOrLoadTexture(url);

            if (!loading.contains(url) && !blacklist.contains(url) && cachedTextures.containsKey(url)) {
                executor.submit(() -> {
                    BufferedImage full = downloadImage(url);
                    if (full == null) {
                        blacklist.add(url);
                        return;
                    }

                    int W = full.getWidth(), H = full.getHeight();
                    if (W <= 0 || H <= 0) return;

                    float totalLengthBlocks = bedLength + 2 * drapeDepth;
                    if (totalLengthBlocks <= 0) return;

                    try {
                        int frontH = Math.round((drapeDepth / totalLengthBlocks) * H);
                        int backH = frontH;
                        int midH = H - frontH - backH;

                        if (frontH <= 0 || backH <= 0 || midH <= 0 || (frontH + midH) > H) return;

                        BufferedImage intendedBackDrapeImg = full.getSubimage(0, 0, W, frontH);
                        BufferedImage midImg = full.getSubimage(0, frontH, W, midH);
                        BufferedImage intendedFrontDrapeImg = full.getSubimage(0, frontH + midH, W, backH);

                        mc.execute(() -> {
                            ResourceLocation finalFrontDrapeTex = registerTextureFromImage(key + "_drape_front", intendedFrontDrapeImg);
                            ResourceLocation finalTopperTex = registerTextureFromImage(key + "_topper_frontback", midImg);
                            ResourceLocation finalBackDrapeTex = registerTextureFromImage(key + "_drape_back", intendedBackDrapeImg);

                            cachedFrontDrape.put(key, finalFrontDrapeTex);
                            cachedFrontBackTopper.put(key, finalTopperTex);
                            cachedBackDrape.put(key, finalBackDrapeTex);
                        });

                    } catch (RasterFormatException e) {
                        System.err.println("Error splitting texture for front/back drapes " + key + ": " + e.getMessage());
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
        ResourceLocation topperTex = cachedSingleTopper.get(key);
        ResourceLocation frontDrapeTex = cachedSingleFrontDrape.get(key);

        if (topperTex == null || frontDrapeTex == null) {
            topperTex = frontDrapeTex = getOrLoadTexture(url);

            if (!loading.contains(url) && !blacklist.contains(url) && cachedTextures.containsKey(url)) {
                executor.submit(() -> {
                    BufferedImage full = downloadImage(url);
                    if (full == null) {
                        blacklist.add(url);
                        return;
                    }

                    int W = full.getWidth(), H = full.getHeight();
                    if (W <= 0 || H <= 0) return;

                    float totalLengthBlocks = bedLength + drapeDepth;
                    if (totalLengthBlocks <= 0) return;

                    try {
                        int drapeH = Math.round((drapeDepth / totalLengthBlocks) * H);
                        int topperH = H - drapeH;

                        if (drapeH <= 0 || topperH <= 0) return;

                        BufferedImage topperImg = full.getSubimage(0, 0, W, topperH);
                        BufferedImage drapeImg = full.getSubimage(0, topperH, W, drapeH);

                        mc.execute(() -> {
                            ResourceLocation finalFrontDrapeTex = registerTextureFromImage(key + "_drape_front", drapeImg);
                            ResourceLocation finalTopperTex = registerTextureFromImage(key + "_topper_front", topperImg);

                            cachedSingleFrontDrape.put(key, finalFrontDrapeTex);
                            cachedSingleTopper.put(key, finalTopperTex);
                        });

                    } catch (RasterFormatException e) {
                        System.err.println("Error splitting texture for front drape " + key + ": " + e.getMessage());
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
            buf.addVertex(p.pose(),  hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(),  hw,  0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), -hw,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nOut.x(), nOut.y(), nOut.z());
            buf.addVertex(p.pose(), -hw,  0, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(),  hw,  0, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(),  hw, -d, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
            buf.addVertex(p.pose(), -hw, -d, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setUv1(uO, vO).setUv2(uL, vL).setNormal(nIn.x(), nIn.y(), nIn.z());
        }
        ps.popPose();
    }

    public static ResourceLocation getTexture(String imageUrl) {
        String key = imageUrl + "_texture";
        ResourceLocation tex = cachedTextures.get(key);
        if (tex == null) {
            tex = getOrLoadTexture(imageUrl);
            if (tex != null) {
                cachedTextures.put(key, tex);
            }
        }
        return tex;
    }

    public static ResourceLocation getBannerTextureLocationForURL(String imageUrl) {
        String key = imageUrl + "_shield_texture";
        ResourceLocation tex = cachedTextures.get(key);
        if (tex == null) {
            tex = getOrLoadTexture(imageUrl);
            if (tex != null) {
                cachedTextures.put(key, tex);
            }
        }
        return tex;
    }
}