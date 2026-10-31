#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DiffuseMap;
#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif
#ifdef ENHANCED
uniform sampler2D m_BumpMap;
uniform sampler2D m_EnvironmentMap;
uniform int m_BumpFrames;
uniform float m_EnhancedOpacity;
uniform float g_Time;
#endif
varying vec2 texCoord;
varying vec3 vVertexColor;
varying vec3 vOverlay;
varying vec2 liquidUV;
varying vec2 environmentUV;
varying float liquidVisibility;

void main() {
    vec4 diffuse = texture2D(m_DiffuseMap, texCoord);
#ifdef ENHANCED
    diffuse.a *= m_EnhancedOpacity;
#endif
#ifdef DISCARD_ALPHA
    if (diffuse.a < m_AlphaDiscardThreshold) discard;
#endif
    vec3 colour = vVertexColor * diffuse.rgb;
#ifdef ENHANCED
    // Explicitly wrap within one frame, keeping bilinear filtering out of its neighbours.
    vec2 size = vec2(textureSize(m_BumpMap, 0));
    float frames = float(m_BumpFrames);
    vec2 frameSize = vec2(size.x / frames, size.y);
    // DKII cycles the complete bump sequence in four seconds, independently of waves.
    // Use the same engine clock as Classic water and other animated sprites.
    float frame = floor(fract(g_Time * 0.25) * frames);
    vec2 pixels = fract(liquidUV) * (frameSize - 1.0) + 0.5;
    vec2 bumpUV = (pixels + vec2(frame * frameSize.x, 0.0)) / size;
    vec2 displacement = (texture2D(m_BumpMap, bumpUV).rg * 255.0 - 128.0) / 128.0;
    vec3 environment = texture2D(m_EnvironmentMap, clamp(environmentUV + displacement, 0.0, 1.0)).rgb;
    // Native fixed-function combination: lit base + perturbed environment map.
    colour += environment * liquidVisibility;
#endif
    colour += vOverlay * vVertexColor;
    gl_FragColor = vec4(colour, diffuse.a);
}
