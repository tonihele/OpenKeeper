precision highp float;
precision highp sampler2D;

#extension GL_ARB_separate_shader_objects   : enable
#extension GL_ARB_explicit_uniform_location : enable

layout(location = 4) uniform float g_Time;

#ifdef OBJECTIVE_TEXTURE
    layout(location = 13) uniform sampler2D m_ObjectiveTexture;
#endif
    layout(location = 14) uniform sampler2D m_CenterTexture;
    layout(location = 15) uniform sampler2D m_HealthTexture;

#ifdef EXPERIENCE
    layout(location = 16) uniform float m_Experience;
#endif
layout(location = 17) uniform bool m_FlashColors;
layout(location = 18) uniform float m_FlashInterval;

layout(location = 0) in vec2 texCoord;

layout(location = 1) in vec4 color;
layout(location = 2) in vec4 color1;
layout(location = 3) in vec4 color2;
layout(location = 4) in vec4 color3;
layout(location = 5) in vec4 color4;
layout(location = 6) in vec4 color5;
layout(location = 7) in vec4 color6;
layout(location = 8) in vec4 color7;

layout(location = 0) out vec4 fragColor;

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
        vec4 centerTextureColor = texture(m_CenterTexture, vec2(uvCenter.x, 1.0 - uvCenter.y));
        finalColor = centerTextureColor + finalColor*(1.0-centerTextureColor[3]);
    }

    if (uvHealth.x >= 0.0 && uvHealth.x <= 1.0 && uvHealth.y >= 0.0 && uvHealth.y <= 1.0) {
        vec4 healthTextureColor = texture(m_HealthTexture, vec2(uvHealth.x, 1.0 - uvHealth.y));
        finalColor = healthTextureColor + finalColor*(1.0-healthTextureColor[3]);
    }

#ifdef OBJECTIVE_TEXTURE
    vec4 objectiveTextureColor = texture(m_ObjectiveTexture, vec2(texCoord.x, 1.0 - texCoord.y));
    
    // Some "dirt" on the objective texture, or I just can't pick a correct composition mode
    if(objectiveTextureColor[3] > 0.075) {
        finalColor = objectiveTextureColor + finalColor*(1.0-objectiveTextureColor[3]);
    }
#endif

    // Apply the coloring
    if(m_FlashColors) {
        vec4 allColors[numOfColors];
        allColors[0] = color1;
        allColors[1] = color2;
        allColors[2] = color3;
        allColors[3] = color4;
        allColors[4] = color5;
        allColors[5] = color6;
        allColors[6] = color7;
        finalColor *= allColors[int(mod(g_Time*m_FlashInterval,  float(numOfColors)))];
    } else {
        finalColor *= color;
    }

#ifdef EXPERIENCE
    // Apply the experience
    vec2 p = 2.0 * texCoord - 1.0;
    float kOffset = 0.5;
    float kRadius = 0.33;
    float kArc = 1.0 - m_Experience;
    float d = length( p );
    float angle = atan( p.x, p.y ) * (1.0/PI) * 0.5;
    angle = fract( angle - kOffset );
    float w = fwidth( d );
    float circle = smoothstep( kRadius + w, kRadius - w, d );
    float segment = step( angle, kArc );
    circle *= mix( segment, 1.0, step( 1.0, kArc ) );
    if(circle == 1.0) {
        finalColor -= vec4(0.2, 0.2, 0.2, 0.0); // Darken a bit
    }
#endif

    fragColor = finalColor;
}
