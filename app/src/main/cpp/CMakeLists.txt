cmake_minimum_required(VERSION 3.22.1)

project("whisper-jni")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/upstream
)

if (ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(-O3 -flto -march=armv8-a+simd)
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    add_compile_options(-O3 -flto -march=armv7-a -mfpu=neon -mfloat-abi=softfp)
else()
    add_compile_options(-O3 -flto)
endif()

add_library(
        whisper-jni
        SHARED
        upstream/ggml.c
        upstream/ggml-alloc.c
        upstream/ggml-backend.c
        upstream/ggml-quants.c
        upstream/whisper.cpp
        whisper_jni.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        whisper-jni
        ${log-lib}
        android
)

