#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform float g_Time;

#ifdef OBJECTIVE_TEXTURE
    uniform sampler2D m_ObjectiveTexture;
#endif

    uniform sampler2D m_CenterTexture;
    uniform sampler2D m_HealthTexture;

varying vec4 color;
varying vec2 texCoord;

#ifdef EXPERIENCE
    uniform float m_Experience;
#endif

uniform bool m_FlashColors;
uniform float m_FlashInterval;
varying vec4 color1;
varying vec4 color2;
varying vec4 color3;
varying vec4 color4;
varying vec4 color5;
varying vec4 color6;
varying vec4 color7;

const float PI = 3.14159265358979323846264;
const int numOfColors = 7;

void main() {

    // Scale the center and health, they are half the size of the canvas
    vec2 scaleCenter = vec2(0.5, 0.5);
    vec2 uvHealth = (texCoord - scaleCenter) * 2.0 + scaleCenter;
    vec2 uvCenter = (texCoord - scaleCenter) * 2.0 + scaleCenter;
    
    vec4 finalColor = vec4(0.0, 0.0, 0.0, 0.0);

    // Draw the textures with Porter-Duff Source Over Destination rule
    if (uvCenter.x >= 0.0 && uvCenter.x <= 1.0 && uvCenter.y >= 0.0 && uvCenter.y <= 1.0) {
        vec4 centerTextureColor = texture2D(m_CenterTexture, vec2(uvCenter.x, 1.0 - uvCenter.y));
        finalColor = centerTextureColor + finalColor*(1.0-centerTextureColor[3]);
    }

    if (uvHealth.x >= 0.0 && uvHealth.x <= 1.0 && uvHealth.y >= 0.0 && uvHealth.y <= 1.0) {
        vec4 healthTextureColor = texture2D(m_HealthTexture, vec2(uvHealth.x, 1.0 - uvHealth.y));
        finalColor = healthTextureColor + finalColor*(1.0-healthTextureColor[3]);
    }

#ifdef OBJECTIVE_TEXTURE
    vec4 objectiveTextureColor = texture2D(m_ObjectiveTexture, vec2(texCoord.x, 1.0 - texCoord.y));
    
    // Some "dirt" on the objective texture, or I just can't pick a correct composition mode
    if(objectiveTextureColor[3] > 0.075) {
        finalColor = objectiveTextureColor + finalColor*(1.0-objectiveTextureColor[3]);
    }
#endif

    // Apply the coloring
    if(m_FlashColors) {
        vec4[numOfColors] allColors = vec4[numOfColors](color1, color2, color3, color4, color5, color6, color7);
        finalColor *= allColors[int(mod(g_Time*m_FlashInterval,  float(numOfColors)))];
    } else {
        finalColor *= color;
    }

#ifdef EXPERIENCE
    // Apply the experience
    vec2 p = 2.0 * texCoord - 1.0;
    float kOffset = 0.5;
    float kRadius = 0.33;
    float kArc = 1 - m_Experience;
    float d = length( p );
    float angle = atan( p.x, p.y ) * (1.0/PI) * 0.5;
    angle = fract( angle - kOffset );
    float w = fwidth( d );
    float circle = smoothstep( kRadius + w, kRadius - w, d );
    float segment = step( angle, kArc );
    circle *= mix( segment, 1.0, step( 1.0, kArc ) );
    if(circle == 1.0) {
        finalColor -= vec4(0.25, 0.25, 0.25, 0.0); // Darken a bit
    }
#endif

    gl_FragColor = finalColor;
}
