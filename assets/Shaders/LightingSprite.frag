#import "Common/ShaderLib/GLSLCompat.glsllib"

in vec2 texCoord;

uniform sampler2D m_DiffuseMap;
uniform float m_AlphaDiscardThreshold;

void main()
{
  vec2 newTexCoord;
  newTexCoord = texCoord;
  vec4 diffuseColor = texture2D(m_DiffuseMap, newTexCoord);
  #ifdef DISCARD_ALPHA
      if (all(lessThan(diffuseColor.rgb, vec3(m_AlphaDiscardThreshold))))
          discard;
  #endif

  gl_FragColor = vec4(diffuseColor.rgb, 1.0);
}
