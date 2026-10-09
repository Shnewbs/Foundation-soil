package com.foundations.soil.client;

import com.foundations.soil.network.LaboratoryOpenPayload;
import com.foundations.soil.network.LaboratoryResultPayload;
public final class ClientLaboratoryState {
    private static LaboratoryOpenPayload open;
    private static LaboratoryResultPayload result;
    private ClientLaboratoryState() {}
    public static void accept(LaboratoryOpenPayload p) { open = p; }
    public static void accept(LaboratoryResultPayload p) { result = p; }
    public static LaboratoryOpenPayload takeOpen() { var p = open; open = null; return p; }
    public static LaboratoryResultPayload takeResult() { var p = result; result = null; return p; }
    public static void clear() { open = null; result = null; }
}
