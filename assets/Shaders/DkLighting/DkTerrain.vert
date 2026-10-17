#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"

// Must match SceneLights.MAX_LIGHTS_PER_MATERIAL.
#define DK_MAX_LIGHTS 96

uniform vec4 m_Ambient;
uniform vec4 m_GlobalAmbient;
uniform vec4 m_Tint;
#ifdef AMBIENT_MAP
uniform sampler2D m_AmbientMap;
uniform vec2 m_AmbientMapSize;
#endif
uniform vec4 m_LightPosRadius[DK_MAX_LIGHTS];
uniform vec4 m_LightColorIntensity[DK_MAX_LIGHTS];
// 1 = use the smooth monotonic falloff (the cursor light), 0 = the exact
// D7 falloff. Must stay the same length as the pair above.
uniform int m_LightRadial[DK_MAX_LIGHTS];
uniform int m_NumLights;

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
    // ambient of neighbouring tiles across each tile (stored halved)
    vec3 tileAmbient = textureLod(m_AmbientMap, worldPos.xz / m_AmbientMapSize, 0.0).rgb * 2.0;
#else
    vec3 tileAmbient = m_Ambient.rgb;
#endif
    vec3 sum = tileAmbient + m_GlobalAmbient.rgb;
    for (int i = 0; i < DK_MAX_LIGHTS; i++) {
        if (i >= m_NumLights) {
            break;
        }
        vec4 posRadius = m_LightPosRadius[i];
        vec4 colorIntensity = m_LightColorIntensity[i];
        sum += dkPointLightContribution(worldPos, worldNormal, posRadius.xyz,
                posRadius.w, colorIntensity.rgb, colorIntensity.w, m_LightRadial[i] == 1);
    }

    vec3 lit = dkSoftClamp(sum);

    // Tile highlight (tag = blue glow, flash = red glow). Applied AFTER the
    // clamp: added before it, a tile already lit near 1.0 swallowed the tint.
    // Multiplying keeps the diffuse texture visible and the colour reads on
    // bright tiles; the small additive part makes it show on dark ones too.
    vec3 tint = m_Tint.rgb;
    if (tint.r + tint.g + tint.b > 0.0) {
        lit = lit * (vec3(0.35) + 1.4 * tint) + tint * 0.25;
    }
    vVertexColor = lit;
}
