//
// Created by root on 9/8/18.
//

#ifndef PROJECT_STNETMSGXPHEADER_H
#define PROJECT_STNETMSGXPHEADER_H

#include <stdint-gcc.h>
struct __STNetMsgXpHeader {
    uint32_t    head_length;
    uint32_t    client_version;
    uint32_t    cmdid;
    uint32_t    seq;
    uint32_t	body_length;
    uint32_t	uid;
};

static uint32_t sg_client_version = 200;

#endif //PROJECT_STNETMSGXPHEADER_H
