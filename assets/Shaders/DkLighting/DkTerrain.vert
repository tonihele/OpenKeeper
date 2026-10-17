#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"

// Must match SceneLights.MAX_LIGHTS.
#define DK_MAX_LIGHTS 1024

uniform vec4 m_Ambient;
uniform vec4 m_Tint;
#ifdef AMBIENT_MAP
uniform sampler2D m_AmbientMap;
uniform vec2 m_AmbientMapSize;
#endif
// Every light near the camera, shared by all terrain materials (see
// SceneLights): texel 0 = (light count, global ambient rgb); then three
// texels per light: (position xyz, radius), (colour rgb, intensity),
// (flags, 0, 0, 0). Flags:
//   bit 0 (1): use the smooth monotonic falloff (the cursor light), else the
//              exact D7 falloff
//   bit 1 (2): the light may shine on unexplored ground (the claim-scan
//              glow and the cursor / "hand" light); every other light fades
//              out over unexplored tiles
#ifdef LIGHT_DATA
uniform sampler2D m_LightData;
#endif

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec3 vVertexColor;

void main() {
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;

    vec3 worldPos = TransformWorld(modelSpacePos).xyz;
    vec3 worldNormal = normalize(TransformWorldNormal(inNormal));

#ifdef AMBIENT_MAP
    // Texel centres sit at tile + 0.5, so linear filtering blends the
    // ambient of neighbouring tiles across each tile (stored halved). The
    // alpha channel holds the tile's explored flag, blended the same way: a
    // vertex touching an unexplored tile is partly (or wholly) unexplored.
    vec4 tileData = textureLod(m_AmbientMap, worldPos.xz / m_AmbientMapSize, 0.0);
    vec3 tileAmbient = tileData.rgb * 2.0;
    float explored = tileData.a;
#else
    vec3 tileAmbient = m_Ambient.rgb;
    float explored = 1.0;
#endif

    vec3 globalAmbient = vec3(0.0);
    int numLights = 0;
#ifdef LIGHT_DATA
    vec4 header = texelFetch(m_LightData, ivec2(0, 0), 0);
    numLights = int(header.x + 0.5);
    globalAmbient = header.yzw;
#endif

    // Unexplored ground is completely black: no tile or global ambient, no floor
    vec3 sum = (tileAmbient + globalAmbient) * explored;
#ifdef LIGHT_DATA
    for (int i = 0; i < DK_MAX_LIGHTS; i++) {
        if (i >= numLights) {
            break;
        }
        int base = 1 + 3 * i;
        vec4 posRadius = texelFetch(m_LightData, ivec2(base, 0), 0);
        vec4 colorIntensity = texelFetch(m_LightData, ivec2(base + 1, 0), 0);
        int flags = int(texelFetch(m_LightData, ivec2(base + 2, 0), 0).x + 0.5);
        // Coloured light (torches, creatures, effects, explored terrain) must
        // not reveal unexplored ground; only the claim-scan glow and the
        // cursor do
        float visibility = (flags & 2) != 0 ? 1.0 : explored;
        sum += visibility * dkPointLightContribution(worldPos, worldNormal, posRadius.xyz,
                posRadius.w, colorIntensity.rgb, colorIntensity.w, (flags & 1) != 0);
    }
#endif

    vec3 lit = dkSoftClampFloor(sum, explored);

    // Tile highlight (tag = blue glow, flash = red glow). Applied AFTER the
    // clamp: added before it, a tile already lit near 1.0 swallowed the tint.
    // Multiplying keeps the diffuse texture visible and the colour reads on
    // bright tiles; the small additive part makes it show on dark ones too.
    vec3 tint = m_Tint.rgb;
    if (tint.r + tint.g + tint.b > 0.0) {
        // A selected / flashing tile is fully lit, whatever the scene lighting
        // around it, so its highlight colour always reads the same
        lit = vec3(1.0);
        lit = lit * (vec3(0.35) + 1.4 * tint) + tint * 0.25;
    }
    vVertexColor = lit;
}
