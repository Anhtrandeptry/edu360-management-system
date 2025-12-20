package fpt.capstone.edu360managementsystem.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for handling Vietnamese text normalization.
 * Provides methods to remove diacritics from Vietnamese characters
 * for better search matching.
 */
public class VietnameseUtils {

    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Removes diacritics from Vietnamese text.
     * Example: "Nguyễn Văn Tuấn" -> "Nguyen Van Tuan"
     *
     * @param text the Vietnamese text to normalize
     * @return the text without diacritics, or null if input is null
     */
    public static String removeDiacritics(String text) {
        if (text == null) {
            return null;
        }
        
        // Special handling for Vietnamese characters
        String normalized = text
                .replace("đ", "d")
                .replace("Đ", "D");
        
        // Normalize to NFD form and remove combining diacritical marks
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = DIACRITIC_PATTERN.matcher(normalized).replaceAll("");
        
        return normalized;
    }

    /**
     * Checks if text contains the search term, ignoring Vietnamese diacritics.
     * Case-insensitive comparison.
     *
     * @param text       the text to search in
     * @param searchTerm the term to search for
     * @return true if the normalized text contains the normalized search term
     */
    public static boolean containsIgnoreDiacritics(String text, String searchTerm) {
        if (text == null || searchTerm == null) {
            return false;
        }
        
        String normalizedText = removeDiacritics(text).toLowerCase();
        String normalizedSearch = removeDiacritics(searchTerm).toLowerCase();
        
        return normalizedText.contains(normalizedSearch);
    }
}
