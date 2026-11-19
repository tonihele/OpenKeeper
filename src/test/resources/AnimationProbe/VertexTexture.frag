flat in vec3 sampledPosition;
out vec4 fragColor;
void main() { fragColor = vec4(sampledPosition, 1.0); }
