package com.foundations.soil.integration;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.api.SoilService;
import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.api.script.SoilScriptGateway;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Best-effort bridge to Foundations Framework without making the Framework JAR a compile-time requirement.
 * This keeps standalone source builds simple while exposing stable services when Framework is present.
 */
public final class FrameworkSoilBridge {
    private static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "soil");
    private static volatile Status status = Status.NOT_STARTED;
    private static volatile String detail = "Not started";
    private static final Map<String, Boolean> SERVICES = new LinkedHashMap<>();
    private static boolean attempted;

    private FrameworkSoilBridge() {}

    public static synchronized void bootstrap() {
        if (attempted && status == Status.ACTIVE) return;
        attempted = true;
        SERVICES.clear();

        if (!ModList.get().isLoaded("foundations_framework")) {
            status = Status.FRAMEWORK_MISSING;
            detail = "Foundations Framework mod is not loaded";
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.foundations.framework.api.FoundationsFrameworkApi");
            Object api = apiClass.getMethod("get").invoke(null);
            Object ownerId = foundationId(FoundationsSoil.MOD_ID, "soil");
            registerModule(api, ownerId);

            Object services = invokeNoArgs(api, "services");
            int registered = 0;
            registered += registerService(services, ownerId, "soil_service", SoilService.class, FoundationsSoilApi.service()) ? 1 : 0;
            registered += registerService(services, ownerId, "script_gateway_service", SoilScriptGateway.class, FoundationsSoilApi.scripting()) ? 1 : 0;
            registered += registerService(services, ownerId, "soil_events", com.foundations.soil.api.event.SoilEventBus.class, SoilEvents.bus()) ? 1 : 0;

            if (registered == SERVICES.size() && registered > 0) {
                status = Status.ACTIVE;
                detail = "Module + " + registered + " service(s) registered";
            } else {
                status = Status.PARTIAL;
                detail = "Framework API found; " + registered + "/" + SERVICES.size() + " service(s) registered";
            }
            FoundationsSoil.LOGGER.info("[Foundations Soil] Framework bridge {} — {}", status, detail);
        } catch (Throwable error) {
            status = Status.FAILED;
            detail = error.getClass().getSimpleName() + ": " + safeMessage(error);
            FoundationsSoil.LOGGER.warn("[Foundations Soil] Framework bridge could not complete; Soil remains operational: {}", detail);
        }
    }

    public static Status status() { return status; }
    public static String detail() { return detail; }
    public static synchronized Map<String, Boolean> services() { return Map.copyOf(SERVICES); }

    private static void registerModule(Object api, Object ownerFoundationId) throws Exception {
        Object modules = invokeNoArgs(api, "modules");
        if (booleanInvokeIfPresent(modules, "isRegistered", MODULE_ID)) return;

        Class<?> moduleClass = Class.forName("com.foundations.framework.api.module.FoundationModule");
        Object builder = invokeStaticCompatible(moduleClass, "builder", new Object[]{MODULE_ID}, new Object[]{ownerFoundationId});
        invokeCompatible(builder, "displayName", new Object[]{"Foundations Soil"});
        invokeCompatible(builder, "version", new Object[]{FoundationsSoil.VERSION});
        Object apiVersion = invokeNoArgs(api, "apiVersion");
        invokeCompatible(builder, "apiVersion", new Object[]{apiVersion}, new Object[]{String.valueOf(apiVersion)});
        Object module = invokeNoArgs(builder, "build");
        invokeCompatible(modules, "register", new Object[]{module});
    }

    private static boolean registerService(Object registry, Object ownerFoundationId, String path,
                                           Class<?> serviceType, Object service) {
        SERVICES.put(path, false);
        try {
            ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, path);
            Object serviceFoundationId = foundationId(FoundationsSoil.MOD_ID, path);
            Object key = createServiceKey(resourceId, serviceFoundationId, serviceType);
            if (key == null) throw new IllegalStateException("No compatible ServiceKey factory was found");

            if (booleanInvokeIfPresent(registry, "isRegistered", key)) {
                SERVICES.put(path, true);
                return true;
            }

            Object[][] candidates = new Object[][]{
                {MODULE_ID, key, service},
                {ownerFoundationId, key, service},
                {key, MODULE_ID, service},
                {key, ownerFoundationId, service},
                {key, service},
                {MODULE_ID, resourceId, serviceType, service},
                {ownerFoundationId, serviceFoundationId, serviceType, service},
                {resourceId, serviceType, service},
                {serviceFoundationId, serviceType, service}
            };
            invokeCompatible(registry, "register", candidates);
            SERVICES.put(path, true);
            return true;
        } catch (Throwable error) {
            FoundationsSoil.LOGGER.warn("[Foundations Soil] Framework service '{}' registration skipped: {}", path, safeMessage(error));
            return false;
        }
    }

    private static Object createServiceKey(ResourceLocation resourceId, Object foundationId, Class<?> serviceType) throws Exception {
        Class<?> keyClass = Class.forName("com.foundations.framework.api.service.ServiceKey");
        Object[][] candidates = new Object[][]{
            {resourceId, serviceType},
            {foundationId, serviceType},
            {resourceId},
            {foundationId},
            {FoundationsSoil.MOD_ID, resourceId.getPath(), serviceType},
            {FoundationsSoil.MOD_ID + ":" + resourceId.getPath(), serviceType}
        };
        for (String name : new String[]{"of", "create", "key", "typed"}) {
            Object value = tryInvokeStaticCompatible(keyClass, name, candidates);
            if (value != null) return value;
        }
        for (Constructor<?> constructor : keyClass.getDeclaredConstructors()) {
            for (Object[] args : candidates) {
                if (compatible(constructor.getParameterTypes(), args)) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
        }
        return null;
    }

    private static Object foundationId(String namespace, String path) throws Exception {
        Class<?> type = Class.forName("com.foundations.framework.api.FoundationId");
        Object value = tryInvokeStaticCompatible(type, "of", new Object[][]{{namespace, path}, {namespace + ":" + path}});
        if (value == null) throw new IllegalStateException("Could not construct Framework FoundationId");
        return value;
    }

    private static Object invokeNoArgs(Object target, String name) throws Exception {
        Method method = target.getClass().getMethod(name);
        return method.invoke(target);
    }

    private static boolean booleanInvokeIfPresent(Object target, String name, Object arg) {
        try {
            Object result = invokeCompatible(target, name, new Object[]{arg});
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeCompatible(Object target, String name, Object[]... candidates) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || Modifier.isStatic(method.getModifiers())) continue;
            for (Object[] args : candidates) {
                if (!compatible(method.getParameterTypes(), args)) continue;
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + " compatible overload not found");
    }

    private static Object invokeStaticCompatible(Class<?> type, String name, Object[]... candidates) throws Exception {
        Object value = tryInvokeStaticCompatible(type, name, candidates);
        if (value == null) throw new NoSuchMethodException(type.getName() + "." + name + " compatible static overload not found");
        return value;
    }

    private static Object tryInvokeStaticCompatible(Class<?> type, String name, Object[][] candidates) throws Exception {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || !Modifier.isStatic(method.getModifiers())) continue;
            for (Object[] args : candidates) {
                if (!compatible(method.getParameterTypes(), args)) continue;
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        }
        return null;
    }

    private static boolean compatible(Class<?>[] parameters, Object[] args) {
        if (parameters.length != args.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            if (args[i] == null) {
                if (parameters[i].isPrimitive()) return false;
                continue;
            }
            Class<?> expected = wrap(parameters[i]);
            if (!expected.isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static String safeMessage(Throwable error) {
        Throwable actual = error;
        while (actual.getCause() != null) actual = actual.getCause();
        String message = actual.getMessage();
        return message == null || message.isBlank() ? actual.getClass().getSimpleName() : message;
    }

    public enum Status {
        NOT_STARTED,
        FRAMEWORK_MISSING,
        ACTIVE,
        PARTIAL,
        FAILED
    }
}
