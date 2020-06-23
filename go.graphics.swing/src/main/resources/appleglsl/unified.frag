#version 330

#extension GL_NV_fragdepth : enable

in vec2 frag_texcoord;

uniform sampler2D texHandle;
uniform float shadow_depth;

uniform lowp int mode;
uniform float color[5]; // r,g,b,a, intensity

out vec4 fragColor;

void main() {
	float fragDepth = gl_FragCoord.z;
	vec4 fragColorTemp = vec4(color[0], color[1], color[2], color[3]);

	bool textured = mode!=0;

	if(textured) {
		bool progress_fence = mode > 3;

		vec4 tex_color;
		if(progress_fence) {
			tex_color = texture(texHandle, fragColorTemp.rg+(fragColorTemp.ba-fragColorTemp.rg)*frag_texcoord);
		} else {
			tex_color = texture(texHandle, frag_texcoord);
		}

		bool image_fence = mode>0;
		bool torso_fence = mode>1 && !progress_fence;
		bool shadow_fence = abs(float(mode))>2.0 && !progress_fence;

		if(torso_fence && tex_color.a < 0.1 && tex_color.r > 0.1) { // torso pixel
			fragColorTemp.rgb *= tex_color.b;
		} else if(shadow_fence && tex_color.a < 0.1 && tex_color.g > 0.1) { // shadow pixel
			fragColorTemp.rgba = tex_color.aaag;
			fragDepth += shadow_depth;
		} else if(image_fence) { // image pixel
			if(!torso_fence && !shadow_fence && !progress_fence) {
				fragColorTemp *= tex_color;
			} else {
				fragColorTemp = tex_color;
			}
		}
	}

	if(fragColorTemp.a < 0.5) discard;

	fragColorTemp.rgb *= color[4];

	fragColor = fragColorTemp;

	#ifdef GL_NV_fragdepth
	gl_FragDepth = fragDepth;
	#else
	#ifndef GL_ES
	gl_FragDepth = fragDepth;
	#endif
	#endif
}
