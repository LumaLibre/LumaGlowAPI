const int SPATIAL_COLUMNS = 1024;
const int SPATIAL_SLOTS = 8192;
const int SPATIAL_ROWS = 16;
const float METADATA_FRACTION = 0.03125;
const float CELL_SIZE = 2.0;
const int LOCATION_ALPHA = 254;
const int PAYLOAD_ALPHA = 253;
const int SOLID_ALPHA = 252;

bool locationInRange(vec3 position) {
    return all(greaterThanEqual(position, vec3(-256.0)))
            && all(lessThan(position, vec3(256.0)));
}

ivec3 locationKey(vec3 position) {
    return ivec3(floor(position / CELL_SIZE)) + 128;
}

uint locationHash(ivec3 cell, int teamColor) {
    uint key = uint(cell.x | (cell.y << 8) | (cell.z << 16) | (teamColor << 24));
    key ^= key >> 16;
    key *= 0x7feb352du;
    key ^= key >> 15;
    key *= 0x846ca68bu;
    key ^= key >> 16;
    return key;
}

int locationSlot(ivec3 cell, int teamColor) {
    return int(locationHash(cell, teamColor) & uint(SPATIAL_SLOTS - 1));
}

ivec3 sampleOffset(int sampleIndex) {
    return ivec3(sampleIndex / 9, (sampleIndex / 3) % 3, sampleIndex % 3) - 1;
}

ivec3 packedLocation(ivec3 cell, int teamColor, int sampleIndex) {
    uint locationBits = ((locationHash(cell, teamColor) >> 13) << 5) | uint(sampleIndex);
    return ivec3(locationBits & 255u, (locationBits >> 8) & 255u, locationBits >> 16);
}

uint unpackLocation(ivec3 bytes) {
    return uint(bytes.x | (bytes.y << 8) | (bytes.z << 16));
}
