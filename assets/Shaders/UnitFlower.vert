#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable

layout(location = 0) uniform mat4 g_WorldViewProjectionMatrix;
layout(location = 4) uniform float g_Time;

layout(location = 5) uniform vec4 m_Color;
layout(location = 6) uniform vec4 m_Color1;
layout(location = 7) uniform vec4 m_Color2;
layout(location = 8) uniform vec4 m_Color3;
layout(location = 9) uniform vec4 m_Color4;
layout(location = 10) uniform vec4 m_Color5;
layout(location = 11) uniform vec4 m_Color6;
layout(location = 12) uniform vec4 m_Color7;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
#ifdef VERTEX_COLOR
    layout(location = 2) in vec4 inColor;
#endif
layout(location = 0) out vec2 texCoord;

layout(location = 1) out vec4 color;
layout(location = 2) out vec4 color1;
layout(location = 3) out vec4 color2;
layout(location = 4) out vec4 color3;
layout(location = 5) out vec4 color4;
layout(location = 6) out vec4 color5;
layout(location = 7) out vec4 color6;
layout(location = 8) out vec4 color7;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
    #ifdef VERTEX_COLOR
        color = m_Color * inColor;
        color1 = m_Color1 * inColor;
        color2 = m_Color2 * inColor;
        color3 = m_Color3 * inColor;
        color4 = m_Color4 * inColor;
        color5 = m_Color5 * inColor;
        color6 = m_Color6 * inColor;
        color7 = m_Color7 * inColor;
    #else
        color = m_Color;
        color1 = m_Color1;
        color2 = m_Color2;
        color3 = m_Color3;
        color4 = m_Color4;
        color5 = m_Color5;
        color6 = m_Color6;
        color7 = m_Color7;
    #endif
}