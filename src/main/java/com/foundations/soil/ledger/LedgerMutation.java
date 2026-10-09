package com.foundations.soil.ledger;

/** A mutation is applied only to the authenticated player's ledger, never a supplied owner UUID. */
public record LedgerMutation(Action action, String cellKey, int fieldId, String name, String note, boolean favorite) {
    public enum Action { SAVE_CELL, SAVE_FIELD, CREATE_FIELD, DELETE_FIELD }
    public record Result(boolean success, String message) {}
    public LedgerMutation {
        if (action == null || cellKey == null || name == null || note == null || fieldId < 0
            || cellKey.length() > 160 || name.length() > 64 || note.length() > 256)
            throw new IllegalArgumentException("Invalid ledger mutation");
    }
    public Result apply(PlayerFieldLedger ledger) {
        FieldLedgerEntry cell = cellKey.isEmpty() ? null : ledger.entry(cellKey).orElse(null);
        FieldLedgerField field = fieldId == 0 ? null : ledger.field(fieldId).orElse(null);
        String cleanName = LedgerText.clean(name, 64), cleanNote = LedgerText.clean(note, 256);
        switch (action) {
            case SAVE_CELL -> {
                if (cell == null) return error("That cell has not been discovered in your ledger.");
                if (fieldId != 0 && field == null) return error("That field does not exist in your ledger.");
                cell.setFieldId(fieldId); cell.setNote(cleanNote); cell.setFavorite(favorite);
            }
            case SAVE_FIELD -> {
                if (field == null) return error("That field does not exist in your ledger.");
                if (cleanName.isBlank()) return error("A field name is required.");
                field.setName(cleanName); field.setNote(cleanNote); field.setFavorite(favorite);
            }
            case CREATE_FIELD -> {
                // Validate the entire request before assigning an ID or changing any state.
                if (!cellKey.isEmpty() && cell == null) return error("Discover that cell before assigning it.");
                if (cleanName.isBlank()) return error("A field name is required.");
                if (ledger.fields().stream().anyMatch(f -> f.name().equalsIgnoreCase(cleanName)))
                    return error("A field with that name already exists.");
                if (ledger.fieldCount() >= PlayerFieldLedger.MAX_FIELDS) return error("The field limit has been reached.");
                FieldLedgerField created;
                try { created = ledger.createField(cleanName); }
                catch (IllegalStateException exception) { return error(exception.getMessage()); }
                created.setNote(cleanNote); created.setFavorite(favorite);
                if (cell != null) cell.setFieldId(created.id());
            }
            case DELETE_FIELD -> {
                if (field == null) return error("That field no longer exists.");
                ledger.deleteField(fieldId);
            }
        }
        return new Result(true, action == Action.DELETE_FIELD ? "Field deleted; its cells remain in your ledger." : "Saved by the server.");
    }
    private static Result error(String message) { return new Result(false, message); }
}
