package com.mypkga.commerceplatformfull.util;

import org.springframework.web.util.HtmlUtils;

/**
 * Utility class for HTML operations
 */
public class HtmlUtilsHelper {
    
    /**
     * Decode HTML entities in a string
     * @param text The text containing HTML entities
     * @return Decoded text
     */
    public static String decodeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return HtmlUtils.htmlUnescape(text);
    }
    
    /**
     * Encode text to HTML entities
     * @param text The text to encode
     * @return Encoded text
     */
    public static String encodeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return HtmlUtils.htmlEscape(text);
    }
}