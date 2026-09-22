//!version

//!import dynamictransforms
//!import projection

uniform sampler2D Sampler0;

//!in 0 vec3 Position
//!in 1 vec4 Color
//!in 2 vec2 UV0

//!out 0 vec4 vertexColor
//!out 1 vec2 texCoord0
//!out 2 flat int isMarker

//!snippet glow_encoding
//!snippet marker

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = vec4(ColorModulator.rgb * Color.rgb, ColorModulator.a);
    texCoord0 = UV0;

    ivec3 markerCode = readMarker(UV0);
    isMarker = markerCode.x >= 0 ? 1 : 0;
    int redByte = decodeByte(vertexColor.r);
    if (isMarker == 1) {
        int effectId = redByte & NIBBLE_MASK;
        vec2 cornerUv = markerCornerUv(markerCode.z);
        float columnPosition = (float(effectId) + cornerUv.x) / float(EFFECT_ID_COUNT);

        gl_Position = vec4(columnPosition * 2.0 - 1.0, cornerUv.y * 2.0 - 1.0, 0.0, 1.0);
    }
}
