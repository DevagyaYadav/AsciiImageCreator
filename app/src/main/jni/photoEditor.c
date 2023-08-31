#include <jni.h>


JNIEXPORT void JNICALL
Java_com_example_photoeditor_MainActivity_greyscale(JNIEnv *env, jclass clazz, jintArray pixels,
                                                    jint width, jint height) {
    char* colors=(char*)pixels;


    int pixelcount= height*width*4;
    int i=0;

    while(i<pixelcount){
        unsigned char average=(colors[i]+colors[i+1]+colors[i+2])/3;
        colors[i]=average;
        colors[i+1]=average;
        colors[i+2]=average;
        i+=4;}

}