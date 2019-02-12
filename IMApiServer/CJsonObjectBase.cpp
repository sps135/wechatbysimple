//
// Created by root on 10/6/18.
//
#include "CJsonObjectBase.h"

CJsonObjectBase::CJsonObjectBase() {
    // TODO Auto-generated constructor stub
    m_listName.clear();
    m_listParamType.clear();
    m_listPropertyAddr.clear();
    m_listType.clear();
}

CJsonObjectBase::~CJsonObjectBase() {
    // TODO Auto-generated destructor stub
}

std::string CJsonObjectBase::Serialize() {
    std::string out;
    Json::Value new_item = DoSerialize();
    Json::FastWriter writer;
    out = writer.write(new_item);
    return out;
}

Json::Value CJsonObjectBase::DoSerialize() {
    Json::Value new_item;
    int nSize = m_listName.size();

    for (int i = 0; i < nSize; ++i) {
        void *pAddr = m_listPropertyAddr[i];
        switch (m_listType[i]) {
            case asArray: {
                new_item[m_listName[i]] = DoArraySerialize((std::vector<CJsonObjectBase *> *) (pAddr));
                break;
            }
            case asJsonObj: {
                new_item[m_listName[i]] =
                        ((CJsonObjectBase *) pAddr)->DoSerialize();
                break;
            }
            case asBool: {
                new_item[m_listName[i]] = (*(bool *) pAddr);
                break;
            }
            case asInt16: {
                new_item[m_listName[i]] = (*(int16_t *) pAddr);
                break;
            }
            case asInt: {
                new_item[m_listName[i]] = (*(int32_t *) pAddr);
                break;
            }
            case asUInt: {
                new_item[m_listName[i]] = (*(uint32_t *) pAddr);
                break;
            }
            case asInt64: {
                //TODO  JSON 无法处理long类型
                break;
            }
            case asUInt64: {
                //TODO  JSON 无法处理long类型
                break;
            }
            case asString: {
                new_item[m_listName[i]] = (*(std::string *) pAddr);
                break;
            }
            default: {
                break;
            }
        }
    }

    return new_item;
}

Json::Value CJsonObjectBase::DoArraySerialize(std::vector<CJsonObjectBase *> *pList) {
    Json::Value arrayValue;
    for (std::vector<CJsonObjectBase *>::iterator iter = pList->begin(); iter != pList->end(); ++iter) {
        arrayValue.append((*iter)->DoSerialize());
    }
    return arrayValue;
}

bool CJsonObjectBase::DoObjArrayDeSerialize(const std::string &propertyName, void *addr, Json::Value &node) {
    if (!node.isArray()) {
        return false;
    }

    std::vector<CJsonObjectBase *> *pList = (std::vector<CJsonObjectBase *> *) addr;
    int size = node.size();
    for (int i = 0; i < size; ++i) {
        CJsonObjectBase *pNode = GenerateJsonObjForDeSerialize(propertyName);

        pNode->DoDeSerialize(node[i]);
        pList->push_back(pNode);
    }
    return true;
}

/* 反序列化的对外接口 */
bool CJsonObjectBase::DeSerialize(const char *str) {
    Json::Reader reader;
    Json::Value root;
    if (reader.parse(str, root)) {
        return DoDeSerialize(root);
    }
    return false;
}

/* 反序列化的具体实现接口 */
bool CJsonObjectBase::DoDeSerialize(Json::Value &root) {
    int nSize = m_listName.size();
    for (int i = 0; i < nSize; ++i) {
        void *pAddr = m_listPropertyAddr[i];

        switch (m_listType[i]) {
            case asArray: {
                if (root.isNull() || root[m_listName[i]].isNull()) {
                    break;
                }

                DoObjArrayDeSerialize(m_listName[i], pAddr, root[m_listName[i]]);
                break;
            }

            case asJsonObj: {
                if (!root[m_listName[i]].isNull())
                    ((CJsonObjectBase *) pAddr)->DoDeSerialize(
                            root[m_listName[i]]);
                break;
            }

            case asBool: {
                (*(bool *) pAddr) = root.get(m_listName[i], 0).asBool();
                break;
            }

            case asInt16: {
                (*(int32_t *) pAddr) = root.get(m_listName[i], 0).asInt();
                break;
            }
                /* todo asUint16类型的处理 */

            case asInt: {
                (*(int32_t *) pAddr) = root.get(m_listName[i], 0).asInt();
                break;
            }

            case asUInt: {
                (*(uint32_t *) pAddr) = root.get(m_listName[i], 0).asUInt();
                break;
            }

            case asInt64: {
                (*(int64_t *) pAddr) = root.get(m_listName[i], 0).asInt();
                break;
            }

            case asUInt64: {
                (*(u_int64_t *) pAddr) = root.get(m_listName[i], 0).asInt();
                break;
            }

            case asString: {
                (*(std::string *) pAddr) =
                        root.get(m_listName[i], "").asString();
                break;
            }
                //我暂时只支持这几种类型，需要的可以自行添加
            default: {
                break;
            }
        }
    }
    return true;
}

void CJsonObjectBase::SetProperty(std::string name, E_JsonType type, void *addr) {
    m_listName.push_back(name);
    m_listPropertyAddr.push_back(addr);
    m_listType.push_back(type);
}

void CJsonObjectBase::SetPropertyObj(std::string name, E_JsonType type, void *addr, E_JsonType listParamType) {
    m_listName.push_back(name);
    m_listPropertyAddr.push_back(addr);
    m_listType.push_back(type);
    m_listType.push_back(listParamType);
}

