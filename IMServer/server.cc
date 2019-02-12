#include "codec.h"
#include "dispatcher.h"

#include "query.pb.h"

#include <muduo/base/Logging.h>
#include <muduo/net/EventLoop.h>
#include <muduo/net/TcpServer.h>

#include <mysql++.h>

#include <boost/bind.hpp>
#include <sstream>

using namespace muduo;
using namespace muduo::net;
using namespace Codec;

typedef boost::shared_ptr<muduo::LoginRequest> LoginPtr;

typedef boost::shared_ptr<muduo::IMMessage> IMMessagePtr;

typedef boost::shared_ptr<muduo::HistoryMessage> HistoryPtr;

namespace {
    const int LOG_OUT_CMDID = 20000;
    const int PUSH_MESSAGE_CMDID = 20001;
}

class QueryServer : boost::noncopyable {
public:
    QueryServer(EventLoop *loop,

                const InetAddress &listenAddr)
            : server_(loop, listenAddr, "QueryServer"),
              dispatcher_(boost::bind(&QueryServer::onUnknownMessage, this, _1, _2, _3, _4)),
              codec_(boost::bind(&ProtobufDispatcher::onProtobufMessage, &dispatcher_, _1, _2, _3, _4),
                     boost::bind(&QueryServer::heartBeatReceived, this, _1, _2)) {
        mysql_conn_.set_option(new mysqlpp::SetCharsetNameOption("utf8"));
        mysql_conn_.connect("IM", "127.0.0.1", "root", "Aa123456+");

        dispatcher_.registerMessageCallback<muduo::LoginRequest>(
                boost::bind(&QueryServer::onLoginRequest, this, _1, _2, _3, _4));
        dispatcher_.registerMessageCallback<muduo::IMMessage>(
                boost::bind(&QueryServer::onIMMessage, this, _1, _2, _3, _4));
        dispatcher_.registerMessageCallback<muduo::HistoryMessage>(
                boost::bind(&QueryServer::onHistoryMessage, this, _1, _2, _3, _4));
        server_.setConnectionCallback(
                boost::bind(&QueryServer::onConnection, this, _1));
        server_.setMessageCallback(
                boost::bind(&ProtobufCodec::onMessage, &codec_, _1, _2, _3));
    }

    void start() {
        server_.start();
    }

    void setThreadNum(int numThreads) {
        server_.setThreadNum(numThreads);
    }

private:
    void onConnection(const TcpConnectionPtr &conn) {
        LOG_INFO << conn->localAddress().toIpPort() << " -> "
                 << conn->peerAddress().toIpPort() << " is "
                 << (conn->connected() ? "UP" : "DOWN");
        MutexLockGuard lock(mutex_);
        if (!conn->connected()) {
            std::map<int64_t, TcpConnectionPtr>::iterator iter = connections_.begin();
            while (iter != connections_.end()) {
                if (iter->second == conn) {
                    LOG_INFO << "Log out...";
                    connections_.erase(iter);
                    break;
                }
                iter++;
            }
        }
    }

    void onUnknownMessage(const TcpConnectionPtr &conn,
                          const MessagePtr &message,
                          __STNetMsgXpHeader &header,
                          Timestamp) {
        LOG_INFO << "onUnknownMessage: " << message->GetTypeName();
        conn->shutdown();
    }

    void onLoginRequest(const muduo::net::TcpConnectionPtr &conn,
                        const LoginPtr &message,
                        __STNetMsgXpHeader &header,
                        muduo::Timestamp timestamp);

    void onIMMessage(const muduo::net::TcpConnectionPtr &conn,
                     const IMMessagePtr &message,
                     __STNetMsgXpHeader &header,
                     muduo::Timestamp timestamp);

    void onHistoryMessage(const muduo::net::TcpConnectionPtr &conn,
                          const HistoryPtr &message,
                          __STNetMsgXpHeader &header,
                          muduo::Timestamp timestamp);

