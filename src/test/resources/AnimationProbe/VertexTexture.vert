uniform sampler2D m_PoseData;
uniform vec3 m_Frames;
uniform float m_Width;
in vec3 inPosition;
flat out vec3 sampledPosition;
void main() {
    vec3 a = texelFetch(m_PoseData, ivec2(gl_VertexID, int(m_Frames.x)), 0).xyz;
    vec3 b = texelFetch(m_PoseData, ivec2(gl_VertexID, int(m_Frames.y)), 0).xyz;
    sampledPosition = a + (b - a) * m_Frames.z;
    gl_Position = vec4((float(gl_VertexID) + 0.5) / m_Width * 2.0 - 1.0, 0.0, 0.0, 1.0);
    gl_PointSize = 1.0;
}
