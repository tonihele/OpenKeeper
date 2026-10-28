#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef TEXTURE
    uniform sampler2D m_Texture;
#endif

varying vec4 color;
varying vec2 texCoord;

uniform float m_Progress;

float PI = 3.14159265358979323846264;

void main() {
    vec2 uv = texCoord.xy;
    uv -= vec2(0.5, 0.5);
    float sweep = (-atan(uv.x,-uv.y) + PI) / (2.*PI);

    #ifdef TEXTURE
      if (sweep < m_Progress) {
        vec4 texVal = texture2D(m_Texture, texCoord);
        gl_FragColor = texVal * color;
      } else
        discard;

    #else
      if (sweep < m_Progress)
        gl_FragColor = color;
      else
        discard;

    #endif
}