    void heartBeatReceived(const muduo::net::TcpConnectionPtr &conn, const int userId);

    void saveIMMessage(const IMMessagePtr &message);

    void logOut(const int uid) {
        MutexLockGuard lock(mutex_);
        if (connections_[uid]) {
            LOG_INFO << "logout user...";
            TcpConnectionPtr &conn = connections_[uid];
            muduo::Logout out;
            codec_.push(conn, out, LOG_OUT_CMDID);
            connections_.erase(uid);
            LOG_INFO << "logout user success";
        }
    }

    void logIn(const int uid, const muduo::net::TcpConnectionPtr &conn) {
        MutexLockGuard lock(mutex_);
        connections_[uid] = conn;
        LOG_INFO << "login user success";
    }

    void notifyNoError(const muduo::net::TcpConnectionPtr &conn, __STNetMsgXpHeader &header) {
        muduo::Response response;
        response.set_error_code(muduo::Response::NO_ERROR);
        codec_.send(conn, response, header);
    }

    template<class Type>
    void stringToType(const std::string &strTmp, Type &TypeTmp) {
        std::stringstream stream;
        stream << strTmp;
        stream >> TypeTmp;
        stream.clear();
        stream.str("");
    }

    TcpServer server_;
    ProtobufDispatcher dispatcher_;
    Codec::ProtobufCodec codec_;
    MutexLock mutex_;
    mysqlpp::Connection mysql_conn_;

    typedef std::map<int64_t, TcpConnectionPtr> ConnectionMap;
    ConnectionMap connections_;
};

void QueryServer::onLoginRequest(const muduo::net::TcpConnectionPtr &conn, const LoginPtr &message,
                                 __STNetMsgXpHeader &header, muduo::Timestamp timestamp) {
    LOG_INFO << "handle login user: " << message->user_id();
    logOut(message->user_id());
    logIn(message->user_id(), conn);
    notifyNoError(conn, header);
}

void QueryServer::onIMMessage(const muduo::net::TcpConnectionPtr &conn, const IMMessagePtr &message,
                              __STNetMsgXpHeader &header, muduo::Timestamp timestamp) {
    LOG_INFO << "message handling...";
    if (message->conversationtype() == muduo::IMMessage_ConversationType_PRIVATE) {
        LOG_INFO << "private voice message handling...";
        if (connections_[message->targetid()]) {
            LOG_INFO << "user online...";
            codec_.push(connections_[message->targetid()], (*message.get()), PUSH_MESSAGE_CMDID);
        } else {
            LOG_INFO << "user offline...";
            try {
                saveIMMessage(message);
            } catch (const mysqlpp::Exception &exception) {
                LOG_INFO << exception.what();
                muduo::Response response;
                response.set_error_code(muduo::Response::DATABASE_EEROR);
                response.set_error_msg(exception.what());
                codec_.send(conn, response, header);
                return;
            }
            LOG_INFO << "save message success...";
        }
        notifyNoError(conn, header);
    } else {

    }
}

void QueryServer::saveIMMessage(const IMMessagePtr &message) {
    mysqlpp::Query query = mysql_conn_.query(
            "insert into message (id, fromUserId, targetId, createAt, extras, conversationType, messageType) values(%0q, %1q, %2q, %3q, %4q, %5q, %6q)");
    query.parse();
    LOG_INFO << "parse sql ..";
    query.execute(mysqlpp::SQLTypeAdapter(message->id()),
                  mysqlpp::SQLTypeAdapter(message->fromuserid()),
                  mysqlpp::SQLTypeAdapter(message->targetid()),
                  mysqlpp::SQLTypeAdapter(message->createat()),
                  mysqlpp::SQLTypeAdapter(message->extras()),
                  mysqlpp::SQLTypeAdapter(message->conversationtype()),
                  mysqlpp::SQLTypeAdapter(message->messagetype())
    );
}

