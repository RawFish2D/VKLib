#version 460

layout (binding = 1) uniform sampler2D u_texture;

layout (location = 0) out vec4 outColor;

layout (location = 0) in FragData {
	vec2 texCoord;
} inData;

void main() {
	vec4 color = texture(u_texture, inData.texCoord);
	if (color.a < 0.001) {
		discard;
	}
	else {
		outColor = color;
	}
	//	outColor = vec4(1.0, 0.0, 1.0, 1.0);
}