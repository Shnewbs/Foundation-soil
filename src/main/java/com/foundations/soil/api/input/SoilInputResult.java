package com.foundations.soil.api.input;

public record SoilInputResult(Status status, String handler, String message) {
    public SoilInputResult {
        status = status == null ? Status.UNHANDLED : status;
        handler = handler == null ? "" : handler;
        message = message == null ? "" : message;
    }

    public static SoilInputResult unhandled() { return new SoilInputResult(Status.UNHANDLED, "", "No Soil input handler accepted the request"); }
    public static SoilInputResult accepted(String handler, String message) { return new SoilInputResult(Status.ACCEPTED, handler, message); }
    public static SoilInputResult rejected(String handler, String message) { return new SoilInputResult(Status.REJECTED, handler, message); }

    public enum Status { UNHANDLED, ACCEPTED, REJECTED }
}
