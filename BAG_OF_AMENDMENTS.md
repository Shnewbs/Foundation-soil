# Amendment Bags — 0.0.2a.R2

Filled bags still contain eight Survival applications and return the Empty Amendment Bag when depleted.

Built-in amendments:

| Amendment | Immediate role | Slow reserve | Relative decomposition | Future delivery metadata |
|---|---|---:|---|---|
| Lime | raises pH | none | none | suspendable |
| Sulfur | lowers pH | none | none | suspendable |
| Clay | structure / water behavior tradeoff | none | none | suspendable |
| Compost | OM + biology + structure | yes | medium | suspendable |
| Biochar | structure/drainage + persistent carbon | yes | very slow | suspendable |
| Manure | OM + biology, some salinity | yes | fast | suspendable |

Repeated applications on the same Soil Cell have diminishing immediate effectiveness. This is temporary: the recent amendment load decays with TFC calendar time.

## Inputs

- Lime uses TFC lime powder.
- Sulfur uses TFC sulfur powder.
- Clay uses Minecraft clay balls (present in TFC progression/worldgen).
- Compost uses TFC compost.
- Biochar is produced by heating supported organic biomass and then bagged.
- Manure can be collected event-driven from adult TFC livestock by using an Empty Amendment Bag; one loose Manure is available per configured cooldown. Eight loose Manure fills a Manure Amendment Bag.

The delivery-form metadata is only a compatibility foundation in 2a. Mixing these into irrigation water is intentionally deferred to the later fertigation update.
