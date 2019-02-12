#include <muduo/net/Buffer.h>
#include <muduo/net/TcpConnection.h>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

#include <google/protobuf/message.h>
#include "STNetMsgXpHeader.h"


typedef boost::shared_ptr<google::protobuf::Message> MessagePtr;
namespace Codec {
    class ProtobufCodec : boost::noncopyable {
    public:

        enum ErrorCode {
            kNoError = 2002,
            kInvalidLength,
            kCheckSumError,
            kInvalidNameLen,
            kUnknownMessageType,
            kParseError,
        };

        typedef boost::function<void(const muduo::net::TcpConnectionPtr &,
                                     const MessagePtr &,
                                     __STNetMsgXpHeader &,
                                     muduo::Timestamp)> ProtobufMessageCallback;

        typedef boost::function<void(const muduo::net::TcpConnectionPtr &,
                                     const int)> HeartBeatCallback;

        typedef boost::function<void(const muduo::net::TcpConnectionPtr &,
                                     muduo::net::Buffer *,
                                     muduo::Timestamp,
                                     ErrorCode)> ErrorCallback;

        ProtobufCodec(const ProtobufMessageCallback &messageCb,
                      const HeartBeatCallback &heartBeatCallback)
                : messageCallback_(messageCb),
                  heartBeatCallback_(heartBeatCallback),
                  errorCallback_(defaultErrorCallback) {
        }

        void onMessage(const muduo::net::TcpConnectionPtr &conn,
                       muduo::net::Buffer *buf,
                       muduo::Timestamp receiveTime);

        void send(const muduo::net::TcpConnectionPtr &conn,
                  const google::protobuf::Message &message,
                  __STNetMsgXpHeader &header) {

            muduo::net::Buffer temp;
            fillEmptyBuffer(&temp, message);

            muduo::net::Buffer buf;
            header.head_length = htonl(sizeof(__STNetMsgXpHeader));
            header.body_length = htonl(temp.readableBytes());
            buf.append(&header, sizeof(__STNetMsgXpHeader));
            buf.append(temp.peek(), temp.readableBytes());

            conn->send(&buf);
        }

        void push(const muduo::net::TcpConnectionPtr &conn,
                  const google::protobuf::Message &message,
                  const int cmdid) {

            muduo::net::Buffer temp;
            fillEmptyBuffer(&temp, message);

            __STNetMsgXpHeader header;
            muduo::net::Buffer buf;

            const uint32_t clientVersion = 200;
            const uint32_t taskId = 0;

            header.head_length = htonl(sizeof(__STNetMsgXpHeader));
            header.body_length = htonl(temp.readableBytes());
            header.client_version = htonl(clientVersion);
            header.seq = htonl(taskId);
            header.cmdid = htonl(cmdid);
            buf.append(&header, sizeof(__STNetMsgXpHeader));
            buf.append(temp.peek(), temp.readableBytes());

            conn->send(&buf);
        }

        static const muduo::string &errorCodeToString(ErrorCode errorCode);

        static void fillEmptyBuffer(muduo::net::Buffer *buf, const google::protobuf::Message &message);

        static google::protobuf::Message *createMessage(const std::string &type_name);

        static MessagePtr parse(const char *buf, int len, ErrorCode *errorCode);

    private:
        static void defaultErrorCallback(const muduo::net::TcpConnectionPtr &,
                                         muduo::net::Buffer *,
                                         muduo::Timestamp,
                                         ErrorCode);

        ProtobufMessageCallback messageCallback_;
        HeartBeatCallback heartBeatCallback_;
        ErrorCallback errorCallback_;

        const static int kHeaderLen = sizeof(int32_t);
        const static int kMinMessageLen = 2 * kHeaderLen + 2; // nameLen + typeName + checkSum
        const static int kMaxMessageLen = 64 * 1024 * 1024;
    };
}


