//
// Created by Simple on 2018/8/27.
//

#ifndef MYAPPLICATION_PARSE_H
#define MYAPPLICATION_PARSE_H



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


#endif //MYAPPLICATION_PARSE_H
