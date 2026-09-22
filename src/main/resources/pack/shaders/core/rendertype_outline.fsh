//!version

uniform sampler2D Sampler0;

//!in 0 vec4 vertexColor
//!in 1 vec2 texCoord0

//!out 0 vec4 fragColor

//!snippet glow_encoding
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
    if ((colorBytes.r >> NIBBLE_BITS) == MARKER_TAG) {
        if (gl_FragCoord.y >= float(FIRST_SILHOUETTE_ROW)) {
            discard;
        }
        fragColor = vec4(vertexColor.rgb, 1.0);
        return;
    }

    if (gl_FragCoord.y < float(FIRST_SILHOUETTE_ROW)) {
        discard;
    }
    if (texture(Sampler0, texCoord0).a == 0.0) {
        discard;
    }

    int effectId = teamColorToEffectId(colorBytes);
    if (effectId < 0) {
        fragColor = vec4(vertexColor.rgb, 1.0);
        return;
    }

    float normalizedBodyHeight = bodyHeight(texCoord0) / PLAYER_HEIGHT;
    float encodedEffectId = float(SILHOUETTE_TAG * EFFECT_ID_COUNT + effectId) / BYTE_MAX;
    fragColor = vec4(encodedEffectId, normalizedBodyHeight, 0.0, 1.0);
}
