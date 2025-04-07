#include <jni.h>
#include <string>
#include <vector>  // This is essential
// Declare your Rust functions
extern "C" JNIEXPORT jbyteArray JNICALL {
// Audio generation functions

jfloatArray Java_x_com_oz_RustBridge_generateSineWave(JNIEnv*, jclass, jfloat, jfloat, jint);
jfloatArray Java_x_com_oz_RustBridge_applyAM(JNIEnv*, jclass, jfloatArray, jfloatArray, jfloat);
jfloatArray Java_x_com_oz_RustBridge_applyFM(JNIEnv*, jclass, jfloat, jfloatArray, jfloat, jint);
jfloatArray Java_x_com_oz_RustBridge_applyADSR(JNIEnv*, jclass, jfloatArray, jint, jfloat, jfloat, jfloat, jfloat);
jfloat Java_x_com_oz_RustBridge_noteToFrequency(JNIEnv*, jclass, jint);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_x_com_oz_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string status = "Native code loaded successfully!\n";

    // Test Rust bridge connectivity
    jfloat testFreq = Java_x_com_oz_RustBridge_noteToFrequency(env, nullptr, 69); // A4 note
    status += "Test frequency for A4: " + std::to_string(testFreq) + " Hz";

    return env->NewStringUTF(status.c_str());
}

// Helper function to convert between jfloatArray and std::vector
std::vector<float> jfloatArrayToVector(JNIEnv* env, jfloatArray array) {
    jsize length = env->GetArrayLength(array);
    std::vector<float> result(length);
    env->GetFloatArrayRegion(array, 0, length, &result[0]);
    return result;
}

// Helper function to convert between std::vector and jfloatArray
jfloatArray vectorToJfloatArray(JNIEnv* env, const std::vector<float>& vec) {
    jfloatArray result = env->NewFloatArray(vec.size());
    env->SetFloatArrayRegion(result, 0, vec.size(), &vec[0]);
    return result;
}