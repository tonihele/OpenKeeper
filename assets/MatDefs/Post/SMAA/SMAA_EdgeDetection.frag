#import "Common/ShaderLib/GLSLCompat.glsllib"
#define SMAA_INCLUDE_VS 0
#define SMAA_INCLUDE_PS 1
#import "MatDefs/Post/SMAA/SMAALib.glsllib"

uniform sampler2D m_Texture;

varying vec2 texCoord;
varying vec4 offset0;
varying vec4 offset1;
varying vec4 offset2;

void main() {
    // Colour-edge detection (Ultra): catches luma and chroma-only edges.
    vec4 offsets[3];
    offsets[0] = offset0;
    offsets[1] = offset1;
    offsets[2] = offset2;
    vec2 edges = SMAAColorEdgeDetectionPS(texCoord, offsets, m_Texture);
    gl_FragColor = vec4(edges, 0.0, 1.0);
}
