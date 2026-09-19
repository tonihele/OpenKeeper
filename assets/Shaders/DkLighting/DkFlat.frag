#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Shaders/DkLighting/DkFalloff.glsllib"

uniform sampler2D m_DiffuseMap;
uniform vec4 m_GlobalAmbient;
uniform vec4 m_FlatLight;
uniform vec4 m_Ambient;

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
    vec3 lit = dkSoftClamp(sum) * diffuse.rgb;
    gl_FragColor = vec4(lit, diffuse.a);
}
