package com.foundations.soil.api.input;

@FunctionalInterface
public interface SoilInputHandler {
    SoilInputResult handle(SoilExternalInput input);
}
