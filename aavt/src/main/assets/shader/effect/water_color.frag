precision mediump float;

uniform sampler2D uTexture;
uniform float uWidth;
uniform float uHeight;
varying vec2 vTextureCo;



vec4 calculateColor(mat3 pos, vec2 filter_pos_delta[9],vec2 xy, vec2 texSize)
{
   vec4 final_color = vec4(0., 0., 0., 0.);

   for(int i=0; i<3; i++){
      for(int j=0; j<3; j++){
         vec2 new_xy = vec2(xy.x + filter_pos_delta[3*i+j].x,
            xy.y + filter_pos_delta[3*i+j].y);
         vec2 new_uv = vec2(new_xy.x / uWidth, new_xy.y / uHeight);
         final_color += (texture2D(uTexture, new_uv) * pos[i][j]);
      }
   }

   return final_color;
}

void main(void)
{
   vec2 xy = vec2(vTextureCo.x * uWidth, vTextureCo.y * uHeight);
   vec4 color = calculateColor(mat3(1./16., 1./8.,1./16.,
                                1./8.,1./4.,1./8.,
                                1./16.,1./8.,1./16.),
                                vec2[9](vec2(-1., -1.), vec2(0., -1.),vec2(1., -1.),
                                vec2(-1., 0.),vec2(0., 0.), vec2(1., 0.),
                                vec2(-1., 1.), vec2(0., 1.), vec2(1., 1.)),
                                xy , vec2(uWidth,uHeight));

   gl_FragColor = color;
}