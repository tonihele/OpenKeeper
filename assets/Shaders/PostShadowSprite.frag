#extension GL_ARB_separate_shader_objects   : enable

#import "Common/ShaderLib/Shadows.glsllib"

layout(location = 0) out vec4 fragColor;
layout(location = 0) in vec4 projCoord0;
layout(location = 1) in vec4 projCoord1;
layout(location = 2) in vec4 projCoord2;
layout(location = 3) in vec4 projCoord3;

#ifdef POINTLIGHT
layout(location = 4) in vec4 projCoord4;
layout(location = 5) in vec4 projCoord5;
layout(location = 6) in vec4 worldPos;
uniform vec3 m_LightPos;
#else
#ifndef PSSM
    layout(location = 7) in float lightDot;
#endif
#endif
#if defined(PSSM) || defined(FADE)
layout(location = 8) in float shadowPosition;
#endif

#ifdef DISCARD_ALPHA
    #ifdef COLOR_MAP
        uniform sampler2D m_ColorMap;
    #else    
        uniform sampler2D m_DiffuseMap;
    #endif
    uniform float m_AlphaDiscardThreshold;
    layout(location = 9) in vec2 texCoord;
#endif
#ifndef BACKFACE_SHADOWS
layout(location = 10) in float nDotL;
#endif

#ifdef FADE
uniform vec2 m_FadeInfo;
#endif

void main(){   
 
    #ifdef DISCARD_ALPHA
        #ifdef COLOR_MAP
            float alpha = texture(m_ColorMap,texCoord).a;
        #else
            float alpha = texture(m_DiffuseMap,texCoord).a;
        #endif
        if(alpha<=m_AlphaDiscardThreshold){
            discard;
        }
    #endif

    #ifndef BACKFACE_SHADOWS
        if(nDotL > 0.0){
            discard;
        }
    #endif


    float shadow = 1.0;
 
    #ifdef POINTLIGHT         
            shadow = getPointLightShadows(worldPos, m_LightPos,
                           m_ShadowMap0,m_ShadowMap1,m_ShadowMap2,m_ShadowMap3,m_ShadowMap4,m_ShadowMap5,
                           projCoord0, projCoord1, projCoord2, projCoord3, projCoord4, projCoord5);
    #else
       #ifdef PSSM
            shadow = getDirectionalLightShadows(m_Splits, shadowPosition,
                           m_ShadowMap0,m_ShadowMap1,m_ShadowMap2,m_ShadowMap3,
                           projCoord0, projCoord1, projCoord2, projCoord3);
       #else 
            //spotlight
            if(lightDot < 0.0){
                fragColor = vec4(1.0);
                return;
            }
            shadow = getSpotLightShadows(m_ShadowMap0,projCoord0);
       #endif
    #endif   

    #ifdef FADE
        shadow = max(0.0, mix(shadow, 1.0, max(0.0, (shadowPosition - m_FadeInfo.x) * m_FadeInfo.y)));
    #endif

    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    fragColor = vec4(shadow, shadow, shadow, 1.0);

}

