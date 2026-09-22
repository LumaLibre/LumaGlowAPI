//!version

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

//!in 0 vec2 texCoord
//!out 0 vec4 fragColor

//!snippet glow_encoding

void main() {
    int effectId = int(gl_FragCoord.x);
    int inputWidth = textureSize(InSampler, 0).x;
    float columnCenter = (float(effectId) + 0.5) * float(inputWidth) / float(EFFECT_ID_COUNT);
    int markerX = min(int(columnCenter), inputWidth - 1);
    vec4 marker = texelFetch(InSampler, ivec2(markerX, METADATA_ROW), 0);

    int redByte = decodeByte(marker.r);
    int expectedMarkerId = (MARKER_TAG << NIBBLE_BITS) | effectId;
    bool hasMarker = marker.a > 0.0 && redByte == expectedMarkerId;
    fragColor = hasMarker ? vec4(1.0, marker.g, marker.b, 1.0) : vec4(0.0);
}
