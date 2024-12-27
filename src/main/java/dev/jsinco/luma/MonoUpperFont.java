package dev.jsinco.luma;

public class MonoUpperFont {

    // Mapping of normal letters to their 'monoupper' equivalents
    private static final String NORMAL_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MONO_UPPER_ALPHABET = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ";

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

        // Iterate through each character in the input string
        for (char c : input.toCharArray()) {
            int index = NORMAL_ALPHABET.indexOf(c);

            // If the character is a regular alphabet letter, convert it
            if (index >= 0) {
                result.append(MONO_UPPER_ALPHABET.charAt(index));
            } else {
                // If not a letter, just append the original character
                result.append(c);
            }
        }

        return result.toString();
    }
}
