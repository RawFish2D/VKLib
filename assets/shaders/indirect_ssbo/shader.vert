#version 460

//#extension GL_ARB_shader_draw_parameters : enable
//#extension SPV_KHR_shader_draw_parameters : enable
//#extension VK_KHR_shader_draw_parameters : enable
//#extension GL_KHR_vulkan_glsl : enable

layout (location = 0) in vec2 aVert;
layout (location = 1) in vec2 aUV;

layout (location = 0) out FragData {
	vec2 texCoord;
} outData;

layout (binding = 0) uniform UniformBufferObject {
	vec2 resolution;
} inData;

layout (std430, set = 0, binding = 2) readonly buffer SSBO {
	vec2 pos[];
} ssbo;

void main() {
	vec2 pos = ssbo.pos[gl_InstanceIndex];
	float x = (aVert.x + pos.x) / (inData.resolution.x * 0.5) - 1.0;
	float y = (aVert.y + pos.y) / (inData.resolution.y * 0.5) - 1.0;
	gl_Position = vec4(x, -y, 0.0, 1.0);
	outData.texCoord = aUV;
}