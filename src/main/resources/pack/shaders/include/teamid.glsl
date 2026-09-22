const int TEAM_BLACK = 0;
const int TEAM_DARK_BLUE = 1;
const int TEAM_DARK_GREEN = 2;
const int TEAM_DARK_AQUA = 3;
const int TEAM_DARK_RED = 4;
const int TEAM_DARK_PURPLE = 5;
const int TEAM_GOLD = 6;
const int TEAM_GRAY = 7;
const int TEAM_DARK_GRAY = 8;
const int TEAM_BLUE = 9;
const int TEAM_GREEN = 10;
const int TEAM_AQUA = 11;
const int TEAM_RED = 12;
const int TEAM_LIGHT_PURPLE = 13;
const int TEAM_YELLOW = 14;
const int TEAM_WHITE = 15;

int teamColorToEffectId(ivec3 colorBytes) {
    if (any(notEqual(colorBytes % 0x55, ivec3(0)))) {
        return -1;
    }
    ivec3 colorSteps = colorBytes / 0x55;
    int colorKey = colorSteps.r * 16 + colorSteps.g * 4 + colorSteps.b;
    if (colorKey == 0)  return TEAM_BLACK;
    if (colorKey == 2)  return TEAM_DARK_BLUE;
    if (colorKey == 8)  return TEAM_DARK_GREEN;
    if (colorKey == 10) return TEAM_DARK_AQUA;
    if (colorKey == 32) return TEAM_DARK_RED;
    if (colorKey == 34) return TEAM_DARK_PURPLE;
    if (colorKey == 56) return TEAM_GOLD;
    if (colorKey == 42) return TEAM_GRAY;
    if (colorKey == 21) return TEAM_DARK_GRAY;
    if (colorKey == 23) return TEAM_BLUE;
    if (colorKey == 29) return TEAM_GREEN;
    if (colorKey == 31) return TEAM_AQUA;
    if (colorKey == 53) return TEAM_RED;
    if (colorKey == 55) return TEAM_LIGHT_PURPLE;
    if (colorKey == 61) return TEAM_YELLOW;
    if (colorKey == 63) return TEAM_WHITE;
    return -1;
}

vec3 effectIdToTeamColor(int effectId) {
    if (effectId == TEAM_BLACK) return vec3(0.000000, 0.000000, 0.000000);
    if (effectId == TEAM_DARK_BLUE) return vec3(0.000000, 0.000000, 0.666667);
    if (effectId == TEAM_DARK_GREEN) return vec3(0.000000, 0.666667, 0.000000);
    if (effectId == TEAM_DARK_AQUA) return vec3(0.000000, 0.666667, 0.666667);
    if (effectId == TEAM_DARK_RED) return vec3(0.666667, 0.000000, 0.000000);
    if (effectId == TEAM_DARK_PURPLE) return vec3(0.666667, 0.000000, 0.666667);
    if (effectId == TEAM_GOLD) return vec3(1.000000, 0.666667, 0.000000);
    if (effectId == TEAM_GRAY) return vec3(0.666667, 0.666667, 0.666667);
    if (effectId == TEAM_DARK_GRAY) return vec3(0.333333, 0.333333, 0.333333);
    if (effectId == TEAM_BLUE) return vec3(0.333333, 0.333333, 1.000000);
    if (effectId == TEAM_GREEN) return vec3(0.333333, 1.000000, 0.333333);
    if (effectId == TEAM_AQUA) return vec3(0.333333, 1.000000, 1.000000);
    if (effectId == TEAM_RED) return vec3(1.000000, 0.333333, 0.333333);
    if (effectId == TEAM_LIGHT_PURPLE) return vec3(1.000000, 0.333333, 1.000000);
    if (effectId == TEAM_YELLOW) return vec3(1.000000, 1.000000, 0.333333);
    return vec3(1.000000, 1.000000, 1.000000);
}
