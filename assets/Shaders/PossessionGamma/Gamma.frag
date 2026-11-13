#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform sampler2D m_Frame;
uniform sampler2D m_Ramps;
uniform int m_Effect;
uniform bool m_OutputSrgb;
varying vec2 uv;

vec3 toLinear(vec3 encoded) {
    return mix(encoded / 12.92, pow((encoded + 0.055) / 1.055, vec3(2.4)),
               step(vec3(0.04045), encoded));
}

vec3 toSrgb(vec3 linear) {
    return mix(linear * 12.92, 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055,
               step(vec3(0.0031308), linear));
}

void main() {
    vec4 source = texture2D(m_Frame, uv);
    // OpenGL's sRGB framebuffer blit decodes the source into our linear target.
    // Recover its display values before selecting the native 8-bit ramp indices.
    vec3 display = m_OutputSrgb ? toSrgb(source.rgb) : source.rgb;
    ivec3 index = ivec3(floor(clamp(display, 0.0, 1.0) * 255.0 + 0.5));
    vec3 colour = vec3(texelFetch(m_Ramps, ivec2(index.r, m_Effect), 0).r,
                       texelFetch(m_Ramps, ivec2(index.g, m_Effect), 0).g,
                       texelFetch(m_Ramps, ivec2(index.b, m_Effect), 0).b);
    // The native tables act on display values. Undo only the output framebuffer's
    // sRGB encoding here so OpenGL does not encode their result a second time.
    gl_FragColor = vec4(m_OutputSrgb ? toLinear(colour) : colour, source.a);
}
