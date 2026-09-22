//!version

//!import dynamictransforms
//!import projection

uniform sampler2D Sampler0;

//!in 0 vec3 Position
//!in 1 vec4 Color
//!in 2 vec2 UV0

//!out 0 vec4 vertexColor
//!out 1 vec2 texCoord0
//!out 2 vec3 relativePosition
//!out 3 vec2 verticalClipDistance
//!out 4 flat ivec4 markerLocation
//!out 5 vec2 markerUv
//!out 6 flat int markerSample

//!snippet glow_encoding
//!snippet spatial
//!snippet marker

const float MARKER_WORLD_HALF_WIDTH = 1.0 / 128.0;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;
    vertexColor = vec4(ColorModulator.rgb * Color.rgb, ColorModulator.a);
    texCoord0 = UV0;
    relativePosition = viewPosition.xyz;
    markerLocation = ivec4(0, 0, 0, -1);
    markerUv = vec2(0.0);
    markerSample = 0;
    verticalClipDistance = vec2(gl_Position.w + gl_Position.y, gl_Position.w - gl_Position.y);
    gl_Position.y = gl_Position.y * (1.0 - 2.0 * METADATA_FRACTION);

    ivec3 markerCode = readMarker(UV0);
    if (markerCode.x >= 0) {
        int effectId = markerCode.y & 15;
        markerSample = markerCode.x + (markerCode.y >= 16 ? 32 : 0);
        int quadCorner = markerCode.z;
        vec2 cornerUv = markerCornerUv(quadCorner);
        markerUv = cornerUv;
        vec3 markerCornerOffset = vec3(
                quadCorner < 2 ? MARKER_WORLD_HALF_WIDTH : -MARKER_WORLD_HALF_WIDTH,
                0.0,
                (quadCorner == 0 || quadCorner == 3) ? MARKER_WORLD_HALF_WIDTH : -MARKER_WORLD_HALF_WIDTH);
        vec3 markerViewCenter = (ModelViewMat * vec4(Position - markerCornerOffset, 1.0)).xyz;
        if (!locationInRange(markerViewCenter)) {
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            return;
        }
        ivec3 cell = locationKey(markerViewCenter) + sampleOffset(markerCode.x);
        if (any(lessThan(cell, ivec3(0))) || any(greaterThan(cell, ivec3(255)))) {
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            return;
        }
        int slot = locationSlot(cell, effectId);
        markerLocation = ivec4(packedLocation(cell, effectId, markerCode.x), slot);
        float columnPosition = (float(slot % SPATIAL_COLUMNS) + cornerUv.x) / float(SPATIAL_COLUMNS);

        gl_Position = vec4(columnPosition * 2.0 - 1.0, cornerUv.y * 2.0 - 1.0, 0.0, 1.0);
    }
}
