precision highp float;

varying vec2 vTextureCo;
uniform sampler2D uTexture;

//为了简化计算，宽高都必须为8的倍数
uniform float uWidth;			// 纹理宽
uniform float uHeight;			// 纹理高

//转换公式
//Y’= 0.299*R’ + 0.587*G’ + 0.114*B’
//U’= -0.147*R’ - 0.289*G’ + 0.436*B’ = 0.492*(B’- Y’)
//V’= 0.615*R’ - 0.515*G’ - 0.100*B’ = 0.877*(R’- Y’)
//导出原理：采样坐标只作为确定输出位置使用，通过输出纹理计算实际采样位置，进行采样和并转换,
//然后将转换的结果填充到输出位置

void main() {
   if(vTextureCo.y<0.2500){
        //todo 填充Y部分
        //Y的采样，宽度是1:4，高度是1:1，一个点是4个Y，采样区域是4*1

        //填充点对应图片的位置
        float posX=uWidth*vTextureCo.x;
        float posY=uHeight*vTextureCo.y;
        //实际采样起始点对应图片的位置
        float rPosX=mod(posX*4.,uWidth);
        float rPosY=posY*4.+float(int(posX*4.)/int(uWidth));

        vec4 color=texture2D(uTexture,vec2(rPosX,rPosY));
        vec4 oColor=vec4(0);
        vec2 tempTextureCo=vec2(0.,rPosY/uHeight);
        for(float i=0.;i<4.;i+=1.){
            tempTextureCo.x=(rPosX+i)/float(uWidth);
            vec4 color=texture2D(uTexture,tempTextureCo);
            oColor[int(i)]=0.299*color.r + 0.587*color.g + 0.114*color.b;
        }
        gl_FragColor=oColor;
//   }else if(vTextureCo.y<0.3125){
//        //todo 填充U部分
//        //U的采样，宽度是1:8，高度是1:2，U的位置高度偏移了1/4，一个点是4个U，采样区域是宽高位8*2
//        float posX=uWidth*vTextureCo.x;
//        float posY=uHeight*vTextureCo.y;
//        //实际采样起始点对应图片的位置
//        float rPosX=mod(posX*8.,uWidth);
//        float rPosY=posY*8.-uHeight*2.+float(int(posX*8.)/int(uWidth));
//
//        vec4 color=texture2D(uTexture,vec2(rPosX,rPosY));
//        vec4 oColor=vec4(0);
//        vec2 tempTextureCo=vec2(0.,rPosY/uHeight);
//        //一共需要采样四个U出来
//        for(float i=0.;i<4.;i+=1.){
//            //采样四个点计算出U
//            vec4 fourColor;
//            fourColor+=texture2D(uTexture,vec2((rPosX+2.*i)/uWidth,rPosY/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+1.+2.*i)/uWidth,rPosY/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+1.+2.*i)/uWidth,(rPosY+1.)/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+2.*i)/uWidth,(rPosY+1.)/uHeight));
//            fourColor/=4.;
//            oColor[int(i)]= -0.147*fourColor.r - 0.289*fourColor.g + 0.436*fourColor.b;
//        }
//        gl_FragColor=oColor;
   }else if(vTextureCo.y<0.3750){
        //todo 填充V部分
        //U的采样，宽度是1:4，高度是1:2，U的位置高度偏移了5/16，一个点是4个U，采样区域是宽高位8*2
//        float posX=uWidth*vTextureCo.x;
//        float posY=uHeight*(vTextureCo.y-0.2500);
//        //实际采样起始点对应图片的位置
//        float rPosX=mod(posX*4.,uWidth);
//        float rPosY=posY*2.+float(int(posX*4.)/int(uWidth));
//
//        vec4 color=texture2D(uTexture,vec2(rPosX,rPosY));
//        vec4 oColor=vec4(0);
//        //一共需要采样四个U出来
//        for(float i=0.;i<2.;i+=1.){
//            //采样四个点计算出U
//            vec4 fourColor=vec4(0);
//            fourColor+=texture2D(uTexture,vec2((rPosX+2.*i)/uWidth,rPosY/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+1.+2.*i)/uWidth,rPosY/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+1.+2.*i)/uWidth,(rPosY+1.)/uHeight));
//            fourColor+=texture2D(uTexture,vec2((rPosX+2.*i)/uWidth,(rPosY+1.)/uHeight));
//            fourColor/=4.;
//            oColor[int(i*2.)]= -0.147*fourColor.r - 0.289*fourColor.g + 0.436*fourColor.b;
//            oColor[int(i*2.+1.)]= 0.615*fourColor.r - 0.515*fourColor.r - 0.100*fourColor.r;
//        }
//        gl_FragColor=oColor;
   }else{
        gl_FragColor=vec4(0,0,0,0);
    }
}