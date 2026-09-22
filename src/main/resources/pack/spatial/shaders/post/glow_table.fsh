//!version

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

//!in 0 vec2 texCoord
//!out 0 vec4 fragColor

//!snippet glow_encoding
//!snippet spatial

void main() {
    ivec2 inputSize = textureSize(InSampler, 0);
    ivec2 entry = ivec2(gl_FragCoord.xy);
    if (inputSize.x < SPATIAL_COLUMNS || float(inputSize.y) * METADATA_FRACTION < float(SPATIAL_ROWS)) {
        fragColor = vec4(0.0);
        return;
    }
    int markerX = int((float(entry.x) + 0.5) * float(inputSize.x) / float(SPATIAL_COLUMNS));
    fragColor = texelFetch(InSampler, ivec2(markerX, entry.y), 0);
}