void QueryServer::onHistoryMessage(const muduo::net::TcpConnectionPtr &conn, const HistoryPtr &message,
                                   __STNetMsgXpHeader &header, muduo::Timestamp timestamp) {
    const int toUserId = message->id();
    LOG_INFO << "onHistoryMessage: " << toUserId;

    try {
        mysqlpp::Query query = mysql_conn_.query("select * from message where targetId = %0q and isSend = %1q order by createAt");
        query.parse();
        mysqlpp::StoreQueryResult result = query.store(mysqlpp::SQLTypeAdapter(toUserId),
                mysqlpp::SQLTypeAdapter(0));

        if (result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                IMMessage imMessage;

                std::string id = std::string(result[i]["id"].c_str());

                std::string fromUserIdStr = std::string(result[i]["fromUserId"].c_str());
                int fromUserId;
                stringToType(fromUserIdStr, fromUserId);

                std::string targetIdStr = std::string(result[i]["targetId"].c_str());
                int targetId;
                stringToType(targetIdStr, targetId);

                std::string createAtStr = std::string(result[i]["createAt"].c_str());
                u_int64_t createAt;
                stringToType(createAtStr, createAt);

                std::string extras = std::string(result[i]["extras"].c_str());

                std::string messageTypeStr = std::string(result[i]["messageType"].c_str());
                int messageType;
                stringToType(messageTypeStr, messageType);

                std::string conversationTypeStr = std::string(result[i]["conversationType"].c_str());
                int conversationType;
                stringToType(conversationTypeStr, conversationType);

                LOG_INFO << "History Message: "
                         << "id: " << id << " "
                         << "fromUserId: " << fromUserId << " "
                         << "createAt: " << createAt << " "
                         << "targetId: " << targetId << " "
                         << "extras: " << extras;

                imMessage.set_id(id);
                imMessage.set_fromuserid(fromUserId);
                imMessage.set_createat(createAt);
                imMessage.set_targetid(targetId);
                imMessage.set_extras(extras);
                if (messageType == 1) {
                    imMessage.set_messagetype(IMMessage_MessageType_TEXT_MESSGAGE);
                } else if (messageType == 2) {
                    imMessage.set_messagetype(IMMessage_MessageType_IMAGE_MESSGAGE);
                } else if (messageType == 3) {
                    imMessage.set_messagetype(IMMessage_MessageType_VOICE_MESSGAGE);
                }
                if (conversationType == 1) {
                    imMessage.set_conversationtype(IMMessage_ConversationType_PRIVATE);
                } else if (conversationType == 2) {
                    imMessage.set_conversationtype(IMMessage_ConversationType_GROUP);
                }

                codec_.push(connections_[toUserId], imMessage, PUSH_MESSAGE_CMDID);

                mysqlpp::Query insert = mysql_conn_.query("update message set isSend = 1 where id = %0q");
                insert.parse();
                insert.execute(mysqlpp::SQLTypeAdapter(id));
                LOG_INFO << "modify isSend status ..";
            }

        } else {
            LOG_INFO << "can not find unsend history message: " << toUserId;
        }

    } catch (const mysqlpp::Exception &exception) {
        LOG_INFO << "mysql exception: " << exception.what();
    }
    notifyNoError(conn, header);
}

void QueryServer::heartBeatReceived(const muduo::net::TcpConnectionPtr &conn, const int userId) {
    LOG_INFO << "handle login user: " << userId;
    if (connections_[userId]) {
        if (connections_[userId].get() == conn.get()) {
            LOG_INFO << "already login !: " << userId;
            return;
        }
        logOut(userId);
    }
    logIn(userId, conn);
}

int main(int argc, char *argv[]) {
    LOG_INFO << "pid = " << getpid();
    EventLoop loop;
    InetAddress serverAddr(8081);
    QueryServer server(&loop, serverAddr);
    server.setThreadNum(3);
    server.start();
    loop.loop();
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
