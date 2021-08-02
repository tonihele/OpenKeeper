#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform float g_Time;

uniform mat4 g_WorldViewProjectionMatrix;
uniform vec4 m_Color;
uniform vec4 m_Color1;
uniform vec4 m_Color2;
uniform vec4 m_Color3;
uniform vec4 m_Color4;
uniform vec4 m_Color5;
uniform vec4 m_Color6;
uniform vec4 m_Color7;

attribute vec3 inPosition;

#ifdef VERTEX_COLOR
    attribute vec4 inColor;
#endif

attribute vec2 inTexCoord;
varying vec2 texCoord;

varying vec4 color;
varying vec4 color1;
varying vec4 color2;
varying vec4 color3;
varying vec4 color4;
varying vec4 color5;
varying vec4 color6;
varying vec4 color7;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
    #ifdef VERTEX_COLOR
        color = m_Color * inColor;
        color1 = m_Color1 * inColor;
        color2 = m_Color2 * inColor;
        color3 = m_Color3 * inColor;
        color4 = m_Color4 * inColor;
        color5 = m_Color5 * inColor;
        color6 = m_Color6 * inColor;
        color7 = m_Color7 * inColor;
    #else
        color = m_Color;
        color1 = m_Color1;
        color2 = m_Color2;
        color3 = m_Color3;
        color4 = m_Color4;
        color5 = m_Color5;
        color6 = m_Color6;
        color7 = m_Color7;
    #endif
}