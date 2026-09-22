//!version

//!import dynamictransforms
//!import projection

//!in 0 vec3 Position
//!in 1 vec4 Color
//!in 2 vec2 UV0

//!out 0 vec4 vertexColor
//!out 1 vec2 texCoord0

//!snippet glow_encoding

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = vec4(ColorModulator.rgb * Color.rgb, ColorModulator.a);
    texCoord0 = UV0;

    int redByte = decodeByte(vertexColor.r);
    if ((redByte >> NIBBLE_BITS) == MARKER_TAG) {
        int effectId = redByte & NIBBLE_MASK;
        int quadCorner = GLOW_VERTEX_ID & 3;
        vec2 cornerUv = vec2(
                (quadCorner == 1 || quadCorner == 2) ? 1.0 : 0.0,
                quadCorner >= 2 ? 1.0 : 0.0);
        float columnPosition = (float(effectId) + cornerUv.x) / float(EFFECT_ID_COUNT);

        gl_Position = vec4(columnPosition * 2.0 - 1.0, cornerUv.y * 2.0 - 1.0, 0.0, 1.0);
    }
}
