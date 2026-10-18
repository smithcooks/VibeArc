param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(-not (Test-Path "$JavaHome/include/jni.h")) { throw 'Set JAVA_HOME to a JDK with JNI headers.' }
$repoRoot=Split-Path $PSScriptRoot -Parent
$cpp=Join-Path $repoRoot 'app/src/main/cpp'
$output=Join-Path $repoRoot 'build/native-host'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$gcc=(Get-Command gcc.exe -ErrorAction Stop).Source
$flags=@('-shared','-O3','-DNDEBUG','-DHAVE_CONFIG_H','-static-libgcc',"-I$cpp", "-I$cpp/lame/include", "-I$cpp/lame/libmp3lame", "-I$JavaHome/include", "-I$JavaHome/include/win32")
$sources=@('VbrTag','bitstream','encoder','fft','gain_analysis','id3tag','lame','newmdct','presets','psymodel','quantize','quantize_pvt','reservoir','set_get','tables','takehiro','util','vbrquantize','version','mpglib_interface') | ForEach-Object { "$cpp/lame/libmp3lame/$_.c" }
& $gcc @flags "$cpp/native_mp3.c" @sources '-lm' '-o' "$output/vibearcmp3.dll"
if($LASTEXITCODE -ne 0) { throw 'Host JNI compilation failed.' }
Write-Output "Native test encoder built: $output/vibearcmp3.dll"
