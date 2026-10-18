cmake_minimum_required(VERSION 3.22.1)
project(vibearcmp3 C)

set(LAME_SOURCES
    VbrTag bitstream encoder fft gain_analysis id3tag lame newmdct presets
    psymodel quantize quantize_pvt reservoir set_get tables takehiro util
    vbrquantize version mpglib_interface)
list(TRANSFORM LAME_SOURCES PREPEND "lame/libmp3lame/")
list(TRANSFORM LAME_SOURCES APPEND ".c")
add_library(vibearcmp3 SHARED native_mp3.c ${LAME_SOURCES})
target_include_directories(vibearcmp3 PRIVATE . lame/include lame/libmp3lame)
target_compile_definitions(vibearcmp3 PRIVATE HAVE_CONFIG_H NDEBUG)
# Native optimized encoder; do not trade away LAME's psychoacoustic quality.
target_compile_options(vibearcmp3 PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(vibearcmp3 PRIVATE -Wl,--gc-sections -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
target_link_libraries(vibearcmp3 m)
