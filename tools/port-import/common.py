"""Deterministic API/resource migrations for the reviewed 26.1.2 source port.
Run only in a disposable checkout of the recorded beta baseline. The release
workflow verifies full expected file hashes after applying residual patches.
"""
from pathlib import Path
import re
root=Path.cwd()
for f in root.glob('src/**/*.java'):
    s=f.read_text()
    s=s.replace('net.minecraft.resources.ResourceLocation','net.minecraft.resources.Identifier').replace('ResourceLocation','Identifier')
    # Current TFC is late-bound, but remains mandatory. No legacy jar or fake implementation.
    s=re.sub(r'import net\.dries007\.tfc\.util\.calendar\.Calendars;', 'import com.foundations.soil.integration.TfcRuntime;', s)
    s=s.replace('net.dries007.tfc.util.calendar.Calendars.SERVER.getTotalCalendarDays()', 'com.foundations.soil.integration.TfcRuntime.days()')
    s=s.replace('Calendars.SERVER.getTotalCalendarDays()', 'TfcRuntime.days()').replace('Calendars.SERVER.getTicks()', 'TfcRuntime.ticks()')
    s=s.replace('net.dries007.tfc.util.climate.Climate.getAverageRainfall(level, soilPos)', 'com.foundations.soil.integration.TfcRuntime.averageRainfall(level, soilPos)')
    s=s.replace('net.dries007.tfc.util.climate.Climate.getAverageTemperature(level, soilPos)', 'com.foundations.soil.integration.TfcRuntime.averageTemperature(level, soilPos)')
    s=s.replace('import net.dries007.tfc.common.blocks.soil.FarmlandBlock;', 'import com.foundations.soil.integration.TfcRuntime;')
    s=s.replace('import net.dries007.tfc.util.climate.Climate;','')
    s=s.replace('Climate.getInstantTemperature(', 'TfcRuntime.temperature(').replace('FarmlandBlock.getInstantHydration(', 'TfcRuntime.hydration(')
    # dedupe imports
    seen=set(); lines=[]
    for line in s.splitlines():
        if line.startswith('import '):
            if line in seen: continue
            seen.add(line)
        lines.append(line)
    f.write_text('\n'.join(lines)+'\n')

