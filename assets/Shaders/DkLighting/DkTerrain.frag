#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DiffuseMap;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

varying vec2 texCoord;
varying vec3 vVertexColor;

void main() {
    vec4 diffuse = texture2D(m_DiffuseMap, texCoord);

    #ifdef DISCARD_ALPHA
        if (diffuse.a < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif

    gl_FragColor = vec4(vVertexColor * diffuse.rgb, diffuse.a);
}
