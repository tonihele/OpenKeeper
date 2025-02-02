#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

// Animation
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;

out vec4 projCoord0;
out vec4 projCoord1;
out vec4 projCoord2;
out vec4 projCoord3;

#ifdef POINTLIGHT
    uniform mat4 m_LightViewProjectionMatrix4;
    uniform mat4 m_LightViewProjectionMatrix5;
    uniform vec3 m_LightPos;
    out vec4 projCoord4;
    out vec4 projCoord5;
    out vec4 worldPos;
#else
    uniform vec3 m_LightDir;
    #ifndef PSSM
        uniform vec3 m_LightPos;
        out float lightDot;
    #endif
#endif

#if defined(PSSM) || defined(FADE)
out float shadowPosition;
#endif

out vec2 texCoord;

in vec3 inPosition;

#ifndef BACKFACE_SHADOWS
    in vec3 inNormal;
    out float nDotL;
#endif

#ifdef DISCARD_ALPHA
    in vec2 inTexCoord;
#endif

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


void main(){
   vec4 modelSpacePos = vec4(inPosition, 1.0);

   #ifdef NUM_MORPH_TARGETS
       Morph_Compute(modelSpacePos);
   #endif

   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    vec3 lightDir;

    #if defined(PSSM) || defined(FADE)
         shadowPosition = gl_Position.z;
    #endif

    #ifndef POINTLIGHT
        vec4 worldPos=vec4(0.0);
    #endif
    // get the vertex in world space
    worldPos = TransformWorld(modelSpacePos);

    #ifdef DISCARD_ALPHA
       texCoord = inTexCoord;

        // Animation
        int iNumberOfTiles = int(m_NumberOfTiles);

        int selectedTile = 0;

        selectedTile += int(g_Time*float(m_Speed));

        texCoord.x = (float((texCoord.x + mod(float(selectedTile),  float(iNumberOfTiles))) / float(iNumberOfTiles))); ///selectedTile;
        // Animation end
    #endif
    // populate the light view matrices array and convert vertex to light viewProj space
    projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;
    #ifdef POINTLIGHT
        projCoord4 = biasMat * m_LightViewProjectionMatrix4 * worldPos;
        projCoord5 = biasMat * m_LightViewProjectionMatrix5 * worldPos;
    #else
        #ifndef PSSM
            //Spot light
            lightDir = worldPos.xyz - m_LightPos;
            lightDot = dot(m_LightDir,lightDir);
        #endif
    #endif

    #ifndef BACKFACE_SHADOWS
        vec3 normal = normalize(TransformWorld(vec4(inNormal,0.0))).xyz;
        #ifdef POINTLIGHT
            lightDir = worldPos.xyz - m_LightPos;
        #else
            #ifdef PSSM
               lightDir = m_LightDir;
            #endif
        #endif
        nDotL = dot(normal, lightDir);
    #endif
}