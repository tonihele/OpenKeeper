#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

attribute vec3 inPosition;
attribute vec2 inTexCoord;

// Animation
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;

varying vec2 texCoord;

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
  
   #ifdef NUM_MORPH_TARGETS
           Morph_Compute(modelSpacePos, modelSpaceNorm);
   #endif

   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;

   // Animation
   int iNumberOfTiles = int(m_NumberOfTiles);

   int selectedTile = 0;

   selectedTile += int(g_Time*float(m_Speed));

   texCoord.x = (float((texCoord.x + mod(float(selectedTile),  float(iNumberOfTiles))) / float(iNumberOfTiles))); ///selectedTile;
   // Animation end
}