#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable

precision highp float;
precision highp sampler2D;

#ifdef TEXTURE
layout(location = 6) uniform sampler2D m_Texture;
#endif

layout(location = 5) uniform float m_Progress;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 color;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265358979323846264;

void main() {
    vec2 uv = texCoord.xy;
    uv -= vec2(0.5, 0.5);
    float sweep = (-atan(uv.x,-uv.y) + PI) / (2.*PI);

    #ifdef TEXTURE
      if (sweep < m_Progress) {
        vec4 texVal = texture(m_Texture, texCoord);
        fragColor = texVal * color;
      } else
        discard;

    #else
      if (sweep < m_Progress)
        fragColor = color;
      else
        discard;

    #endif
}
