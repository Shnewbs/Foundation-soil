package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.client.DetailScroll;
import com.foundations.soil.network.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class BetaBoundsTest {
    @Test void packetCountsRejectRatherThanTruncateAndDesynchronize() {
        assertThrows(IllegalArgumentException.class,()->NetworkBounds.count(-1,64));
        assertThrows(IllegalArgumentException.class,()->NetworkBounds.count(65,64));
        assertEquals(64,NetworkBounds.count(64,64));
    }
    @Test void paginationIncludesPartialLastPageAndCanReturnToPreviousBatch() {
        assertEquals(128,NetworkBounds.pageOffset(100000,137,64));
        assertEquals(64,NetworkBounds.pageOffset(127,137,64));
        assertEquals(0,NetworkBounds.pageOffset(0,0,64));
    }
    @Test void replayGuardDoesNotConfuseReadHudAndMutationCooldowns() {
        RequestWindow w=new RequestWindow();UUID id=UUID.randomUUID();
        assertTrue(w.edit(id,10));assertFalse(w.edit(id,40));assertFalse(w.edit(UUID.randomUUID(),19));
        assertTrue(w.read(10));assertTrue(w.hud(10));assertFalse(w.read(11));assertFalse(w.hud(11));assertTrue(w.hud(14));assertTrue(w.edit(UUID.randomUUID(),20));
    }
    @Test void detailScrollingHandlesResizeSelectionChangesAndNonFiniteInput() {
        DetailScroll s=new DetailScroll();s.layout("A",1000,100);s.scroll(100000);assertEquals(900,s.offset());
        s.layout("A",400,100);assertEquals(300,s.offset());s.layout("B",400,100);assertEquals(0,s.offset());
        s.drag(1000);assertEquals(300,s.offset());s.set(Double.NaN);assertEquals(0,s.offset());
    }
}
