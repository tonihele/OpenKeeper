#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable

precision highp float;
precision highp sampler2D;

layout(location = 0) uniform mat4 g_WorldViewProjectionMatrix;
layout(location = 4) uniform vec4 m_Color;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
#ifdef VERTEX_COLOR
layout(location = 2) in vec4 inColor;
#endif

layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec4 color;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
    #ifdef VERTEX_COLOR
        color = m_Color * inColor;
    #else
        color = m_Color;
    #endif
}