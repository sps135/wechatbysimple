cmake_minimum_required(VERSION 2.6)

set(PROTOBUF_INCLUDE "/usr/local/protobuf/include")
include_directories(${PROTOBUF_INCLUDE})

set(MUDUO_INCLUDE "/root/Downloads/build/release-install/include")
set(MUDUO_LIB "/root/Downloads/build/release-install/lib")

set(MYSQLPP_INCLUDE "/usr/local/include/mysql++")
set(MYSQLPP_LIB "/usr/local/lib")

set(MYSQL_INCLUDE "/usr/include/mysql")
set(MYSQL_LIB "/usr/lib64/mysql")

include_directories(${MUDUO_INCLUDE})
link_directories(${MUDUO_LIB})

set(ZLIB_INCLUDE "/usr/local/zlib/include")
set(ZLIB_LIB "/usr/local/zlib/lib")

include_directories(${ZLIB_INCLUDE})
link_directories(${ZLIB_LIB})

include_directories(${MYSQLPP_INCLUDE})
link_directories(${MYSQLPP_LIB})

include_directories(${MYSQL_INCLUDE})
link_directories(${MYSQL_LIB})

add_custom_command(OUTPUT query.pb.cc query.pb.h
        COMMAND protoc
        ARGS --cpp_out . ${CMAKE_CURRENT_SOURCE_DIR}/query.proto -I${CMAKE_CURRENT_SOURCE_DIR}
        DEPENDS query.proto
        VERBATIM )

set_source_files_properties(query.pb.cc PROPERTIES COMPILE_FLAGS "-Wno-conversion -Wno-shadow")
include_directories(${PROJECT_BINARY_DIR})

add_library(query_proto query.pb.cc)
target_link_libraries(query_proto protobuf pthread)

add_library(protobuf_codec codec.cc)
target_link_libraries(protobuf_codec protobuf muduo_base muduo_net z)

add_executable(protobuf_server server.cc)
set_target_properties(protobuf_server PROPERTIES COMPILE_FLAGS "-Wno-error=shadow")
target_link_libraries(protobuf_server protobuf_codec query_proto mysqlclient mysqlpp)
