# Foundations Soil 0.0.2a.R5 — Field Guide Render/Readability Test Plan

R5 is a presentation/render-order revision. It keeps the R4 book design, the R3 Field Guide payload, and the R2 Field Ledger / Soil Cell save schemas unchanged.

## 1. Native build

Run:

```bat
build_mod.bat
```

Expected artifact:

```text
build/libs/foundations_soil-0.0.2a.R5.jar
```

The build must pass `compileJava`, tests, and final JAR assembly before replacing R4.

## 2. Existing-world compatibility

Use the same world that already contains discovered cells/fields.

Expected:
- Soil Cells load unchanged;
- Field Ledger loads unchanged;
- names, notes, favorites, assignments, snapshots, and recent events remain intact;
- no save/schema migration is required.

## 3. Critical blur test

Open the Foundations Field Guide while standing in a visually busy outdoor area.

Expected:
- the world behind the guide may be darkened/separated;
- **book leather, parchment, specimen art, side-tab icons, text, search field, and page controls stay crisp**;
- opening/closing/reopening the guide does not progressively soften the UI;
- changing tabs does not introduce a blur pass over already-rendered content.

If the world is soft while the book remains sharp, that is acceptable. The book itself must not be blurred.

## 4. Header/binding test

Expected:
- `FOUNDATIONS FIELD GUIDE` is centered over the left page;
- no header text crosses or hides behind the central binding;
- right-page section titles remain inside the right-page content area.

Test at several GUI scales if practical.

## 5. R4 visual regression

Verify the R4 visual features still work:
- leather book frame;
- parchment pages;
- visible center binding;
- green bookmark;
- four vertical icon tabs;
- soil profile specimen;
- field/cell journal entries;
- amendment specimen cards;
- integrated search and paging.

## 6. Functional regression

Verify all four tabs and run:

```mcfunction
/fsoil status
/fsoil cell
/fsoil ledger status
/fsoil ledger list
```

Also verify Probe discovery, amendment refresh, HUD rendering, save/quit/reload, and existing ledger persistence.

## R5 pass condition

R5 passes when the native build succeeds, the book/art/text remain sharp, the title no longer overlaps the gutter, all R4 guide controls still work, and R2/R3/R4 soil/ledger persistence survives restart.
