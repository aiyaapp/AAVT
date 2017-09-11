precision mediump float;
varying vec2 vTextureCo;
uniform sampler2D uTexture;
void main() {
    vec4 color = texture2D( uTexture, vTextureCo);
    float c=(color.r+color.g+color.b)/3.0;
    gl_FragColor=vec4(c,c,c,1.0);
}