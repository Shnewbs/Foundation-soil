package com.foundations.soil;

import com.foundations.soil.soil.*;
import com.foundations.soil.ledger.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Reproducible model/storage microbenchmark, not an in-game server TPS claim. */
public final class SoilScaleBenchmark {
    private static final SoilProfile PROFILE=new SoilProfile(.38,6.4,.55,.2,.6,.55,.06);
    private static final SoilSimulator.Rates RATES=new SoilSimulator.Rates(.00008,.03,.0012,.006,.004,.004,.06);
    private static final SoilSimulationContext CONTEXT=new SoilSimulationContext(22,.65);
    private static volatile double sink;
    public static void main(String[] args)throws Exception {
        ModelConfigFixture.loadDefaults();
        for(int i=0;i<5000;i++){var c=new SoilCell(i,0,64,PROFILE,1,0);SoilSimulator.advance(c,1,2,CONTEXT,RATES);sink+=c.profile().biology();}
        StringBuilder json=new StringBuilder("{\n  \"kind\":\"model-storage-microbenchmark-not-server-TPS\",\n  \"java\":\"")
            .append(System.getProperty("java.version")).append("\",\n  \"results\":[\n");
        int[] sizes={1000,10000,50000};
        for(int index=0;index<sizes.length;index++) {
            int size=sizes[index];System.gc();long before=used();long start=System.nanoTime();
            var world=new SoilWorldData();
            for(int i=0;i<size;i++){var cell=new SoilCell(i,0,64,PROFILE,1,1);cell.initializePhysical(SoilTexture.LOAM,"tfc:dirt/loam");world.put(new SoilCellKey(i,0),cell);}
            long initialize=System.nanoTime()-start;long resident=used()-before;
            // Warm the serializer before the measured phases, without changing the target cell count.
            SoilCell.load(world.getPacked(0).orElseThrow().save());
            start=System.nanoTime();long[] keys=world.packedKeysSnapshot();long keyNanos=System.nanoTime()-start;
            List<Long> batches=new ArrayList<>();start=System.nanoTime();
            for(int at=0;at<keys.length;at+=250){long batch=System.nanoTime();for(int i=at;i<Math.min(keys.length,at+250);i++)SoilSimulator.advance(world.getPacked(keys[i]).orElseThrow(),2,3,CONTEXT,RATES);batches.add(System.nanoTime()-batch);}
            long simulation=System.nanoTime()-start;batches.sort(Long::compare);
            start=System.nanoTime();CompoundTag root=world.save(new CompoundTag(),null);long save=System.nanoTime()-start;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();start=System.nanoTime();NbtIo.writeCompressed(root,bytes);long compress=System.nanoTime()-start;
            start=System.nanoTime();var loaded=SoilWorldData.load(NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()),NbtAccounter.unlimitedHeap()),null);long load=System.nanoTime()-start;
            if(loaded.size()!=size)throw new AssertionError("Lost cells in scale round trip");
            for(long key:keys)if(!world.getPacked(key).orElseThrow().save().equals(loaded.getPacked(key).orElseThrow().save()))throw new AssertionError("Changed cell "+key);
            String row=String.format(Locale.ROOT,"    {\"cells\":%d,\"initialize_ms\":%.3f,\"key_snapshot_ms\":%.3f,\"simulate_day_ms\":%.3f,\"batch_250_p95_ms\":%.3f,\"save_nbt_ms\":%.3f,\"compress_ms\":%.3f,\"load_compressed_ms\":%.3f,\"compressed_bytes\":%d,\"allocated_heap_delta_bytes\":%d}",
                size,initialize/1e6,keyNanos/1e6,simulation/1e6,batches.get(Math.min(batches.size()-1,(int)Math.ceil(batches.size()*.95)-1))/1e6,save/1e6,compress/1e6,load/1e6,bytes.size(),resident);
            System.out.println(row);json.append(row).append(index+1<sizes.length?",\n":"\n");
        }
        json.append("  ],\n  \"limitations\":\"No live TFC chunks, networking, server tick loop, or dedicated-server pack. Heap delta is approximate allocation, not retained heap.\"\n}\n");
        Path target=Path.of(args.length>0?args[0]:"build/reports/soil-scale-benchmark.json");Files.createDirectories(target.toAbsolutePath().getParent());Files.writeString(target,json);
    }
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
