// Reads a marker from the texture at the given UV coordinates.
// Returns the marker's sample, model code, and corner.
// If the pixel is not a valid marker, returns (-1, -1, -1).
ivec3 readMarker(vec2 uv) {
    ivec2 size = textureSize(Sampler0, 0);
    ivec2 pixel = clamp(ivec2(uv * vec2(size)), ivec2(0), size - 1);
    ivec4 data = ivec4(round(texelFetch(Sampler0, pixel, 0) * BYTE_MAX));
    if (data.a != 5 || data.r < 240 || data.r > 243 || data.g >= 27 || data.b >= 32) {
        return ivec3(-1);
    }
    return ivec3(data.g, data.b, data.r - 240);
}

vec2 markerCornerUv(int corner) {
    return vec2((corner == 1 || corner == 2) ? 1.0 : 0.0, corner >= 2 ? 1.0 : 0.0);
}
