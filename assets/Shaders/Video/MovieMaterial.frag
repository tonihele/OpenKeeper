#if defined(DISCARD_ALPHA)
    uniform float m_AlphaDiscardThreshold;
#endif

uniform sampler2D m_TexLuma;
uniform sampler2D m_TexCr;
uniform sampler2D m_TexCb;

varying vec2 texCoord;

uniform vec2 m_AspectValues;
uniform vec2 m_ValidRange;

#ifdef LETTERBOX
uniform vec4 m_LetterboxColor;
#endif

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
	color = vec4(convert * vec3(texture2D(m_TexLuma,uv).r-(16.0/256.0), texture2D(m_TexCb,uv).r-0.5, texture2D(m_TexCr,uv).r - 0.5),1.0);
	
	}


#if defined(DISCARD_ALPHA)
    if(color.a < m_AlphaDiscardThreshold){
       discard;
    }
#endif

    gl_FragColor = color;
}