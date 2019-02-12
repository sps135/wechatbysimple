//
// Created by root on 10/6/18.
//

#ifndef IMAPISERVER_CJSONOBJECTBASE_H
#define IMAPISERVER_CJSONOBJECTBASE_H
#ifndef JSONHANDLE_CJSONOBJECTBASE_H_
#define JSONHANDLE_CJSONOBJECTBASE_H_

#include <string>
#include <vector>
#include <list>
#include "json/json.h"

/* 各种json对象的标识 */
typedef enum {
    asBool = 1,
    asInt16,
    asInt,
    asUInt,
    asString,
    asInt64,
    asUInt64,
    asJsonObj,                 //复杂对象
    asArray,                 //数组
    jsonNum                  //标识数
} E_JsonType;

class CJsonObjectBase {
public:
    CJsonObjectBase();

    virtual ~CJsonObjectBase();

public:
    std::string Serialize();            /* 序列化为json结构 */
    bool DeSerialize(const char *str);  /* 反序列化为内存变量 */

    Json::Value DoArraySerialize(std::vector<CJsonObjectBase *> *pList);     /* 对数组结构进行 */
    void SetProperty(std::string name, E_JsonType type, void *addr);

    void SetPropertyObj(std::string name, E_JsonType type, void *addr, E_JsonType listParamType = asInt);

private:
    Json::Value DoSerialize();

    bool DoDeSerialize(Json::Value &root);

    bool DoObjArrayDeSerialize(const std::string &propertyName, void *addr, Json::Value &node);

    virtual CJsonObjectBase *GenerateJsonObjForDeSerialize(const std::string &propertyName) {
        return NULL;
    };

    virtual void SetPropertys() = 0;

private:
    std::vector<std::string> m_listName;
    std::vector<void *> m_listPropertyAddr;
    std::vector<E_JsonType> m_listType;
    std::vector<E_JsonType> m_listParamType;
};

#endif /* JSONHANDLE_CJSONOBJECTBASE_H_ */

#endif //IMAPISERVER_CJSONOBJECTBASE_H
