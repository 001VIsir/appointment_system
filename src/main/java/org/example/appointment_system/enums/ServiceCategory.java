package org.example.appointment_system.enums;

/**
 * Service category enumeration for classifying service items.
 *
 * <p>Defines the types of services that merchants can offer:</p>
 * <ul>
 *   <li>{@link #GENERAL} - General services</li>
 *   <li>{@link #MEDICAL} - Medical and healthcare services</li>
 *   <li>{@link #BEAUTY} - Beauty and wellness services</li>
 *   <li>{@link #CONSULTATION} - Consulting and advisory services</li>
 *   <li>{@link #EDUCATION} - Educational and training services</li>
 *   <li>{@link #FITNESS} - Fitness and sports services</li>
 *   <li>{@link #OTHER} - Other uncategorized services</li>
 * </ul>
 */
public enum ServiceCategory {

    /**
     * General services category.
     * Default category for services that don't fit other categories.
     */
    GENERAL("General", "General Services"),

    /**
     * Medical and healthcare services.
     * Includes clinics, hospitals, dental services, etc.
     */
    MEDICAL("Medical", "Medical & Healthcare"),

    /**
     * Beauty and wellness services.
     * Includes salons, spas, skincare, etc.
     */
    BEAUTY("Beauty", "Beauty & Wellness"),

    /**
     * Consulting and advisory services.
     * Includes business consulting, legal advice, financial planning, etc.
     */
    CONSULTATION("Consultation", "Consulting Services"),

    /**
     * Educational and training services.
     * Includes tutoring, courses, workshops, etc.
     */
    EDUCATION("Education", "Education & Training"),

    /**
     * Fitness and sports services.
     * Includes gyms, personal training, sports coaching, etc.
     */
    FITNESS("Fitness", "Fitness & Sports"),

    /**
     * Other uncategorized services.
     * Fallback category for services that don't fit standard categories.
     */
    OTHER("Other", "Other Services");

    private final String code;
    private final String displayName;

    ServiceCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Get the short code for the category.
     *
     * @return the category code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
