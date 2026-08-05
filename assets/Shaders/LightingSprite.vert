#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable

layout(location = 0) uniform mat4 g_WorldViewProjectionMatrix;
layout(location = 4) uniform float g_Time;
layout(location = 5) uniform int m_NumberOfTiles;
layout(location = 6) uniform int m_Speed;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;

layout(location = 0) out vec2 texCoord;

void main() {
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
   texCoord = inTexCoord;

   int iNumberOfTiles = int(m_NumberOfTiles);
   int selectedTile = int(g_Time * float(m_Speed));
   texCoord.x = float((texCoord.x + mod(float(selectedTile),  float(iNumberOfTiles))) / float(iNumberOfTiles));
}