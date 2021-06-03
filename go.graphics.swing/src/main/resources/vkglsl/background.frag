#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (location=0) in float frag_color;
layout (location=1) in vec2 frag_texcoord;

layout(constant_id=0) const int MAX_TEXTURE_COUNT = 2;
layout (set=1, binding=0) uniform sampler2D texHandle;

layout(push_constant) uniform LocalData {
	int globalTransIndex;
} local;

layout (location=0) out vec4 out_color;

void main() {
	vec4 tmp_out_color = texture(texHandle, frag_texcoord);
	tmp_out_color.rgb *= frag_color;

	out_color = tmp_out_color;
}
