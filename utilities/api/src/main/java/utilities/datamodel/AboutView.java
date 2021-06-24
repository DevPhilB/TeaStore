package utilities.datamodel;

import java.util.Map;

public record AboutView (
        String storeIcon,
        String title,
        Map<String, String> portraits,
        String descartesDescription,
        String descartesLogo,
        String description
) {}
