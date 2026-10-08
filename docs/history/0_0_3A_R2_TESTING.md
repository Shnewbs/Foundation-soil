# Foundations Soil 0.0.3a.R2 — Runtime Test Checklist

The Windows NeoForge/TFC build is authoritative.

## 1. Native build

```bat
build_mod.bat
```

Expected:

```text
compileJava PASS
compileTestJava PASS
test PASS
build PASS
build\libs\foundations_soil-0.0.3a.R2.jar
```

## 2. Startup / self-test

Launch with Foundations Framework + TFC and run:

```mcfunction
/fsoil selftest
/fsoil amendment list
```

Expected:

- self-test result remains `PASS`;
- `Physical Amendment Fluids` reports 5 registered fluid families;
- all five liquid definitions show a `foundations_soil:*` physical fluid binding;
- no missing-registry, missing-model, or recipe decode errors in the log.

## 3. TFC barrel production

Test all five recipes in a TFC barrel:

```text
Compost Tea        = 1 TFC Compost + 1000 mB Water, sealed 12000 ticks
Manure Slurry      = 1 Soil Manure + 1000 mB Water, instant
Lime Suspension    = 1 TFC Lime Powder + 1000 mB Water, instant
Sulfur Suspension  = 1 TFC Sulfur Powder + 1000 mB Water, instant
Biochar Suspension = 1 Soil Biochar + 1000 mB Water, instant
```

Confirm the barrel accepts and retains each output fluid. Confirm JEI/TFC recipe viewing exposes the barrel recipes.

## 4. Containers

Fill a standard bucket and at least one TFC bucket/container that exposes a NeoForge item fluid handler.

For each physical fluid:

1. hold the filled container;
2. right-click compatible TFC soil/farmland;
3. verify the fluid is **applied rather than placed** on the soil;
4. verify Survival drains the applied amount;
5. verify Creative does not drain it;
6. verify the action-bar receipt reports mB and normalized dose.

Do not expect a TFC drinking jug to accept these fluids; R2 deliberately does not mark amendment fluids as drinkable.

## 5. Agronomy / persistence

Before and after application, use the Soil Probe and:

```mcfunction
/fsoil history
/fsoil preview 7
/fsoil hooks
```

For a delayed-release liquid, verify:

- immediate effect appears;
- pending release is recorded;
- TFC day advancement releases residual effect;
- save/restart retains pending release;
- history/event records identify the liquid amendment;
- overapplication still follows the 0.0.3a engine rules.

## 6. Regression

Verify:

- solid amendment bags still work;
- Field Guide opens and shows bucket specimen icons/mixing instructions;
- old R8 / 0.0.3a world loads without data loss;
- Soil Cell schema/model remains `3/3`;
- no irrigation/pump/pipe gameplay was introduced.
