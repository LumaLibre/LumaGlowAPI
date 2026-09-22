package dev.lumas.glowapi.pack;

import dev.lumas.glowapi.effect.GradientPalette;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.CubeFace;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.overlays.OverlayEntry;
import team.unnamed.creative.metadata.overlays.OverlaysMeta;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.model.Element;
import team.unnamed.creative.model.ElementFace;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;
import team.unnamed.creative.texture.TextureUV;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GlowPack {

    public static final String NAMESPACE = "lumaglowapi";

    private static final int PACK_FORMAT = 88;
    private static final int MIN_FORMAT = 75;

    private static final int MAX_FORMAT = 199;

    private static final int SPIRV_FORMAT = 97;
    private static final String SPIRV_OVERLAY = "spirv";

    private static final int MARKER_ALPHA = 5;

    private static final String RESOURCE_ROOT = "pack/";

    private static final String PALETTE_TOKEN = "//__GRADIENT_PALETTE__";

    private static final String DEBUG_TOKEN = "//__DEBUG__";

    private GlowPack() {
    }

    public static @NotNull ResourcePack build(@NotNull ClassLoader resources, @NotNull GradientPalette gradients,
                                             boolean debug) {
        ResourcePack pack = ResourcePack.resourcePack();
        pack.packMeta(PackFormat.format(PACK_FORMAT, MIN_FORMAT, MAX_FORMAT),
                Component.text("LumaGlowAPI - shader glow effects"));

        Key textureKey = Key.key(NAMESPACE, "item/marker.png");
        Key textureRef = Key.key(NAMESPACE, "item/marker");
        pack.texture(Texture.texture(textureKey, Writable.bytes(markerPng())));

        ElementFace face = ElementFace.face().uv(TextureUV.uv(0f, 0f, 1f, 1f)).texture("#0").build();

        Element markerQuad = Element.element()
                .from(7f, 7f, 7f)
                .to(9f, 9f, 9f)
                .faces(Map.of(CubeFace.UP, face))
                .build();
        pack.model(Model.model()
                .key(Key.key(NAMESPACE, "item/marker"))
                .textures(ModelTextures.builder()
                        .variables(Map.of("0", ModelTexture.ofKey(textureRef)))
                        .particle(ModelTexture.ofKey(textureRef))
                        .build())
                .elements(markerQuad)
                .build());
        bundled(pack, resources, "assets/" + NAMESPACE + "/items/marker.json", "items/marker.json");

        bundled(pack, resources, "assets/minecraft/post_effect/entity_outline.json", "post_effect/entity_outline.json");

        String palette = paletteGlsl(gradients);
        Map<String, String> snippets = new HashMap<>();
        for (String[] shader : SHADERS) {
            String source = read(resources, RESOURCE_ROOT + shader[0])
                    .replace(PALETTE_TOKEN, palette)
                    .replace(DEBUG_TOKEN, debug ? "#define GLOW_DEBUG 1" : "");
            pack.unknownFile(shader[1], Writable.stringUtf8(expand(source, Dialect.LEGACY, resources, snippets)));

            pack.unknownFile(SPIRV_OVERLAY + "/" + shader[1],
                    Writable.stringUtf8(expand(source, Dialect.SPIRV, resources, snippets)));
        }
        pack.overlaysMeta(OverlaysMeta.of(
                OverlayEntry.of(PackFormat.format(SPIRV_FORMAT, SPIRV_FORMAT, MAX_FORMAT), SPIRV_OVERLAY)));
        return pack;
    }

    private static final String[][] SHADERS = {
            {"shaders/core/rendertype_outline.vsh", "assets/minecraft/shaders/core/rendertype_outline.vsh"},
            {"shaders/core/rendertype_outline.fsh", "assets/minecraft/shaders/core/rendertype_outline.fsh"},
            {"shaders/post/glow_table.fsh", "assets/" + NAMESPACE + "/shaders/post/glow_table.fsh"},
            {"shaders/post/glow_resolve.fsh", "assets/" + NAMESPACE + "/shaders/post/glow_resolve.fsh"},
    };

    private enum Dialect {

        LEGACY,

        SPIRV
    }

    private static String expand(String source, Dialect dialect, ClassLoader loader,
                                 Map<String, String> snippets) {
        StringBuilder out = new StringBuilder(source.length() + 256);
        for (String line : source.split("\n", -1)) {
            String directive = line.strip();
            if (!directive.startsWith("//!")) {
                out.append(line).append('\n');
                continue;
            }
            directive = directive.substring(3);

            String trailing = "";
            int comment = directive.indexOf("//");
            if (comment >= 0) {
                trailing = "  " + directive.substring(comment);
                directive = directive.substring(0, comment).strip();
            }

            String[] parts = directive.split("\\s+");
            switch (parts[0]) {
                case "version" -> {
                    out.append("#version 330\n");
                    if (dialect == Dialect.SPIRV) {
                        out.append("#extension GL_ARB_separate_shader_objects : require\n");
                    }
                    out.append("#define GLOW_VERTEX_ID ")
                            .append(dialect == Dialect.SPIRV ? "gl_VertexIndex" : "gl_VertexID")
                            .append('\n');
                }
                case "import" -> out.append(dialect == Dialect.SPIRV ? "#include" : "#moj_import")
                        .append(" <minecraft:").append(parts[1]).append(".glsl>\n");

                case "snippet" -> out.append(snippets.computeIfAbsent(parts[1],
                        name -> read(loader, RESOURCE_ROOT + "shaders/include/" + name + ".glsl")));
                case "in", "out" -> {
                    if (dialect == Dialect.SPIRV) {
                        out.append("layout(location = ").append(Integer.parseInt(parts[1])).append(") ");
                    }
                    out.append(parts[0]);
                    for (int i = 2; i < parts.length; i++) {
                        out.append(' ').append(parts[i]);
                    }
                    out.append(';').append(trailing).append('\n');
                }
                default -> throw new IllegalStateException("Unknown shader directive: " + line);
            }
        }
        return out.toString();
    }

    private static String paletteGlsl(GradientPalette palette) {
        List<GradientPalette.Gradient> gradients = palette.all();

        StringBuilder stops = new StringBuilder();
        StringBuilder starts = new StringBuilder();
        StringBuilder speeds = new StringBuilder();
        int offset = 0;
        for (GradientPalette.Gradient gradient : gradients) {
            starts.append(starts.isEmpty() ? "" : ", ").append(offset);
            speeds.append(speeds.isEmpty() ? "" : ", ")
                    .append(String.format(Locale.ROOT, "%.4f", gradient.speed()));
            for (int rgb : gradient.stops()) {
                stops.append(stops.isEmpty() ? "" : ",\n        ")
                        .append(String.format(Locale.ROOT, "vec3(%.5f, %.5f, %.5f)",
                                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f));
            }
            offset += gradient.stops().size();
        }
        starts.append(starts.isEmpty() ? "" : ", ").append(offset);

        int stopCount = Math.max(offset, 1);
        int startCount = gradients.isEmpty() ? 1 : gradients.size() + 1;
        if (gradients.isEmpty()) {
            stops.append("vec3(0.0, 0.0, 0.0)");
            starts.setLength(0);
            starts.append(0);
            speeds.append("1.0");
        }
        int speedCount = Math.max(gradients.size(), 1);

        StringBuilder out = new StringBuilder();
        out.append("const int GRADIENT_COUNT = ").append(gradients.size()).append(";\n");

        out.append("const int GRADIENT_START[").append(startCount).append("] = int[").append(startCount)
                .append("](").append(starts).append(");\n");
        out.append("const vec3 GRADIENT_STOPS[").append(stopCount).append("] = vec3[").append(stopCount)
                .append("](\n        ").append(stops).append(");\n");
        out.append("const float GRADIENT_SPEED[").append(speedCount).append("] = float[").append(speedCount)
                .append("](").append(speeds).append(");\n");
        out.append("""

                float paletteSpeed(int paletteIndex) {
                    if (paletteIndex < 0 || paletteIndex >= GRADIENT_COUNT) {
                        return 1.0;
                    }
                    return GRADIENT_SPEED[paletteIndex];
                }

                vec3 paletteColor(int paletteIndex, float blend) {
                    if (paletteIndex < 0 || paletteIndex >= GRADIENT_COUNT) {
                        return vec3(-1.0);
                    }
                    int firstStop = GRADIENT_START[paletteIndex];
                    int stopCount = GRADIENT_START[paletteIndex + 1] - firstStop;
                    if (stopCount < 2) {
                        return GRADIENT_STOPS[firstStop];
                    }
                    float stopPosition = clamp(blend, 0.0, 1.0) * float(stopCount - 1);
                    int segment = min(int(floor(stopPosition)), stopCount - 2);
                    return mix(GRADIENT_STOPS[firstStop + segment], GRADIENT_STOPS[firstStop + segment + 1],
                            stopPosition - float(segment));
                }
                """.stripIndent());
        return out.toString();
    }

    private static String read(ClassLoader loader, String name) {
        try (InputStream in = loader.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled pack resource: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void bundled(ResourcePack pack, ClassLoader loader, String packPath, String resource) {
        String name = RESOURCE_ROOT + resource;
        if (loader.getResource(name) == null) {
            throw new IllegalStateException("Missing bundled pack resource: " + name);
        }
        pack.unknownFile(packPath, Writable.resource(loader, name));
    }

    private static byte[] markerPng() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int argb = (MARKER_ALPHA << 24) | 0xFFFFFF;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, argb);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
