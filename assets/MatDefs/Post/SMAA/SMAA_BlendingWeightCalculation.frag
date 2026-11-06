#import "Common/ShaderLib/GLSLCompat.glsllib"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#import "MatDefs/Post/SMAA/SMAALib.glsllib"

uniform sampler2D m_EdgesTexture;
uniform sampler2D m_AreaTexture;
uniform sampler2D m_SearchTexture;

varying vec2 texCoord;
varying vec2 pixcoord;
varying vec4 offset0;
varying vec4 offset1;
varying vec4 offset2;

void main() {
    vec4 offsets[3];
    offsets[0] = offset0;
    offsets[1] = offset1;
    offsets[2] = offset2;
    gl_FragColor = SMAABlendingWeightCalculationPS(texCoord, pixcoord, offsets,
            m_EdgesTexture, m_AreaTexture, m_SearchTexture, vec4(0.0));
}
