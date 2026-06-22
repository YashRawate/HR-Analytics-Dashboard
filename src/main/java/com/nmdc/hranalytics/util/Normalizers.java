package com.nmdc.hranalytics.util;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalization helpers ported 1:1 from app.py's normalize_* functions.
 */
public final class Normalizers {

    private Normalizers() { }

    private static final Set<String> BLANK_TOKENS = Set.of("", "None", "nan", "undefined", "null");

    private static final Map<String, String> PRE_ALIAS = Map.of(
            "T&S, E", "T&S and Environment",
            "T&S,E", "T&S and Environment"
    );

    private static boolean isBlank(String raw) {
        return raw == null || BLANK_TOKENS.contains(raw.trim());
    }

    /** Mirrors normalize_department() in app.py. */
    public static String normalizeDepartment(String raw) {
        if (isBlank(raw)) return null;
        String s = raw.trim();
        s = PRE_ALIAS.getOrDefault(s, s);

        // Exact match (case-insensitive) against known departments
        for (String d : Constants.ALL_DEPTS) {
            if (d.equalsIgnoreCase(s)) return d;
        }

        String sl = s.toLowerCase();
        if (sl.contains("plant") && (sl.contains("elec") || sl.contains("elect"))) return "Plant (Elect.)";
        if (sl.contains("plant") && sl.contains("mech")) return "Plant (Mech.)";
        if (sl.contains("plant")) return "Plant (Mech.)";
        if (sl.contains("service") && (sl.contains("elec") || sl.contains("elect"))) return "Services (Elect.)";
        if (sl.contains("service")) return "Services (Mech.)";
        if (sl.contains("mining")) return "Mining";
        if (sl.contains("geo")) return "Geology & QC";
        if (sl.contains("chem")) return "Chemical Lab";
        if (sl.contains("civil")) return "Civil";
        if (sl.contains("finance")) return "Finance";
        if (sl.contains("human") || Pattern.compile("\\bhr\\b").matcher(sl).find()) return "Human Resource";
        if (sl.contains("material")) return "Materials";
        if (sl.contains("school")) return "School";
        if (sl.contains("hospital")) return "Hospital";
        if (sl.contains("m&s") || sl.contains("m & s")) return "M&S";
        if (sl.contains("contract")) return "Contracts Dept.";
        if (sl.contains("works")) return "Works";
        if (sl.contains("vigilance")) return "Vigilance";
        if (sl.contains("csr")) return "CSR";
        if (sl.contains("ie dept") || sl.contains("industrial eng")) return "IE Dept.";
        if (sl.contains("cgm (p)") || sl.contains("gm (p)")) return "GM (P) Sectt.";
        if (sl.contains("commercial") || sl.contains("c &") || (sl.contains("c") && sl.contains("it"))) return "Commercial";
        if (sl.contains("t&s") || sl.contains("environment") || Pattern.compile("t&s\\s*,").matcher(sl).find())
            return "T&S and Environment";
        return s; // keep unknown as-is
    }

    /** Mirrors normalize_deposit() in app.py. */
    public static String normalizeDeposit(String raw) {
        if (raw == null || Set.of("", "None", "nan").contains(raw.trim())) return null;
        String s = raw.trim().toUpperCase();
        if (s.contains("11B")) return "11B";
        if (s.contains("11C")) return "11C";
        if (s.contains("-14") || s.equals("14") || s.endsWith("14")) return "14";
        return null;
    }

    /** Mirrors normalize_gender() in app.py. */
    public static String normalizeGender(String raw) {
        if (raw == null || Set.of("", "None", "nan").contains(raw.trim())) return null;
        String s = raw.trim().toLowerCase();
        if (s.equals("m") || s.equals("male")) return "Male";
        if (s.equals("f") || s.equals("female")) return "Female";
        return null;
    }

    /** Mirrors normalize_section() in app.py. */
    public static String normalizeSection(String raw) {
        if (isBlank(raw)) return null;
        return raw.trim();
    }

    // Matches sections like 'Excavation-11B', 'Field Ser-11C(Ele.)', 'CP&DH(E)-14'
    private static final Pattern SECTION_DEPOSIT_RE =
            Pattern.compile("^(?<base>.*?)[\\s\\-_/]*(?<deposit>11B|11C|14)[()\\w.\\s]*$", Pattern.CASE_INSENSITIVE);

    /** Mirrors split_section() in app.py — returns {base, deposit} pair (either may be null). */
    public static Map.Entry<String, String> splitSection(String raw) {
        String section = normalizeSection(raw);
        if (section == null) return new AbstractMap.SimpleEntry<>(null, null);
        Matcher m = SECTION_DEPOSIT_RE.matcher(section);
        if (!m.matches()) return new AbstractMap.SimpleEntry<>(section, null);
        String base = m.group("base").trim();
        // strip leading/trailing " -_/" characters like Python's strip(' -_/')
        base = base.replaceAll("^[\\s\\-_/]+", "").replaceAll("[\\s\\-_/]+$", "");
        if (base.isEmpty()) base = section;
        return new AbstractMap.SimpleEntry<>(base, m.group("deposit").toUpperCase());
    }

    /** Mirrors get_category() in app.py. */
    public static String getCategory(String dept) {
        if (Constants.PROD_SET.contains(dept)) return "Production";
        if (Constants.NONPROD_SET.contains(dept)) return "Non Production";
        if (Constants.OTHER_SET.contains(dept)) return "Others";
        return "Unknown";
    }

    /** Mirrors parse_skills() in app.py. */
    public static java.util.List<String> parseSkills(String skillsStr) {
        if (skillsStr == null || Set.of("", "nan", "None").contains(skillsStr.trim())) {
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String t : skillsStr.split("[,;|/]")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty() && !trimmed.equals("undefined") && !trimmed.equals("null")) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** Mirrors safe_str() in app.py. */
    public static String safeStr(Object val) {
        return safeStr(val, -1);
    }

    public static String safeStr(Object val, int maxLen) {
        String s = (val == null) ? "" : String.valueOf(val).trim();
        if (maxLen > 0 && s.length() > maxLen) {
            return s.substring(0, maxLen) + "\u2026";
        }
        return s;
    }
}