from pathlib import Path
import re
root=Path.cwd()
j=root/'src/main/java/com/foundations/soil'
p=j/'integration/TfcSoilSnapshot.java';s=p.read_text();s=re.sub(r'import net\.dries007[^\n]+\n','',s)
s=s.replace('if (!(level.getBlockEntity(soilPos) instanceof IFarmland farmland)) {','Object farmland = level.getBlockEntity(soilPos);\n        if (!TfcRuntime.isFarmland(farmland)) {')
s=s.replace('        ChunkData data = ChunkData.get(level, soilPos);\n','').replace('data.getMinRainfallHydration(soilPos)','TfcRuntime.rainfallHydration(level, soilPos, true)').replace('data.getMaxRainfallHydration(soilPos)','TfcRuntime.rainfallHydration(level, soilPos, false)')
s=s.replace('FarmlandBlock.getInstantHydrationFromRainHydration(', 'TfcRuntime.rainHydration(')
for index,name in enumerate(['NITROGEN','PHOSPHOROUS','POTASSIUM']):s=s.replace(f'farmland.getNutrient(NutrientType.{name})',f'TfcRuntime.nutrient(farmland, {index})')
p.write_text(s)
p=j/'event/SoilGameplayEvents.java';s=p.read_text().replace('level.getBlockEntity(soil.above()) instanceof net.dries007.tfc.common.blockentities.CropBlockEntity crop\n                ? crop.getGrowth()', 'TfcRuntime.isCrop(level.getBlockEntity(soil.above()))\n                ? TfcRuntime.growth(level.getBlockEntity(soil.above()))');p.write_text(s)
p=j/'event/ManureCollectionEvents.java';s=p.read_text().replace('import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;\n','').replace('if (!(event.getTarget() instanceof TFCAnimalProperties animal)) return;\n        if (animal.getEntity().isBaby()) return;', 'if (!TfcRuntime.isAnimal(event.getTarget()) || !(event.getTarget() instanceof net.minecraft.world.entity.AgeableMob animal)) return;\n        if (animal.isBaby()) return;').replace('animal.getEntity().getPersistentData()', 'animal.getPersistentData()');p.write_text(s)
p=j/'FoundationsSoil.java';s=p.read_text().replace('"0.1.0-beta"','"0.1.0-beta-port.1"').replace('            AmendmentInputBridge.bootstrap();','            com.foundations.soil.integration.TfcRuntime.bootstrap();\n            AmendmentInputBridge.bootstrap();');p.write_text(s)
p=j/'mixin/TfcCropYieldMixin.java';s=p.read_text();s=re.sub(r'import net\.dries007[^\n]+\n','',s);s=s.replace('@Mixin(value = CropHelpers.class, remap = false)','@Mixin(targets = "net.dries007.tfc.common.blocks.crop.CropHelpers", remap = false)');s=s.replace('CropBlockEntity receiver', '@org.spongepowered.asm.mixin.injection.Coerce Object receiver').replace('CropBlockEntity crop','@org.spongepowered.asm.mixin.injection.Coerce Object crop').replace('receiver.getYield()', 'com.foundations.soil.integration.TfcRuntime.yield(receiver)');p.write_text(s)
p=j/'mixin/TfcPickingHarvestMixin.java';s=p.read_text();s=re.sub(r'import net\.dries007[^\n]+\n','',s);s=s.replace('@Mixin(value = {PickableCropBlock.class, PickableClimbingCropBlock.class}, remap = false)', '@Mixin(targets = {"net.dries007.tfc.common.blocks.crop.PickableCropBlock", "net.dries007.tfc.common.blocks.crop.PickableClimbingCropBlock"}, remap = false)').replace('CropBlockEntity crop,', '@org.spongepowered.asm.mixin.injection.Coerce Object cropObject,');s=s.replace('original.call(crop, value);', 'original.call(cropObject, value);\n        var crop = (net.minecraft.world.level.block.entity.BlockEntity) cropObject;');p.write_text(s)
# NBT API conversion: restrict changes to files that use NBT, avoiding JSON definitions.
for p in root.glob('src/**/*.java'):
    s=p.read_text()
    if 'net.minecraft.nbt.' not in s:continue
    # Scalar getter call sites; leave JSON get calls and strings elsewhere alone.
    for name,default in [('Int','0'),('Long','0L'),('Double','0.0'),('Float','0f'),('Boolean','false'),('String','""')]:
        s=re.sub(r'(?P<recv>\b(?:root|tag|nbt|physical|data|wrapper|player|copy))\.get'+name+r'\((?P<key>"[^"]*"|[A-Z_]+)\)', lambda m:f'{m["recv"]}.get{name}({m["key"]}).orElse({default})',s)
    s=re.sub(r'\b(\w+)\.contains\(("[^"]*"|[A-Z_]+), (Tag\.TAG_\w+)\)',r'com.foundations.soil.storage.NbtAccess.contains(\1, \2, \3)',s)
    s=re.sub(r'\b(\w+)\.getList\(("[^"]*"), Tag\.TAG_COMPOUND\)',r'com.foundations.soil.storage.SavedTagChecks.compounds(\1, \2, false)',s)
    s=re.sub(r'\b(\w+)\.getList\(("[^"]*"), Tag\.TAG_STRING\)',r'com.foundations.soil.storage.NbtAccess.strings(\1, \2)',s)
    s=re.sub(r'(\b\w+\.getCompound\([^()]*\))(?!\.orElse)',r'\1.orElseThrow(() -> new IllegalStateException("Expected saved compound"))',s)
    p.write_text(s)

