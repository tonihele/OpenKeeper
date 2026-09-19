#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"

// Must match SceneLights.MAX_LIGHTS_PER_MATERIAL.
#define DK_MAX_LIGHTS 16

uniform vec4 m_Ambient;
uniform vec4 m_GlobalAmbient;
uniform vec4 m_LightPosRadius[DK_MAX_LIGHTS];
uniform vec4 m_LightColorIntensity[DK_MAX_LIGHTS];
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

    vec3 sum = m_Ambient.rgb + m_GlobalAmbient.rgb;
    for (int i = 0; i < DK_MAX_LIGHTS; i++) {
        if (i >= m_NumLights) {
            break;
        }
        vec4 posRadius = m_LightPosRadius[i];
        vec4 colorIntensity = m_LightColorIntensity[i];
        sum += dkPointLightContribution(worldPos, worldNormal, posRadius.xyz,
                posRadius.w, colorIntensity.rgb, colorIntensity.w);
    }

    vVertexColor = dkSoftClamp(sum);
}
