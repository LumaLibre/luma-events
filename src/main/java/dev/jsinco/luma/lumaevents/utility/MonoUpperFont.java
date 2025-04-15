package dev.jsinco.luma.lumaevents.utility;

public class MonoUpperFont {

    // Mapping of normal letters to their 'monoupper' equivalents
    private static final String NORMAL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MONO_UPPER_ALPHABET = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ";

    /**
     * Converts a string to 'monoupper' text, where each letter is replaced
     * by a special character from the MONO_UPPER_ALPHABET set.
     *
     * @param input The input string to convert
     * @return The converted string in 'monoupper' form
     */
    public static String toMonoupperText(String input) {
        if (input == null) {
            return null; // Handle null input
        }

        StringBuilder result = new StringBuilder();
        String regex = "(<[^>]*>)|([^<]+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Group 1: Content inside < > tags, leave it unchanged
                result.append(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // Group 2: Content outside < > tags, convert to 'monoupper'
                String outsideText = matcher.group(2).toUpperCase();
                for (char c : outsideText.toCharArray()) {
                    int index = NORMAL_ALPHABET.indexOf(c);
                    if (index >= 0) {
                        result.append(MONO_UPPER_ALPHABET.charAt(index));
                    } else {
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }
}