from pathlib import Path
import re
root=Path.cwd(); j=root/'src/main/java/com/foundations/soil'
for p in root.glob('src/**/*.java'):
 s=p.read_text().replace('.serverLevel()', '.level()').replace('.location()', '.identifier()').replace('.noCollission()', '.noCollision()')
 s=s.replace('BlockEvent.BreakEvent', 'net.neoforged.neoforge.event.level.block.BreakBlockEvent')
 s=s.replace('net.neoforged.neoforge.network.PacketDistributor.sendToServer(', 'net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(')
 s=re.sub(r'(?<!\.)\bPacketDistributor.sendToServer\(', 'net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(',s)
 s=s.replace('.hasPermission(2)', '.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)')
 s=s.replace('GuiGraphics', 'GuiGraphicsExtractor')
 s=s.replace('.drawString(', '.text(').replace('.drawCenteredString(', '.centeredText(').replace('.renderItem(', '.item(').replace('.renderFakeItem(', '.fakeItem(').replace('.hLine(', '.horizontalLine(').replace('.vLine(', '.verticalLine(')
 s=s.replace('.renderTooltip(', '.setTooltipForNextFrame(')
 s=s.replace('.pushPose()', '.pushMatrix()').replace('.popPose()', '.popMatrix()')
 # These client transforms were two-dimensional; remove only their unused depth arguments.
 s=re.sub(r'(\.pose\(\)\.(?:translate|scale)\([^;\n]+),\s*(?:0|0\.0[Ff]?|1|1\.0[Ff]?)\);',r'\1);',s)
 if p.name in ['FieldGuideScreen.java','LaboratoryScreen.java','LedgerEditorScreen.java']:
  s=s.replace('public void render(', 'public void extractRenderState(').replace('super.render(', 'super.extractRenderState(').replace('renderBackground(', 'extractBackground(')
  s=s.replace('protected void renderBlurredBackground(float partialTick)', 'protected void extractBlurredBackground(GuiGraphicsExtractor graphics)')
  # method bodies still use former primitive variable names, sourced from native events
  s=re.sub(r'boolean keyPressed\(int key, int scan, int modifiers\) \{', 'boolean keyPressed(net.minecraft.client.input.KeyEvent event) {\n        int key = event.key(), scan = event.scancode(), modifiers = event.modifiers();',s)
  s=s.replace('super.keyPressed(key, scan, modifiers)', 'super.keyPressed(event)')
  for name in ['mouseClicked','mouseReleased','mouseDragged']:
   pattern=r'boolean '+name+r'\(double (\w+), double (\w+), int button'+(r', double dx, double dy' if name=='mouseDragged' else '')+r'\) \{'
   def repl(m):
    args='net.minecraft.client.input.MouseButtonEvent event'+(', boolean doubleClick' if name=='mouseClicked' else ', double dx, double dy' if name=='mouseDragged' else '')
    return 'boolean '+name+'('+args+') {\n        double '+m[1]+' = event.x(), '+m[2]+' = event.y(); int button = event.button();'
   s=re.sub(pattern,repl,s)
   s=re.sub(r'super\.'+name+r'\(\w+, \w+, button'+(r', dx, dy' if name=='mouseDragged' else '')+r'\)', 'super.'+name+'(event'+(', doubleClick' if name=='mouseClicked' else ', dx, dy' if name=='mouseDragged' else '')+')',s)
 if '.displayClientMessage(' in s:
  s=s.replace('.displayClientMessage(','.sendOverlayMessage(').replace(', true);',');')
 s=s.replace('InteractionResult.sidedSuccess(level.isClientSide())','InteractionResult.SUCCESS')
 if p.name=='FieldGuideItem.java':
  s=s.replace('InteractionResultHolder<ItemStack>', 'InteractionResult').replace('import net.minecraft.world.InteractionResultHolder;', 'import net.minecraft.world.InteractionResult;').replace('InteractionResultHolder.sidedSuccess(stack, level.isClientSide())','InteractionResult.SUCCESS')
  s=s.replace('if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)', 'if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && com.foundations.soil.network.SoilNetwork.permitLedgerOpen(serverPlayer))')
 if p.name=='SavedTagChecks.java':
  s=s.replace('(!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)', 'list.stream().anyMatch(entry -> entry.getId() != Tag.TAG_COMPOUND)')
 if p.name=='FieldLedgerEntry.java':s=s.replace('events.getString(i)', 'events.getString(i).orElseThrow()')
 if p.name in ['SoilWorldData.java','FieldLedgerData.java']:
  s=s.replace('extends SavedData','extends com.foundations.soil.storage.SoilSavedData')
 p.write_text(s)

from pathlib import Path
import json
r=Path.cwd()/'src/main/resources'
def save(p,o): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,indent=2)+'\n')
def ingredient(o):
    if isinstance(o,dict) and set(o)=={'item'}:return o['item']
    if isinstance(o,dict) and set(o)=={'tag'}:return '#'+o['tag']
    if isinstance(o,list):return [ingredient(x) for x in o]
    return o
for p in (r/'data/foundations_soil/recipe').rglob('*.json'):
    data=json.loads(p.read_text());typ=data['type']
    if 'key' in data:data['key']={k:ingredient(v) for k,v in data['key'].items()}
    if 'ingredients' in data:data['ingredients']=[ingredient(i) for i in data['ingredients']]
    if 'ingredient' in data:data['ingredient']=ingredient(data['ingredient'])
    if 'input_item' in data:
        old=data['input_item']
        if 'item' in old or 'tag' in old:
            data['input_item']={'ingredient':old.get('item') or '#'+old['tag'],'count':old.get('count',1)}
    # Native SizedFluidIngredient wraps its ingredient; exact TFC recipe loading remains a runtime gate.
    if 'input_fluid' in data:
        old=data['input_fluid']
        if 'fluid' in old or 'tag' in old:
            data['input_fluid']={'ingredient': old.get('fluid') or '#'+old['tag'],'amount':old['amount']}
    save(p,data)
for p in (r/'assets/foundations_soil/models/item').glob('*.json'):
    obj=json.loads(p.read_text());name=p.stem
    if obj.get('loader')=='neoforge:fluid_container':
        model={'type':'neoforge:fluid_container','fluid':obj['fluid'], 'textures':{
            'base':'minecraft:item/bucket','fluid':'neoforge:item/mask/bucket_fluid','cover':'neoforge:item/mask/bucket_fluid_cover'},'force_opaque_fluid':True}
        save(p,{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/bucket'}})
    else:model={'type':'minecraft:model','model':f'foundations_soil:item/{name}'}
    save(r/'assets/foundations_soil/items'/f'{name}.json',{'model':model})
save(r/'pack.mcmeta', {'pack': {'description':'Foundations Soil native 26.1.2 port resources','min_format':84,'max_format':84}})
# Minecraft has separate major versions for resource/data packs in 26.1.2. NeoForge supplies
# mod pack compatibility from the loader; a resource-only range would incorrectly reject data.
# Leave pack metadata to native NeoForge (same as upstream mod convention).
(r/'pack.mcmeta').unlink()
