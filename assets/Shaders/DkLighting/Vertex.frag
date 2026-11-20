#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DiffuseMap;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec3 vGlow;
varying vec2 liquidTexCoord;
varying vec2 embmEnvironmentTexCoord;
varying float vSurfaceVisibility;

#ifdef EMBM_ENABLED
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
uniform bool m_EmbmUseHeightMap;
uniform bool m_EmbmUseSphereEnvironmentUv;
uniform bool m_EmbmUseSurfaceLighting;
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
    vec2 bumpUv = liquidTexCoord;
    vec4 bump;
#ifdef EMBM_BUMP_FRAMES
    float frame = mod(floor(g_Time * m_EmbmBumpAnimationSpeed), float(max(m_EmbmBumpFrameCount, 1)));
    bump = texture(m_EmbmBumpFrames, vec3(bumpUv, frame));
#else
    bumpUv += vec2(g_Time * m_EmbmBumpAnimationSpeed,
            -g_Time * m_EmbmBumpAnimationSpeed * 0.5);
    bump = texture2D(m_EmbmBumpMap, bumpUv);
#endif
    vec2 bumpDelta = bump.rg * 2.0 - 1.0;
    if (m_EmbmUseHeightMap) {
        vec2 bumpSize;
#ifdef EMBM_BUMP_FRAMES
        bumpSize = vec2(textureSize(m_EmbmBumpFrames, 0).xy);
#else
        bumpSize = vec2(textureSize(m_EmbmBumpMap, 0));
#endif
        vec2 bumpTexel = 1.0 / bumpSize;
        vec2 leftUv = bumpUv - vec2(bumpTexel.x, 0.0);
        vec2 rightUv = bumpUv + vec2(bumpTexel.x, 0.0);
        vec2 downUv = bumpUv - vec2(0.0, bumpTexel.y);
        vec2 upUv = bumpUv + vec2(0.0, bumpTexel.y);
#ifdef EMBM_BUMP_FRAMES
        float leftHeight = texture(m_EmbmBumpFrames, vec3(leftUv, frame)).r;
        float rightHeight = texture(m_EmbmBumpFrames, vec3(rightUv, frame)).r;
        float downHeight = texture(m_EmbmBumpFrames, vec3(downUv, frame)).r;
        float upHeight = texture(m_EmbmBumpFrames, vec3(upUv, frame)).r;
#else
        float leftHeight = texture2D(m_EmbmBumpMap, leftUv).r;
        float rightHeight = texture2D(m_EmbmBumpMap, rightUv).r;
        float downHeight = texture2D(m_EmbmBumpMap, downUv).r;
        float upHeight = texture2D(m_EmbmBumpMap, upUv).r;
#endif
        // Convert the finite difference to height slope per UV unit before
        // applying the EMBM matrix. This preserves a stable offset across map
        // resolutions while keeping the original height field as the input.
        bumpDelta = clamp(0.5 * vec2(rightHeight - leftHeight, upHeight - downHeight) * bumpSize,
                vec2(-2.0), vec2(2.0));
    }
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
    vec3 reflectionLighting = m_EmbmUseSurfaceLighting ? vVertexColor : vec3(1.0);
    embmReflection = texture2D(m_EmbmEnvironmentMap, environmentUv).rgb
            * reflectionLighting
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
