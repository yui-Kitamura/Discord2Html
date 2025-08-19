package pro.eng.yui.oss.d2h.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageInfoTest {

    @Test
    void convertsPlainHttpsLink() {
        String in = "See https://example.com now";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("See <a href=\"https://example.com\">https://example.com</a> now", out);
    }

    @Test
    void convertsMarkdownLabelLink() {
        String in = "Click [サイト](https://example.com/path)";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("Click <a href=\"https://example.com/path\">https://example.com/path</a>(サイト)", out);
    }

    @Test
    void escapesHtmlSpecialCharsAroundLinks() {
        String in = "5 < 6 & 7 > 3 \"quotes\" 'single' https://example.com";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("5 &lt; 6 &amp; 7 &gt; 3 &quot;quotes&quot; &#39;single&#39; <a href=\"https://example.com\">https://example.com</a>", out);
    }

    @Test
    void handlesMultipleLinksAndMixedMarkdown() {
        String in = "A https://example.pro and [lbl](https://example.com/path) end";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals(
                "A <a href=\"https://example.pro\">https://example.pro</a> and <a href=\"https://example.com/path\">https://example.com/path</a>(lbl) end",
                out
        );
    }

    
    /** 
     * Expect the raw text outside to be escaped, and links to stop before ) or > 
     * */
    @Test
    void doesNotIncludeTrailingParenthesisOrAngle() {
        String in = "link(https://example.com) and <https://example.org>";
        String out = MessageInfo.toHtmlWithLinks(in);
        // 
        assertEquals(
                "link(<a href=\"https://example.com\">https://example.com</a>) and &lt;<a href=\"https://example.org\">https://example.org</a>&gt;",
                out
        );
    }

    @Test
    void nullAndEmptyAreHandled() {
        assertEquals("", MessageInfo.toHtmlWithLinks(null));
        assertEquals("", MessageInfo.toHtmlWithLinks(""));
    }

    @Test
    void existingHtmlIsEscapedAndSafe() {
        String in = "<a href=\"https://example.com\">x</a> and text";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals(
                "&lt;a href=&quot;<a href=\"https://example.com\">https://example.com</a>&quot;&gt;x&lt;/a&gt; and text",
                out
        );
    }

    @Test
    void httpAndFtpAreNotConverted_plain() {
        String in = "visit http://example.net and ftp://files.example.net today";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("visit http://example.net and ftp://files.example.net today", out);
    }

    @Test
    void httpAndFtpAreNotConverted_markdownLabel() {
        String in = "[site](http://example.net) and [files](ftp://files.example.net)";
        String out = MessageInfo.toHtmlWithLinks(in);
        // markdown parts are not treated specially; remain as-is
        assertEquals("[site](http://example.net) and [files](ftp://files.example.net)", out);
    }

    @Test
    void stopsBeforeHtmlEntityLikeQuote() {
        String in = "See https://example.com\" now";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("See <a href=\"https://example.com\">https://example.com</a>&quot; now", out);
    }

    @Test
    void allowsAmpersandWithinUrlQuery() {
        String in = "Go https://example.com?a=1&b=2 .";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("Go <a href=\"https://example.com?a=1&amp;b=2\">https://example.com?a=1&amp;b=2</a> .", out);
    }

    @Test
    void supportsFragmentInPlainUrl() {
        String in = "Go https://example.com/path#section end";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("Go <a href=\"https://example.com/path#section\">https://example.com/path#section</a> end", out);
    }

    @Test
    void supportsFragmentInMarkdownUrl() {
        String in = "See [lbl](https://example.com/path#id)";
        String out = MessageInfo.toHtmlWithLinks(in);
        assertEquals("See <a href=\"https://example.com/path#id\">https://example.com/path#id</a>(lbl)", out);
    }
}
