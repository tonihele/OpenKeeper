#import "Common/ShaderLib/GLSLCompat.glsllib"
#define SMAA_INCLUDE_VS 1
#define SMAA_INCLUDE_PS 0
#import "MatDefs/Post/SMAA/SMAALib.glsllib"

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec2 pixcoord;
varying vec4 offset0;
varying vec4 offset1;
varying vec4 offset2;

void main() {
    texCoord = inTexCoord;
    pixcoord = vec2(0.0);
    offset0 = vec4(0.0);
    offset1 = vec4(0.0);
    offset2 = vec4(0.0);

#ifdef VS_EDGEDETECTION
    {
        vec4 offsets[3];
        SMAAEdgeDetectionVS(texCoord, offsets);
        offset0 = offsets[0];
        offset1 = offsets[1];
        offset2 = offsets[2];
    }
#endif

#ifdef VS_BLENDING
    {
        vec4 offsets[3];
        SMAABlendingWeightCalculationVS(texCoord, pixcoord, offsets);
        offset0 = offsets[0];
        offset1 = offsets[1];
        offset2 = offsets[2];
    }
#endif

#ifdef VS_NEIGHBORHOODBLENDING
    offset0 = mad(SMAA_RT_METRICS.xyxy, vec4(1.0, 0.0, 0.0, 1.0), texCoord.xyxy);
#endif

    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);
}
