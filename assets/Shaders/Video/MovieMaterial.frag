#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable

precision highp float;
precision highp sampler2D;

#if defined(DISCARD_ALPHA)
    layout(location = 7) uniform float m_AlphaDiscardThreshold;
#endif

layout(location = 4) uniform sampler2D m_TexLuma;
layout(location = 5) uniform sampler2D m_TexCr;
layout(location = 6) uniform sampler2D m_TexCb;

layout(location = 7) uniform bool m_NoFrame;

layout(location = 8) uniform vec2 m_AspectValues;
layout(location = 9) uniform vec2 m_ValidRange;

#ifdef LETTERBOX
layout(location = 10) uniform vec4 m_LetterboxColor;
#endif

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

mat3 convert = mat3(
		1.164, 1.164, 1.164,
		0.0, -0.392, 2.017,
		1.596, -0.813, 0.0
	);



void main(){

    vec2 uv = vec2(texCoord.x,1.0-texCoord.y);

    vec4 color = vec4(0.0);

    uv = vec2(
    	(uv.x*m_AspectValues.x - (m_AspectValues.x-1.0)/2.0)*m_ValidRange.x,
    	(uv.y*m_AspectValues.y - (m_AspectValues.y-1.0)/2.0)*m_ValidRange.y
    );


#ifdef LETTERBOX
	if ( uv.x < 0.0 || uv.x > m_ValidRange.x || uv.y < 0.0 || uv.y > m_ValidRange.y ) {
		color = m_LetterboxColor;
	}
	else
#endif
	{

        if(!m_NoFrame) {
            color = vec4(convert * vec3(texture(m_TexLuma,uv).r-(16.0/256.0), texture(m_TexCb,uv).r-0.5, texture(m_TexCr,uv).r - 0.5),1.0);
	}

        }


#if defined(DISCARD_ALPHA)
    if(color.a < m_AlphaDiscardThreshold){
       discard;
    }
#endif

    fragColor = color;
}