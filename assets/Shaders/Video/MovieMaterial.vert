#extension GL_ARB_separate_shader_objects   : enable
#extension GL_ARB_explicit_uniform_location : enable

layout(location = 0) uniform mat4 g_WorldViewProjectionMatrix;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;

layout(location = 0) out vec2 texCoord;

void main(){
    
    texCoord = inTexCoord;
    gl_Position = g_WorldViewProjectionMatrix*vec4(inPosition,1.0);
}