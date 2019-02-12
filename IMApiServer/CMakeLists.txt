cmake_minimum_required(VERSION 3.12)
project(IMApiServer)

set(CMAKE_CXX_STANDARD 11)

set(ZLIB_INCLUDE "/usr/local/zlib/include")
set(ZLIB_LIB "/usr/local/zlib/lib")

set(MYSQL_INCLUDE "/usr/include/mysql")
set(MYSQL_LIB "/usr/lib64/mysql")

set(MYSQLPP_INCLUDE "/usr/local/include/mysql++")
set(MYSQLPP_LIB "/usr/local/lib")

include_directories(${ZLIB_INCLUDE})
link_directories(${ZLIB_LIB})

include_directories(${MYSQLPP_INCLUDE})
link_directories(${MYSQLPP_LIB})

include_directories(${MYSQL_INCLUDE})
link_directories(${MYSQL_LIB})

add_executable(IMApiServer IMServer.cpp CJsonObjectBase.h CJsonObjectBase.cpp BaseData.h)
target_link_libraries(IMApiServer z boost_system mysqlclient mysqlpp json_linux-gcc-4.8.5_libmt)