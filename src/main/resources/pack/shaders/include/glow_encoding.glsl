const float BYTE_MAX = 255.0;
const int NIBBLE_BITS = 4;
const int NIBBLE_MASK = 0x0F;
const int EFFECT_ID_COUNT = 16;
const int MARKER_TAG = 2;
const int SILHOUETTE_TAG = 4;
const int METADATA_ROW = 0;
const int FIRST_SILHOUETTE_ROW = 1;

int decodeByte(float channel) {
    return int(channel * BYTE_MAX + 0.5);
}

ivec3 decodeBytes(vec3 color) {
    return ivec3(color * BYTE_MAX + 0.5);
}
