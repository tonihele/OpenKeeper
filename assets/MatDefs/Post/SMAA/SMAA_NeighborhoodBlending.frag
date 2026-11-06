#import "Common/ShaderLib/GLSLCompat.glsllib"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#import "MatDefs/Post/SMAA/SMAALib.glsllib"

uniform sampler2D m_Texture;
uniform sampler2D m_WeightsTexture;

varying vec2 texCoord;
varying vec4 offset0;

void main() {
    gl_FragColor = SMAANeighborhoodBlendingPS(texCoord, offset0, m_Texture, m_WeightsTexture);
}
