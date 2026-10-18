# Native LAME source

Unmodified encoder library/include files from the official LAME 3.100 release:
https://downloads.sourceforge.net/project/lame/lame/3.100/lame-3.100.tar.gz

Archive SHA-256:
`ddfe36cab873794038ae2c1210557ad34857a4b6bdc515785d1da9e175b1da1e`

The upstream `COPYING` license is preserved. VibeArc's CMake configuration,
config.h and JNI bridge live one directory above this vendor directory. No
command-line frontend or MP3 decoder is compiled. Android's MediaCodec handles
source decoding. LAME is linked into a replaceable shared JNI library, not into
the app's DEX or a static executable. Source for rebuilding that library is here.
