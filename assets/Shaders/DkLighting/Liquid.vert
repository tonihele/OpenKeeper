// Reuse the terrain light calculation without changing non-liquid materials.
#import "Shaders/DkLighting/TerrainLighting.glsllib"

varying vec2 liquidUV;
varying vec2 environmentUV;
varying float liquidVisibility;

void main() {
    dkTerrainMain();
    liquidUV = inTexCoord;
    vec3 viewPosition = (g_WorldViewMatrix * vec4(inPosition, 1.0)).xyz;
    vec3 normal = normalize(g_NormalMatrix * inNormal);
    vec3 reflection = reflect(normalize(viewPosition), normal);
    float denominator = 2.0 * sqrt(dot(reflection.xy, reflection.xy)
            + (reflection.z + 1.0) * (reflection.z + 1.0));
    environmentUV = vec2(0.5) + reflection.xy / max(denominator, 0.0001);
#ifdef AMBIENT_MAP
    vec3 position = TransformWorld(vec4(inPosition, 1.0)).xyz;
    liquidVisibility = textureLod(m_AmbientMap, (position.xz + 0.5) / m_AmbientMapSize, 0.0).a;
#else
    liquidVisibility = 1.0;
#endif
}
