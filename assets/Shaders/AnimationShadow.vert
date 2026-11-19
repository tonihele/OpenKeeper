#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Shaders/AnimationFrame.glsllib"
in vec3 inPosition;
in vec2 inTexCoord;
out vec2 texCoord;
#ifdef ANIMATED_TILES
uniform float g_Time;
uniform int m_NumberOfTiles;
uniform int m_Speed;
#endif
void main() {
    gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));
    texCoord = inTexCoord;
#ifdef ANIMATED_TILES
    texCoord.x = (texCoord.x + mod(float(animationFrame(g_Time,m_Speed)),float(m_NumberOfTiles))) / float(m_NumberOfTiles);
#endif
}
