uniform mat4 g_WorldViewProjectionMatrix;

uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;

in vec3 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main() {
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
   texCoord = inTexCoord;

   int iNumberOfTiles = int(m_NumberOfTiles);
   int selectedTile = int(g_Time * float(m_Speed));
   texCoord.x = float((texCoord.x + mod(float(selectedTile),  float(iNumberOfTiles))) / float(iNumberOfTiles));
}