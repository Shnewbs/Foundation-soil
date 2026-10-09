package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
@org.junit.jupiter.api.Tag("tfc-runtime")
class TfcBridgeTargetTest {
    @Test void pinnedTfcContainsExactlyOneExpectedYieldHook() throws Exception {
        try(InputStream input=getClass().getResourceAsStream("/net/dries007/tfc/common/blocks/crop/CropHelpers.class")) {
            assertNotNull(input);ClassNode node=new ClassNode();new ClassReader(input).accept(node,0);
            var target=node.methods.stream().filter(m->m.name.equals("growthTickStep")&&m.desc.equals(
                "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;JJLnet/dries007/tfc/common/blockentities/CropBlockEntity;)Z")).findFirst().orElseThrow();
            int matches=0;for(var instruction:target.instructions)if(instruction instanceof MethodInsnNode m&&m.owner.equals("net/dries007/tfc/common/blockentities/CropBlockEntity")&&m.name.equals("setYield")&&m.desc.equals("(F)V"))matches++;
            assertEquals(1,matches,"TFC changed the bridge target: review the native integration before release");
        }
    }
    @Test void bothPickingClassesContainTwoConfirmedHarvestResetPaths() throws Exception {
        for (String type : new String[]{"PickableCropBlock", "PickableClimbingCropBlock"}) {
            try(InputStream input=getClass().getResourceAsStream("/net/dries007/tfc/common/blocks/crop/"+type+".class")) {
                assertNotNull(input); ClassNode node=new ClassNode(); new ClassReader(input).accept(node,0);
                var target=node.methods.stream().filter(m->m.name.equals("useItemOn")).findFirst().orElseThrow();
                int matches=0;for(var instruction:target.instructions)
                    if(instruction instanceof MethodInsnNode m&&m.owner.equals("net/dries007/tfc/common/blockentities/CropBlockEntity")&&m.name.equals("setYield")&&m.desc.equals("(F)V"))matches++;
                assertEquals(2,matches,type+" changed its picking contract; review the mixin");
            }
        }
    }
}
