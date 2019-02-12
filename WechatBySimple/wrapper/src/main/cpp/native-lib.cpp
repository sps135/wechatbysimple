#include <jni.h>
#include <string>
#include <muduo/Buffer.h>
#include <vector>
#include <sys/time.h>
#include <zlib.h>

const int kHEADER_LENGTH = sizeof(int32_t);

jbyteArray charTojstring(JNIEnv *env, const char *pat, size_t len) {
    jbyteArray bytes = (env)->NewByteArray(len);
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, len, (jbyte *) pat);
    return bytes;
}

void fillEmptyBuffer(muduo::net::Buffer *buffer, const std::string &typeName, const char *message,
                     const int length) {
    int32_t nameLength = static_cast<int32_t >(typeName.size() + 1);
    buffer->appendInt32(nameLength);
    buffer->append(typeName.c_str(), nameLength);

    buffer->ensureWritableBytes(length);
    buffer->append(message, length);

    int32_t check_sum = static_cast<int32_t >(
            ::adler32(1,
                      reinterpret_cast<const Bytef *>(buffer->peek()),
                      static_cast<int>(buffer->readableBytes())));
    buffer->appendInt32(check_sum);

    int32_t len = muduo::net::sockets::hostToNetwork32(buffer->readableBytes());
    buffer->prepend(&len, sizeof len);
}

int32_t asInt32(const char *buf) {
    int32_t be32 = 0;
    ::memcpy(&be32, buf, sizeof(be32));
    return muduo::net::sockets::networkToHost32(be32);
}

jbyteArray parse(JNIEnv *env, const char *buf, const int &length) {
    int32_t expectedCheckSum = asInt32(buf + length - kHEADER_LENGTH);
    int32_t checkSum = static_cast<int32_t>(
            ::adler32(1,
                      reinterpret_cast<const Bytef *>(buf),
                      static_cast<int>(length - kHEADER_LENGTH)));
    if (expectedCheckSum == checkSum) {
        int32_t nameLength = asInt32(buf);
        if (nameLength >= 2 && nameLength <= length - 2 * kHEADER_LENGTH) {
            std::string typeName(buf + kHEADER_LENGTH, buf + kHEADER_LENGTH + nameLength - 1);

            const char *data = buf + kHEADER_LENGTH + nameLength;
            int32_t dataLen = length - nameLength - 2 * kHEADER_LENGTH;
            return charTojstring(env, data, dataLen);
        }
    }

    return NULL;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_tencent_mars_wrapper_remote_PackageUtils_packageDataNative(
        JNIEnv *env,
        jobject, /* this */
        jbyteArray array,
        jint length,
        jstring typeName) {

    char *chars = NULL;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(array, 0);
    chars = new char[length];
    memset(chars, 0, length);
    memcpy(chars, bytes, length);

    muduo::net::Buffer buffer;

    const char *typeNameStr;
    typeNameStr = env->GetStringUTFChars(typeName, JNI_FALSE);
    fillEmptyBuffer(&buffer, typeNameStr, chars, length);

    env->ReleaseStringUTFChars(typeName, typeNameStr);
    const char *start = buffer.peek();
    size_t size = buffer.readableBytes();

    delete[] chars;

    return charTojstring(env, start, size);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_tencent_mars_wrapper_remote_PackageUtils_unpackDataNative(
        JNIEnv *env,
        jobject, /* this */
        jbyteArray array,
        jint length) {

    char *chars = NULL;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(array, 0);
    chars = new char[length];
    memset(chars, 0, length);
    memcpy(chars, bytes, length);

    muduo::net::Buffer buffer;
    buffer.append(chars, length);

    delete[] chars;

    return parse(env, buffer.peek() + kHEADER_LENGTH, buffer.peekInt32());
}