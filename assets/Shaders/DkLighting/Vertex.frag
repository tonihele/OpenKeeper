#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DiffuseMap;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec3 vGlow;
varying float vSurfaceVisibility;
#ifdef EMBM_ENABLED
varying vec2 liquidTexCoord;
varying vec2 embmEnvironmentTexCoord;
uniform sampler2D m_EmbmBaseMap;
uniform sampler2D m_EmbmEnvironmentMap;
uniform vec4 m_EmbmBumpMatrix;
uniform float m_EmbmReflectionIntensity;
uniform float m_EmbmBumpAnimationSpeed;
uniform int m_EmbmBumpFrameCount;
uniform float m_EmbmLuminanceScale;
uniform float m_EmbmLuminanceOffset;
uniform float m_EmbmBaseAlpha;
uniform float m_EmbmBaseColorMix;
uniform bool m_EmbmUseLuminance;
uniform bool m_EmbmUseSphereEnvironmentUv;
uniform float g_Time;
#ifdef EMBM_BUMP_MAP
uniform sampler2D m_EmbmBumpMap;
#endif
#ifdef EMBM_BUMP_FRAMES
uniform sampler2DArray m_EmbmBumpFrames;
#endif
#endif

void main() {
    vec4 diffuse = texture2D(m_DiffuseMap, texCoord);
    vec3 embmReflection = vec3(0.0);
#ifdef EMBM_ENABLED
    vec4 bump;
#ifdef EMBM_BUMP_FRAMES
    // DKII's 32 water bump frames cycle at 8 fps (four seconds per loop).
    float frame = mod(floor(g_Time * m_EmbmBumpAnimationSpeed),
            float(max(m_EmbmBumpFrameCount, 1)));
    bump = texture(m_EmbmBumpFrames, vec3(liquidTexCoord, frame));
#else
    bump = texture2D(m_EmbmBumpMap, liquidTexCoord);
#endif
    // Dk2EmbmResources converts the original grayscale height image into
    // signed dU/dV channels once at load time. Keep the linear data as-is.
    vec2 bumpDelta = bump.rg * 2.0 - 1.0;
    mat2 bumpMatrix = mat2(m_EmbmBumpMatrix.x, m_EmbmBumpMatrix.z,
            m_EmbmBumpMatrix.y, m_EmbmBumpMatrix.w);
    vec2 environmentBaseUv = m_EmbmUseSphereEnvironmentUv
            ? embmEnvironmentTexCoord : liquidTexCoord;
    vec2 environmentUv = environmentBaseUv + bumpMatrix * bumpDelta;
    vec4 embmBase = texture2D(m_EmbmBaseMap, liquidTexCoord);
    diffuse.rgb = mix(diffuse.rgb, embmBase.rgb, clamp(m_EmbmBaseColorMix, 0.0, 1.0));
    diffuse.a = mix(diffuse.a, embmBase.a, clamp(m_EmbmBaseAlpha, 0.0, 1.0));
    float luminance = m_EmbmUseLuminance
            ? max(0.0, bump.a * m_EmbmLuminanceScale + m_EmbmLuminanceOffset)
            : 1.0;
    embmReflection = texture2D(m_EmbmEnvironmentMap, environmentUv).rgb
            * (m_EmbmReflectionIntensity * luminance * clamp(vSurfaceVisibility, 0.0, 1.0));
#endif

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
    colour += embmReflection;
    float alpha = max(diffuse.a, max(max(vGlow.r, vGlow.g), vGlow.b));
    gl_FragColor = vec4(colour, alpha);
}
