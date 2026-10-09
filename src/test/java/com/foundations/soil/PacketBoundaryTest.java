package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.network.*;
import io.netty.buffer.Unpooled;
import java.lang.reflect.RecordComponent;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PacketBoundaryTest {
    private static RegistryFriendlyByteBuf buffer() { return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY); }
    private static String text(int size) { return "界".repeat(size); }
    @SuppressWarnings("unchecked")
    private static <T> T maximumRecord(Class<T> type) throws Exception {
        RecordComponent[] components=type.getRecordComponents();Object[] values=new Object[components.length];Class<?>[] types=new Class<?>[components.length];
        Map<String,Integer> limits=Map.ofEntries(Map.entry("dimension",128),Map.entry("note",256),Map.entry("tfcSoilBlock",128),Map.entry("lastHarvestFamily",64),Map.entry("lastAmendmentId",64),Map.entry("lastAmendmentUnit",32),Map.entry("id",64),Map.entry("name",80),Map.entry("description",320),Map.entry("form",32),Map.entry("doseUnit",32));
        for(int i=0;i<components.length;i++) {var c=components[i];types[i]=c.getType();
            values[i]=types[i]==String.class?text(limits.get(c.getName())):types[i]==int.class?Integer.MAX_VALUE:types[i]==long.class?Long.MAX_VALUE:types[i]==double.class?1.0:types[i]==boolean.class?true:Collections.nCopies(8,text(160));
        }
        return type.getDeclaredConstructor(types).newInstance(values);
    }
    @Test void maximumMultibyteGuideRoundTripsBelowOneMegabyte() throws Exception {
        var fields=Collections.nCopies(128,new FieldGuideDataPayload.FieldView(Integer.MAX_VALUE,text(64),text(256),true,Integer.MAX_VALUE));
        var cells=Collections.nCopies(64,maximumRecord(FieldGuideDataPayload.CellView.class));
        var amendments=Collections.nCopies(64,maximumRecord(FieldGuideDataPayload.AmendmentView.class));
        var payload=new FieldGuideDataPayload(fields,cells,amendments,new FieldGuideDataPayload.WarningView(.2,.6,.5,.7,5,8,.2),text(32),64,99,200,true,0,0,200);
        var b=buffer();try {FieldGuideDataPayload.STREAM_CODEC.encode(b,payload);assertTrue(b.readableBytes()<1024*1024,"Guide payload too large: "+b.readableBytes());assertEquals(payload,FieldGuideDataPayload.STREAM_CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
    }
    @Test void malformedCountsFailBeforeAllocationOrReadingBodies() {
        for(int value:new int[]{-1,Integer.MAX_VALUE,129}) {var b=buffer();try{b.writeVarInt(value);assertThrows(IllegalArgumentException.class,()->FieldGuideDataPayload.STREAM_CODEC.decode(b));}finally{b.release();}}
    }
    @Test void laboratoryReportHasBoundedRoundTripAndRejectsOverlongLines() {
        var p=new LaboratoryResultPayload(UUID.randomUUID(),Collections.nCopies(96,text(256)),Collections.nCopies(96,text(256)));
        var b=buffer();try{LaboratoryResultPayload.STREAM_CODEC.encode(b,p);assertTrue(b.readableBytes()<160000);assertEquals(p,LaboratoryResultPayload.STREAM_CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        assertThrows(IllegalArgumentException.class,()->new LaboratoryResultPayload(UUID.randomUUID(),List.of(text(257)),List.of()));
    }
}
