#include "codec.h"

#include <muduo/base/Logging.h>
#include <muduo/net/Endian.h>
#include <muduo/net/protorpc/google-inl.h>

#include <google/protobuf/descriptor.h>

#include <zlib.h>

using namespace muduo;
using namespace muduo::net;
using namespace Codec;

void ProtobufCodec::fillEmptyBuffer(Buffer *buf, const google::protobuf::Message &message) {
    assert(buf->readableBytes() == 0);

    const std::string &typeName = message.GetTypeName();
    int32_t nameLen = static_cast<int32_t>(typeName.size() + 1);
    buf->appendInt32(nameLen);
    buf->append(typeName.c_str(), nameLen);

            GOOGLE_DCHECK(message.IsInitialized()) << InitializationErrorMessage("serialize", message);

    int byte_size = message.ByteSize();
    buf->ensureWritableBytes(byte_size);

    uint8_t *start = reinterpret_cast<uint8_t *>(buf->beginWrite());
    uint8_t *end = message.SerializeWithCachedSizesToArray(start);
    if (end - start != byte_size) {
        ByteSizeConsistencyError(byte_size, message.ByteSize(), static_cast<int>(end - start));
    }
    buf->hasWritten(byte_size);

    int32_t checkSum = static_cast<int32_t>(
            ::adler32(1,
                      reinterpret_cast<const Bytef *>(buf->peek()),
                      static_cast<int>(buf->readableBytes())));
    buf->appendInt32(checkSum);
    assert(buf->readableBytes() == sizeof nameLen + nameLen + byte_size + sizeof checkSum);
    int32_t len = sockets::hostToNetwork32(static_cast<int32_t>(buf->readableBytes()));
    buf->prepend(&len, sizeof len);
}

namespace {
    const string kNoErrorStr = "NoError";
    const string kInvalidLengthStr = "InvalidLength";
    const string kCheckSumErrorStr = "CheckSumError";
    const string kInvalidNameLenStr = "InvalidNameLen";
    const string kUnknownMessageTypeStr = "UnknownMessageType";
    const string kParseErrorStr = "ParseError";
    const string kUnknownErrorStr = "UnknownError";
}

const string &ProtobufCodec::errorCodeToString(ErrorCode errorCode) {
    switch (errorCode) {
        case kNoError:
            return kNoErrorStr;
        case kInvalidLength:
            return kInvalidLengthStr;
        case kCheckSumError:
            return kCheckSumErrorStr;
        case kInvalidNameLen:
            return kInvalidNameLenStr;
        case kUnknownMessageType:
            return kUnknownMessageTypeStr;
        case kParseError:
            return kParseErrorStr;
        default:
            return kUnknownErrorStr;
    }
}

void ProtobufCodec::defaultErrorCallback(const muduo::net::TcpConnectionPtr &conn,
                                         muduo::net::Buffer *buf,
                                         muduo::Timestamp,
                                         ErrorCode errorCode) {
    LOG_ERROR << "ProtobufCodec::defaultErrorCallback - " << errorCodeToString(errorCode);
    if (conn && conn->connected()) {
        buf->retrieveAll();
        conn->shutdown();
    }
}

int32_t asInt32(const char *buf) {
    int32_t be32 = 0;
    ::memcpy(&be32, buf, sizeof(be32));
    return sockets::networkToHost32(be32);
}

void ProtobufCodec::onMessage(const TcpConnectionPtr &conn,
                              Buffer *buff,
                              Timestamp receiveTime) {
    LOG_INFO << "onMessage";
    if (buff->readableBytes() < sizeof(__STNetMsgXpHeader)) {
        buff->retrieveAll();
        return;
    } else if (buff->readableBytes() == sizeof(__STNetMsgXpHeader)) {
        LOG_INFO << "Heart beat received";
        __STNetMsgXpHeader msgXpHeaderReceived = {0};
        memcpy(&msgXpHeaderReceived, buff->peek(), sizeof(__STNetMsgXpHeader));
        LOG_INFO << "uid: " << htonl(msgXpHeaderReceived.uid);
        heartBeatCallback_(conn, htonl(msgXpHeaderReceived.uid));
        conn->send(buff);
    } else {
        LOG_INFO << "start handle message...";
        __STNetMsgXpHeader msgXpHeaderReceived = {0};
        memcpy(&msgXpHeaderReceived, buff->peek(), sizeof(__STNetMsgXpHeader));

        buff->retrieve(sizeof(__STNetMsgXpHeader));

        LOG_INFO << "start parse message...";
        while (buff->readableBytes() >= (kMinMessageLen + kHeaderLen)) {
            const int32_t len = buff->peekInt32();
            LOG_INFO << "message length: " << len;
            if (len > kMaxMessageLen || len < kMinMessageLen) {
                LOG_INFO << "invalid length!";
                errorCallback_(conn, buff, receiveTime, kInvalidLength);
                break;
            } else if (buff->readableBytes() >= (len + kHeaderLen)) {
                ErrorCode errorCode = kNoError;
                MessagePtr message = parse(buff->peek() + kHeaderLen, len, &errorCode);
                if (errorCode == kNoError && message) {
                    LOG_INFO << "parse no error!";
                    buff->retrieve(ntohl(msgXpHeaderReceived.body_length));
                    messageCallback_(conn, message, msgXpHeaderReceived, receiveTime);
                } else {
                    errorCallback_(conn, buff, receiveTime, errorCode);
                    break;
                }
            } else {
                break;
            }
        }
    }
}

google::protobuf::Message *ProtobufCodec::createMessage(const std::string &typeName) {
    google::protobuf::Message *message = NULL;
    const google::protobuf::Descriptor *descriptor =
            google::protobuf::DescriptorPool::generated_pool()->FindMessageTypeByName(typeName);
    if (descriptor) {
        const google::protobuf::Message *prototype =
                google::protobuf::MessageFactory::generated_factory()->GetPrototype(descriptor);
        if (prototype) {
            message = prototype->New();
        }
    }
    return message;
}

MessagePtr ProtobufCodec::parse(const char *buf, int len, ErrorCode *error) {
    MessagePtr message;

    // check sum
    int32_t expectedCheckSum = asInt32(buf + len - kHeaderLen);
    int32_t checkSum = static_cast<int32_t>(
            ::adler32(1,
                      reinterpret_cast<const Bytef *>(buf),
                      static_cast<int>(len - kHeaderLen)));
    if (checkSum == expectedCheckSum) {
        // get message type name
        int32_t nameLen = asInt32(buf);
        if (nameLen >= 2 && nameLen <= len - 2 * kHeaderLen) {
            std::string typeName(buf + kHeaderLen, buf + kHeaderLen + nameLen - 1);
            LOG_INFO << "type name: " << typeName;
            message.reset(createMessage(typeName));
            if (message) {
                // parse from buffer
                const char *data = buf + kHeaderLen + nameLen;
                int32_t dataLen = len - nameLen - 2 * kHeaderLen;
                if (message->ParseFromArray(data, dataLen)) {
                    *error = kNoError;
                } else {
                    *error = kParseError;
                }
            } else {
                *error = kUnknownMessageType;
            }
        } else {
            *error = kInvalidNameLen;
        }
    } else {
        *error = kCheckSumError;
    }

    return message;
}
