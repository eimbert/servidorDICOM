package fhes.cat.enums;

public enum Estados {
    IN_PROGRESS("IN PROGRESS", "IP"),
    COMPLETED("COMPLETED", "CO"),
    DISCONTINUED("DISCONTINUED", "CA"),
    FIRST_IMAGE("FIRST IMAGE", "RI"),
	RECUPERAT("RECUPERAT", "RE");

    private final String originalValue;
    private final String abbreviation;

    Estados(String originalValue, String abbreviation) {
        this.originalValue = originalValue;
        this.abbreviation = abbreviation;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

}
