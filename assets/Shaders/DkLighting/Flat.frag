#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"

uniform sampler2D m_DiffuseMap;
uniform vec4 m_GlobalAmbient;
uniform vec4 m_FlatLight;
uniform vec4 m_Ambient;
uniform vec4 m_Tint;
uniform vec4 m_TintOverlay;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

varying vec2 texCoord;

void main() {
    vec4 diffuse = texture2D(m_DiffuseMap, texCoord);

    #ifdef DISCARD_ALPHA
        if (diffuse.a < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif

    vec3 sum = m_GlobalAmbient.rgb + m_FlatLight.rgb + m_Ambient.rgb;
    vec3 light = dkSoftClamp(sum);
    if (m_Tint.r + m_Tint.g + m_Tint.b > 0.0) {
        light = min(light + m_Tint.rgb, vec3(1.0));
    }
    vec3 lit = light * diffuse.rgb + m_TintOverlay.rgb * light;
    gl_FragColor = vec4(lit, diffuse.a);
}
