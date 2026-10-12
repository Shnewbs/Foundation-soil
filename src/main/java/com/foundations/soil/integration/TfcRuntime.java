package com.foundations.soil.integration;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Mandatory, fail-closed TFC boundary for the 26.1.2 port. Resolves real TFC methods once.
 * This is not a vanilla fallback or a replacement TFC implementation. Runtime compatibility
 * must be established with a genuine TFC-Foundations artifact before gameplay release.
 */
public final class TfcRuntime {
    private TfcRuntime() {}
    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName("net.dries007.tfc." + name, false, TfcRuntime.class.getClassLoader());
    }
    private static IllegalStateException incompatible(Throwable cause) {
        if (cause instanceof VirtualMachineError fatal) throw fatal;
        return new IllegalStateException("Foundations Soil requires a compatible native 26.1.2 TFC-Foundations build. "
            + "The required TFC API is missing or incompatible; no vanilla soil/calendar fallback is used.", cause);
    }
    private static volatile Access cached;
    private static Access access() {
        Access result = cached;
        if (result == null) synchronized (TfcRuntime.class) {
            result = cached;
            if (result == null) cached = result = Access.create();
        }
        return result;
    }
    private static final class Access {
        final Class<?> farmland, crop, animal;
        final MethodHandle ticks, days, temperature, averageTemperature, rainfall, hydration, rainHydration;
        final MethodHandle chunk, minRain, maxRain, nutrient, growth, yield;
        final Object nitrogen, phosphorus, potassium;
        Access() throws ReflectiveOperationException {
            var lookup = MethodHandles.publicLookup();
            Object calendar = type("util.calendar.Calendars").getField("SERVER").get(null);
            Class<?> calendarApi = type("util.calendar.ICalendar");
            ticks = lookup.unreflect(calendarApi.getMethod("getTicks")).bindTo(calendar).asType(MethodType.methodType(long.class));
            days = lookup.unreflect(calendarApi.getMethod("getTotalCalendarDays")).bindTo(calendar).asType(MethodType.methodType(long.class));
            Class<?> climate = type("util.climate.Climate");
            temperature = stat(climate, "getInstantTemperature", double.class, Level.class, BlockPos.class);
            averageTemperature = stat(climate, "getAverageTemperature", double.class, Level.class, BlockPos.class);
            rainfall = stat(climate, "getAverageRainfall", double.class, Level.class, BlockPos.class);
            Class<?> block = type("common.blocks.soil.FarmlandBlock");
            hydration = stat(block, "getInstantHydration", int.class, Level.class, BlockPos.class);
            rainHydration = stat(block, "getInstantHydrationFromRainHydration", int.class, Level.class, BlockPos.class, int.class);
            Class<?> data = type("world.chunkdata.ChunkData");
            chunk = stat(data, "get", Object.class, LevelReader.class, BlockPos.class);
            minRain = lookup.unreflect(data.getMethod("getMinRainfallHydration", BlockPos.class))
                .asType(MethodType.methodType(double.class, Object.class, BlockPos.class));
            maxRain = lookup.unreflect(data.getMethod("getMaxRainfallHydration", BlockPos.class))
                .asType(MethodType.methodType(double.class, Object.class, BlockPos.class));
            farmland = type("common.blockentities.IFarmland");
            Class<?> nutrientType = type("common.blockentities.FarmlandBlockEntity$NutrientType");
            nutrient = lookup.unreflect(farmland.getMethod("getNutrient", nutrientType))
                .asType(MethodType.methodType(double.class, Object.class, Object.class));
            nitrogen = nutrientType.getField("NITROGEN").get(null);
            phosphorus = nutrientType.getField("PHOSPHOROUS").get(null);
            potassium = nutrientType.getField("POTASSIUM").get(null);
            crop = type("common.blockentities.CropBlockEntity");
            growth = lookup.unreflect(crop.getMethod("getGrowth")).asType(MethodType.methodType(float.class, Object.class));
            yield = lookup.unreflect(crop.getMethod("getYield")).asType(MethodType.methodType(float.class, Object.class));
            animal = type("common.entities.livestock.TFCAnimalProperties");
        }
        static Access create() {
            try { return new Access(); }
            catch (ReflectiveOperationException | RuntimeException failure) { throw incompatible(failure); }
        }
        private static MethodHandle stat(Class<?> owner, String name, Class<?> result, Class<?>... args) throws ReflectiveOperationException {
            return MethodHandles.publicLookup().unreflect(owner.getMethod(name, args)).asType(MethodType.methodType(result, args));
        }
    }
    public static void bootstrap() { if (access() == null) throw incompatible(null); }

    /** Resolves every TFC symbol Soil relies on and returns a concise runtime diagnostic. */
    public static String diagnostic() {
        Access value = access();
        if (value == null) throw incompatible(null);
        return "calendar+climate+farmland+chunkdata+crop+livestock API READY";
    }
    public static long ticks() { try { return (long) access().ticks.invokeExact(); } catch (Throwable e) { throw incompatible(e); } }
    public static long days() { try { return (long) access().days.invokeExact(); } catch (Throwable e) { throw incompatible(e); } }
    public static double temperature(Level l, BlockPos p) { try { return (double) access().temperature.invokeExact(l,p); } catch (Throwable e) { throw incompatible(e); } }
    public static double averageTemperature(Level l, BlockPos p) { try { return (double) access().averageTemperature.invokeExact(l,p); } catch (Throwable e) { throw incompatible(e); } }
    public static double averageRainfall(Level l, BlockPos p) { try { return (double) access().rainfall.invokeExact(l,p); } catch (Throwable e) { throw incompatible(e); } }
    public static int hydration(Level l, BlockPos p) { try { return (int) access().hydration.invokeExact(l,p); } catch (Throwable e) { throw incompatible(e); } }
    public static int rainHydration(Level l, BlockPos p, int rain) { try { return (int) access().rainHydration.invokeExact(l,p,rain); } catch (Throwable e) { throw incompatible(e); } }
    public static double rainfallHydration(LevelReader l, BlockPos p, boolean minimum) {
        try { Object c = access().chunk.invokeExact(l,p); return minimum ? (double) access().minRain.invokeExact(c,p) : (double) access().maxRain.invokeExact(c,p); }
        catch (Throwable e) { throw incompatible(e); }
    }
    public static boolean isFarmland(Object value) { return access().farmland.isInstance(value); }
    public static boolean isCrop(Object value) { return access().crop.isInstance(value); }
    public static boolean isAnimal(Object value) { return access().animal.isInstance(value); }
    public static float growth(Object crop) { try { return (float) access().growth.invokeExact(crop); } catch (Throwable e) { throw incompatible(e); } }
    public static float yield(Object crop) { try { return (float) access().yield.invokeExact(crop); } catch (Throwable e) { throw incompatible(e); } }
    public static double nutrient(Object farmland, int index) {
        Object kind = switch (index) { case 0 -> access().nitrogen; case 1 -> access().phosphorus; case 2 -> access().potassium; default -> throw new IllegalArgumentException("Nutrient index"); };
        try { return (double) access().nutrient.invokeExact(farmland,kind); } catch (Throwable e) { throw incompatible(e); }
    }
}
