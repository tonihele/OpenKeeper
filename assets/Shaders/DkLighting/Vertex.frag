#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DiffuseMap;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec3 vGlow;

void main() {
    vec4 diffuse = texture2D(m_DiffuseMap, texCoord);

    #ifdef DISCARD_ALPHA
        if (diffuse.a < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif

    vec3 colour = vVertexColor * diffuse.rgb;

    // Highlight overlay (tag / flash): a second additive pass of a flat
    // colour over the same triangles, lit by the same vertex colour
    colour += vOverlay * vVertexColor;

    // Surface glow (water / lava): added on top, and opaque enough to show
    colour += vGlow;
    float alpha = max(diffuse.a, max(max(vGlow.r, vGlow.g), vGlow.b));
    gl_FragColor = vec4(colour, alpha);
}
