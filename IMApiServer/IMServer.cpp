//
// Created by root on 10/4/18.
//
#include <websocketpp/config/asio_no_tls.hpp>
#include <websocketpp/server.hpp>
#include <streambuf>
#include <string>
#include <mysql++.h>
#include <ssqls.h>
#include <json/json.h>
#include "CJsonObjectBase.h"
#include "BaseData.h"

typedef websocketpp::server<websocketpp::config::asio> Server;
typedef Server::message_ptr message_ptr;

using websocketpp::lib::placeholders::_1;
using websocketpp::lib::placeholders::_2;
using websocketpp::lib::bind;

namespace {
    const int LOGIN_SERVICE = 1001;
    const int USER_LIST_SERVICE = 1002;

    const int UNKNOWN_SERVICE = 20001;
    const int UNKNOWN_DATA_FORMAT = 20002;
    const int UNKNOWN_PARAM_FORMAT = 20003;
    const int USERNAME_PASSWORD_ERROR = 20004;
}

class IMAPIServer {
public:
    IMAPIServer() {
        mysql_conn_.set_option(new mysqlpp::SetCharsetNameOption("utf8"));
        mysql_conn_.connect("IM", "127.0.0.1", "root", "Aa123456+");
        server_.init_asio();
        server_.set_reuse_addr(true);
        server_.set_http_handler(bind(&IMAPIServer::on_http, this, ::_1));
    }

    void run() {
        std::stringstream log_ss;
        log_ss << "Running IM API server on port " << 8080;

        server_.get_alog().write(websocketpp::log::alevel::app, log_ss.str());
        server_.listen(8080);
        server_.start_accept();

        try {
            server_.run();
        } catch (websocketpp::exception const &e) {
            std::cout << e.what() << std::endl;
        }

    }

private:
    void on_http(websocketpp::connection_hdl hdl) {
        Server::connection_ptr conn = server_.get_con_from_hdl(hdl);

        websocketpp::http::parser::request rt = conn->get_request();
        const std::string &strBody = rt.get_body();

        log("received data: ", strBody);

        BaseRequestBodyEntity request;

        if (request.DeSerialize(strBody.c_str())) {
            log("serviceCode: ", std::to_string(request.getServiceCode()));
            log("request data: ", request.getData());
            switch (request.getServiceCode()) {
                case LOGIN_SERVICE:
                    handleLogin(conn, request.getData());
                    break;
                case USER_LIST_SERVICE:
                    handleUserList(conn);
                    break;
                default:
                    response(UNKNOWN_SERVICE, false, "unknown service", "", conn);
            }
        } else {
            response(UNKNOWN_DATA_FORMAT, false, "unknown data format", "", conn);
        }

    }

    void handleLogin(const Server::connection_ptr &conn,
                     const std::string &requestData);

    void handleUserList(const Server::connection_ptr &conn);

    void response(const std::string data,
                  const Server::connection_ptr &conn) {
        response(0, true, "", data, conn);
    }

    void response(const int errorCode,
                  const bool success,
                  const std::string message,
                  const std::string data,
                  const Server::connection_ptr &conn) {

        BaseResponseBodyEntity response;
        response.setData(data);
        response.setErrorCode(errorCode);
        response.setSuccess(success);
        response.setMessage(message);

        const std::string responseData = response.Serialize();
        log("response data: ", responseData);
        conn->set_body(responseData);
        conn->set_status(websocketpp::http::status_code::ok);
    }

    void log(const std::string &tag, const std::string &value) {
        std::stringstream log_data;
        log_data << tag << value;
        server_.get_alog().write(websocketpp::log::alevel::app, log_data.str());
    }

    Server server_;
    mysqlpp::Connection mysql_conn_;
};

void IMAPIServer::handleLogin(const Server::connection_ptr &conn, const std::string &requestData) {
    LoginRequest request;
    log("handleLogin: ", requestData);
    if (request.DeSerialize(requestData.c_str())) {
        log("received username: ", request.getUsername());
        log("received password: ", request.getPassword());

        try {
            mysqlpp::Query query = mysql_conn_.query("select * from user where username = %0q and password = %1q");
            query.parse();
            mysqlpp::StoreQueryResult result = query.store(mysqlpp::SQLTypeAdapter(request.getUsername()),
                                                           mysqlpp::SQLTypeAdapter(request.getPassword()));
            if (result.size() == 1) {
                log("hit username: ", request.getUsername());
                LoginResponse loginResponse;
                loginResponse.setUid(std::stoi(std::string(result[0]["id"].c_str())));
                loginResponse.setNickname(result[0]["nickname"].c_str());
                loginResponse.setPortrait(result[0]["portrait"].c_str());
                response(loginResponse.Serialize(), conn);
            } else {
                log("can not find user or password error", "");
                response(USERNAME_PASSWORD_ERROR, false, "username or password error", "", conn);
            }

        } catch (const mysqlpp::Exception &exception) {
            log("mysql exception: ", exception.what());
        }

    } else {
        response(UNKNOWN_PARAM_FORMAT, false, "unknown param format", "", conn);
    }
}

void IMAPIServer::handleUserList(const Server::connection_ptr &conn) {
    log("handleUserList", "");
    Json::Value userList;

    try {
        mysqlpp::Query query = mysql_conn_.query("select * from user");
        query.parse();
        mysqlpp::StoreQueryResult result = query.store();

        Json::Value array;
        for (int i = 0; i < result.size(); i++) {
            Json::Value item;
            item["uid"] = std::stoi(std::string(result[i]["id"].c_str()));
            item["nickname"] = std::string(result[i]["nickname"].c_str());
            item["portrait"] = std::string(result[i]["portrait"].c_str());
            item["pinyin"] = std::string(result[i]["pinyin"].c_str());

            array.append(item);
        }

        response(array.toStyledString(), conn);
    } catch (const mysqlpp::Exception &exception) {
        log("mysql exception: ", exception.what());
    }

}


int main() {

    IMAPIServer server;
    server.run();
    return 0;
}