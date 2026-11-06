#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform mat4 g_WorldViewProjectionMatrix;
attribute vec3 inPosition;
attribute vec2 inTexCoord;
varying vec2 uv;
void main(){uv=inTexCoord;gl_Position=g_WorldViewProjectionMatrix*vec4(inPosition,1.0);}
