package dev.lumas.glowapi.pack;

import dev.lumas.glowapi.effect.GradientPalette;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

public final class ShaderCompatibilityRegression {
    // GLSL 3.30 section 3.6. Some compilers accept these as variable names,
    // while other drivers reject the entire required outline pipeline.
    private static final Set<String> RESERVED = Set.of(
            "common", "partition", "active", "asm", "class", "union", "enum", "typedef",
            "template", "this", "packed", "goto", "inline", "noinline", "volatile", "public",
            "static", "extern", "external", "interface", "long", "short", "double", "half",
            "fixed", "unsigned", "superp", "input", "output", "filter", "sizeof", "cast",
            "namespace", "using", "row_major");
    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(?:bool|int|uint|float|[biu]?vec[234]|mat[234])\\s+([A-Za-z_][A-Za-z_0-9]*)\\b");

    public static void run() throws Exception {
        int shaders = 0;
        for (boolean spatial : new boolean[]{false, true}) {
            for (boolean debug : new boolean[]{false, true}) {
                var pack = GlowPack.build(ShaderCompatibilityRegression.class.getClassLoader(),
                        GradientPalette.get(), debug, spatial);
                byte[] bytes = MinecraftResourcePackWriter.minecraft().build(pack).data().toByteArray();
                try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        if (!entry.getName().endsWith(".vsh") && !entry.getName().endsWith(".fsh")) {
                            continue;
                        }
                        String source = new String(zip.readAllBytes(), StandardCharsets.UTF_8)
                                .replaceAll("(?s)/\\*.*?\\*/|//[^\\r\\n]*", "");
                        var declarations = DECLARATION.matcher(source);
                        while (declarations.find()) {
                            String identifier = declarations.group(1);
                            if (RESERVED.contains(identifier)) {
                                throw new AssertionError("Reserved GLSL identifier '" + identifier
                                        + "' in " + entry.getName() + " (spatial=" + spatial
                                        + ", debug=" + debug + ")");
                            }
                        }
                        shaders++;
                    }
                }
            }
        }
        if (shaders != 32) throw new AssertionError("Expected 32 exported shader variants, got " + shaders);
        System.out.println("All 32 exported shader variants avoid reserved GLSL variable names");
    }
}
