#version 330

precision mediump float;

in float frag_color;
in vec2 frag_texcoord;

uniform sampler2D texHandle;

void main() {
	gl_FragColor = texture2D(texHandle, frag_texcoord);
	gl_FragColor.rgb *= frag_color;
}
