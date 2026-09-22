//!version

uniform sampler2D Sampler0;

//!in 0 vec4 vertexColor
//!in 1 vec2 texCoord0
//!in 2 vec3 relativePosition
//!in 3 vec2 verticalClipDistance
//!in 4 flat ivec4 markerLocation
//!in 5 vec2 markerUv
//!in 6 flat int markerSample

//!out 0 vec4 fragColor

//!snippet glow_encoding
//!snippet spatial
//!snippet teamid

const float SKIN_WIDTH = 64.0;
const float PLAYER_HEIGHT = 32.0;

float bodyHeight(vec2 skinUv) {
    vec2 skinTextureSize = vec2(textureSize(Sampler0, 0));
    vec2 skinPixel = skinUv * SKIN_WIDTH * vec2(1.0, skinTextureSize.y / skinTextureSize.x);
    float skinX = skinPixel.x;
    float skinY = skinPixel.y;

    if (skinY < 16.0) {
        if (skinY >= 8.0) {
            return 40.0 - skinY;
        }
        bool isHeadTop = mod(skinX, 32.0) < 16.0;
        return isHeadTop ? 32.0 : 24.0;
    }

    if (skinY < 48.0) {
        float layerY = mod(skinY, 16.0);
        if (skinX < 16.0) {
            return layerY >= 4.0 ? 16.0 - layerY : (skinX < 8.0 ? 12.0 : 0.0);
        }
        if (layerY >= 4.0) {
            return 28.0 - layerY;
        }
        float torsoOrArmTopEnd = skinX < 40.0 ? 28.0 : 48.0;
        return skinX < torsoOrArmTopEnd ? 24.0 : 12.0;
    }

    float leftLimbY = skinY - 48.0;
    bool isLeftLimbTop = mod(skinX, 16.0) < 8.0;
    if (skinX < 32.0) {
        return leftLimbY >= 4.0 ? 16.0 - leftLimbY : (isLeftLimbTop ? 12.0 : 0.0);
    }
    return leftLimbY >= 4.0 ? 28.0 - leftLimbY : (isLeftLimbTop ? 24.0 : 12.0);
}

void main() {
    ivec3 colorBytes = decodeBytes(vertexColor.rgb);
    if (markerLocation.w >= 0) {
        float targetWidth = float(SPATIAL_COLUMNS) / abs(dFdx(markerUv.x));
        float targetHeight = 1.0 / abs(dFdy(markerUv.y));
        if (targetWidth + 0.01 < float(SPATIAL_COLUMNS)
                || targetHeight * METADATA_FRACTION + 0.01 < float(SPATIAL_ROWS)) {
            discard;
        }
        int slot = markerLocation.w;
        if (slot < 0) {
            discard;
        }
        ivec3 cell = markerLocation.xyz;
        int row = int(gl_FragCoord.y) - (slot / SPATIAL_COLUMNS) * 2;
        if (row == 0) {
            fragColor = vec4(vec3(cell), float(LOCATION_ALPHA)) / BYTE_MAX;
        } else if (row == 1) {
            fragColor = vec4(vertexColor.rgb, float(markerSample >= 32 ? SOLID_ALPHA : PAYLOAD_ALPHA) / BYTE_MAX);
        } else {
            discard;
        }
        return;
    }

    if (any(lessThan(verticalClipDistance, vec2(0.0))) || texture(Sampler0, texCoord0).a == 0.0) {
        discard;
    }

    int effectId = teamColorToEffectId(colorBytes);
    if (effectId < 0) {
        fragColor = vec4(vertexColor.rgb, 1.0);
        return;
    }

    if (!locationInRange(relativePosition)) {
        fragColor = vec4(vertexColor.rgb, 1.0);
        return;
    }
    float normalizedBodyHeight = clamp(bodyHeight(texCoord0) / PLAYER_HEIGHT, 0.0, 1.0);
    int heightBand = int(round(normalizedBodyHeight * 14.0));
    int identity = 1 + effectId * 15 + heightBand;
    fragColor = vec4(vec3(locationKey(relativePosition)), float(identity)) / BYTE_MAX;
}
