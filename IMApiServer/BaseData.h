//
// Created by root on 10/6/18.
//
#include "CJsonObjectBase.h"

class BaseRequestBodyEntity : public CJsonObjectBase {
public:
    BaseRequestBodyEntity() {
        SetPropertys();
    }

    virtual ~BaseRequestBodyEntity() {

    }

    const std::string &getData() const {
        return data;
    }

    int getServiceCode() const {
        return serviceCode;
    }

private:
    virtual void SetPropertys() {
        SetProperty("data", asString, &data);
        SetProperty("serviceCode", asInt, &serviceCode);
    }

    std::string data;
    int serviceCode;
};

class BaseResponseBodyEntity : public CJsonObjectBase {

public:
    BaseResponseBodyEntity() {
        SetPropertys();
    }

    virtual ~BaseResponseBodyEntity() {

    }

    void setSuccess(bool success) {
        BaseResponseBodyEntity::success = success;
    }

    void setErrorCode(int errorCode) {
        BaseResponseBodyEntity::errorCode = errorCode;
    }

    void setMessage(const std::string &message) {
        BaseResponseBodyEntity::message = message;
    }

    void setData(const std::string &data) {
        BaseResponseBodyEntity::data = data;
    }

private:
    virtual void SetPropertys() {
        SetProperty("success", asBool, &success);
        SetProperty("errorCode", asInt, &errorCode);
        SetProperty("data", asString, &data);
        SetProperty("message", asString, &message);
    }

    bool success;
    int errorCode;
    std::string message;
    std::string data;
};

class LoginRequest : public CJsonObjectBase {
public:
    LoginRequest() {
        SetPropertys();
    }

    virtual ~LoginRequest() {

    }

    const std::string &getUsername() const {
        return username;
    }

    const std::string &getPassword() const {
        return password;
    }


private:
    virtual void SetPropertys() {
        SetProperty("username", asString, &username);
        SetProperty("password", asString, &password);
    }

    std::string username;
    std::string password;
};

class LoginResponse : public CJsonObjectBase {
public:
    LoginResponse() {
        SetPropertys();
    }

    virtual ~LoginResponse() {

    }

    void setUid(int uid) {
        LoginResponse::uid = uid;
    }

    void setNickname(const std::string &nickname) {
        LoginResponse::nickname = nickname;
    }

    void setPortrait(const std::string &portrait) {
        LoginResponse::portrait = portrait;
    }

private:
    virtual void SetPropertys() {
        SetProperty("uid", asInt, &uid);
        SetProperty("nickname", asString, &nickname);
        SetProperty("portrait", asString, &portrait);
    }

    int uid;
    std::string nickname;
    std::string portrait;
};

