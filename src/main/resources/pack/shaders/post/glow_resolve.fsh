//!version

uniform sampler2D InSampler;
uniform sampler2D TableSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

//!import globals

//!in 0 vec2 texCoord
//!out 0 vec4 fragColor

//!snippet glow_encoding
//!snippet teamid

//__DEBUG__

const int EFFECT_RAINBOW = 0;
const int EFFECT_HUE_GRADIENT = 1;
const int EFFECT_PULSE = 2;
const int EFFECT_SOLID = 3;
const int EFFECT_PALETTE_GRADIENT = 4;

const float RAINBOW_CYCLES = 0.75;
const float GRADIENT_CYCLES = 0.5;
const float GAME_TIME_TO_SECONDS = 1200.0;
const float FULL_CIRCLE = 6.28318530718;

vec3 hsvToRgb(vec3 hsv) {
    vec3 channelOffsets = vec3(0.0, 2.0 / 3.0, 1.0 / 3.0);
    vec3 hueRamp = abs(fract(hsv.xxx + channelOffsets) * 6.0 - 3.0);
    return hsv.z * mix(vec3(1.0), clamp(hueRamp - 1.0, 0.0, 1.0), hsv.y);
}

//__GRADIENT_PALETTE__

vec3 animatedEffectColor(int effectMode, float parameter, float hue, float bodyHeight, float timeSeconds) {
    if (effectMode == EFFECT_RAINBOW) {
        float speed = parameter == 0.0 ? 1.0 : parameter / 8.0;
        float animatedHue = fract(bodyHeight * RAINBOW_CYCLES - timeSeconds * 0.5 * speed);
        return hsvToRgb(vec3(animatedHue, 1.0, 1.0));
    }

    if (effectMode == EFFECT_HUE_GRADIENT) {
        float hueSpan = parameter == 0.0 ? 1.0 : parameter / 16.0;
        float blend = abs(fract(bodyHeight * GRADIENT_CYCLES + timeSeconds * 0.05) * 2.0 - 1.0);
        return hsvToRgb(vec3(fract(hue + hueSpan * blend), 1.0, 1.0));
    }

    if (effectMode == EFFECT_PULSE) {
        float speed = parameter == 0.0 ? 1.0 : parameter / 8.0;
        float brightness = 0.55 + 0.45 * sin(timeSeconds * speed * FULL_CIRCLE);
        return hsvToRgb(vec3(hue, 1.0, brightness));
    }

    if (effectMode == EFFECT_PALETTE_GRADIENT) {
        int paletteIndex = int(parameter) * 256 + decodeByte(hue);
        float blend = abs(fract(bodyHeight * GRADIENT_CYCLES
                + timeSeconds * 0.05 * paletteSpeed(paletteIndex)) * 2.0 - 1.0);
        return paletteColor(paletteIndex, blend);
    }

    return vec3(-1.0);
}

void main() {
    ivec2 inputSize = textureSize(InSampler, 0);
    ivec2 sourcePixel = clamp(ivec2(texCoord * vec2(inputSize)), ivec2(0), inputSize - 1);
    sourcePixel.y = max(sourcePixel.y, min(FIRST_SILHOUETTE_ROW, inputSize.y - 1));
    vec4 outlinePixel = texelFetch(InSampler, sourcePixel, 0);
    if (outlinePixel.a == 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    ivec3 outlineBytes = decodeBytes(outlinePixel.rgb);
    int pixelTag = outlineBytes.r >> NIBBLE_BITS;
    if (pixelTag == MARKER_TAG) {
        fragColor = vec4(0.0);
        return;
    }
    if (pixelTag != SILHOUETTE_TAG) {
#ifdef GLOW_DEBUG
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
#else
        fragColor = vec4(outlinePixel.rgb, 1.0);
#endif
        return;
    }

    int effectId = outlineBytes.r & NIBBLE_MASK;
    float bodyHeight = float(outlineBytes.g) / BYTE_MAX;
    vec4 effectEntry = texelFetch(TableSampler, ivec2(effectId, 0), 0);

#ifdef GLOW_DEBUG
    fragColor = effectEntry.r < 0.5
            ? vec4(1.0, 0.0, 0.0, 1.0)
            : vec4(hsvToRgb(vec3(0.15 + bodyHeight * 0.55, 1.0, 1.0)), 1.0);
    return;
#endif

    if (effectEntry.r < 0.5) {
        fragColor = vec4(effectIdToTeamColor(effectId), 1.0);
        return;
    }

    int settingsByte = decodeByte(effectEntry.g);
    int colorByte = decodeByte(effectEntry.b);
    int effectMode = settingsByte & NIBBLE_MASK;
    int parameter = settingsByte >> NIBBLE_BITS;
    vec3 effectColor;

    if (effectMode == EFFECT_SOLID) {
        effectColor = vec3(float(parameter), float(colorByte >> NIBBLE_BITS),
                float(colorByte & NIBBLE_MASK)) / float(NIBBLE_MASK);
    } else {
        float hue = float(colorByte) / BYTE_MAX;
        float timeSeconds = GameTime * GAME_TIME_TO_SECONDS;
        effectColor = animatedEffectColor(effectMode, float(parameter), hue, bodyHeight, timeSeconds);
        if (effectColor.r < 0.0) {
            effectColor = effectIdToTeamColor(effectId);
        }
    }

    fragColor = vec4(effectColor, 1.0);
}
