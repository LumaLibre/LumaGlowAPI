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
//!snippet spatial
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

bool lookupEffect(ivec3 cell, ivec3 targetCell, int effectId, out vec4 payload) {
    if (any(lessThan(cell, ivec3(0))) || any(greaterThan(cell, ivec3(255)))) {
        return false;
    }
    uint hash = locationHash(cell, effectId);
    int slot = int(hash & uint(SPATIAL_SLOTS - 1));
    ivec2 entry = ivec2(slot % SPATIAL_COLUMNS, (slot / SPATIAL_COLUMNS) * 2);
    vec4 location = texelFetch(TableSampler, entry, 0);
    uint locationBits = unpackLocation(decodeBytes(location.rgb));
    int sampleIndex = int(locationBits & 31u);
    if (decodeByte(location.a) != LOCATION_ALPHA || (locationBits >> 5) != (hash >> 13) || sampleIndex >= 27) {
        return false;
    }
    ivec3 anchor = cell - sampleOffset(sampleIndex);
    if (any(greaterThan(abs(targetCell - anchor), ivec3(1)))) {
        return false;
    }
    payload = texelFetch(TableSampler, entry + ivec2(0, 1), 0);
    int kind = decodeByte(payload.a);
    return kind == SOLID_ALPHA || (kind == PAYLOAD_ALPHA
            && decodeByte(payload.r) == (MARKER_TAG << NIBBLE_BITS) + effectId);
}

bool findEffect(ivec3 cell, int effectId, out vec4 payload) {
    if (lookupEffect(cell, cell, effectId, payload)) {
        return true;
    }
    const ivec3 neighbors[6] = ivec3[6](ivec3(1, 0, 0), ivec3(-1, 0, 0),
            ivec3(0, 1, 0), ivec3(0, -1, 0), ivec3(0, 0, 1), ivec3(0, 0, -1));
    bool found = false;
    for (int i = 0; i < 6; i++) {
        vec4 candidate;
        if (lookupEffect(cell + neighbors[i], cell, effectId, candidate)) {
            if (found && any(notEqual(candidate, payload))) {
                return false;
            }
            payload = candidate;
            found = true;
        }
    }
    return found;
}

void main() {
#ifdef GLOW_DEBUG
    vec2 debugPosition = (texCoord - vec2(0.01, 0.65)) / vec2(0.48, 0.33);
    if (all(greaterThanEqual(debugPosition, vec2(0.0)))
            && all(lessThan(debugPosition, vec2(1.0)))) {
        ivec2 tile = ivec2(debugPosition * vec2(130.0, 66.0));
        if (tile.x == 0 || tile.x == 129 || tile.y == 0 || tile.y == 65) {
            fragColor = vec4(1.0);
            return;
        }
        int debugSlot = (tile.y - 1) * 128 + tile.x - 1;
        ivec2 debugEntry = ivec2(debugSlot % SPATIAL_COLUMNS, (debugSlot / SPATIAL_COLUMNS) * 2);
        bool hasLocation = decodeByte(texelFetch(TableSampler, debugEntry, 0).a) == LOCATION_ALPHA;
        int payloadKind = decodeByte(texelFetch(TableSampler, debugEntry + ivec2(0, 1), 0).a);
        bool hasPayload = payloadKind == PAYLOAD_ALPHA || payloadKind == SOLID_ALPHA;
        fragColor = hasLocation && hasPayload ? vec4(0.0, 1.0, 0.0, 1.0)
                : hasLocation || hasPayload ? vec4(1.0, 1.0, 0.0, 1.0) : vec4(0.0);
        return;
    }
#endif
    ivec2 inputSize = textureSize(InSampler, 0);
    ivec2 sourcePixel = clamp(ivec2(vec2(texCoord.x, METADATA_FRACTION + texCoord.y * (1.0 - 2.0 * METADATA_FRACTION)) * vec2(inputSize)), ivec2(0), inputSize - 1);
    vec4 outlinePixel = texelFetch(InSampler, sourcePixel, 0);
    if (outlinePixel.a == 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    int identity = decodeByte(outlinePixel.a) - 1;
    if (identity >= 240) {
        fragColor = vec4(outlinePixel.rgb, 1.0);
        return;
    }
    int effectId = identity / 15;
    float bodyHeight = float(identity % 15) / 14.0;
    ivec3 cell = decodeBytes(outlinePixel.rgb);
    vec4 effectEntry;
    bool hasEffect = findEffect(cell, effectId, effectEntry);

#ifdef GLOW_DEBUG
    fragColor = hasEffect ? vec4(hsvToRgb(vec3(0.15 + bodyHeight * 0.55, 1.0, 1.0)), 1.0)
            : vec4(1.0, 0.0, 0.0, 1.0);
    return;
#endif

    if (!hasEffect) {
        fragColor = vec4(effectIdToTeamColor(effectId), 1.0);
        return;
    }

    if (decodeByte(effectEntry.a) == SOLID_ALPHA) {
        fragColor = vec4(effectEntry.rgb, 1.0);
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
