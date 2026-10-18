package com.vibearc.app;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Opt-in synthetic workload; desktop results are not phone performance claims. */
public final class NativeMp3Benchmark {
    public static void main(String[] args) throws Exception {
        int seconds = args.length == 0 ? 300 : Integer.parseInt(args[0]);
        if (seconds < 1 || seconds > 1800) throw new IllegalArgumentException("1–1800 seconds");
        File file = new File("build/native-mp3-benchmark.mp3");
        file.getParentFile().mkdirs();
        ByteBuffer pcm = ByteBuffer.allocateDirect(48000 * 4).order(ByteOrder.LITTLE_ENDIAN);
        long random = 12345;
        for (int frame=0;frame<48000;frame++) {
            random=(random*1664525+1013904223)&0xffffffffL;
            double noise=((random>>>16)/65535.0-0.5)*3000;
            short left=(short)(Math.sin(frame*2*Math.PI*440/48000)*10000+noise);
            short right=(short)(Math.sin(frame*2*Math.PI*659/48000)*9000-noise);
            pcm.putShort(left);pcm.putShort(right);
        }
        pcm.flip();
        long started=System.nanoTime();
        try(FileOutputStream stream=new FileOutputStream(file);
            Mp3Encoder encoder=new Mp3Encoder(48000,2,stream)) {
            for(int second=0;second<seconds;second++) { pcm.rewind();encoder.write(pcm,false); }
            encoder.finish();
        }
        double elapsed=(System.nanoTime()-started)/1e9;
        System.out.printf("native_audio_seconds=%d wall_seconds=%.3f realtime_factor=%.4f bytes=%d%n",
            seconds,elapsed,elapsed/seconds,file.length());
    }
}
