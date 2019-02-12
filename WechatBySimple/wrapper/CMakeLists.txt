cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(distribution_DIR ${CMAKE_SOURCE_DIR}/../protobuf)
set(SRC_DIR ./src/main/cpp)

SET(CMAKE_C_COMPILER g++)
add_compile_options(-std=c++11)

include_directories(${distribution_DIR}/include)
include_directories(${SRC_DIR})

add_library(buffer ./src/main/cpp/muduo/Buffer.cc)

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ./src/main/cpp/native-lib.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib
        z
        buffer
        )