package org.tantalum.tests;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareEverythingForTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.tantalum.MockedStaticInitializers;
import org.tantalum.jme.JMEFontUtils;
import static org.mockito.Mockito.*;


import javax.microedition.lcdui.Font;
import java.util.Vector;

import static org.junit.Assert.assertEquals;

/**
 * User: kink
 * Date: 2013.03.26
 * Time: 15:34
 */
@PrepareForTest(JMEFontUtils.class)
public class FontUtilsTest extends MockedStaticInitializers {

    private Font font;
    private JMEFontUtils fontUtils;

    @Before
    public final void fontUtilsTestFixture() {
        font = PowerMockito.mock(Font.class);
        fontUtils = JMEFontUtils.getFontUtils(font, "...");
    }


    /**
     * Test of testTruncate method, of class StringUtils.
     */
    @Test
    public void testTruncate() {
        final String str_1 = "This is a a really long line of text--------------------------------------------------------------------------------------------------------------------------------";
        int maxWidth_1 = 240;
        final String expResult_1 = str_1.substring(0, 36) + "...";
        //String expResult_1 = "This is a a really long line of te...";

        when(font.stringWidth()).thenReturn(str_1.length());

        String result_1 = fontUtils.truncate(str_1, maxWidth_1);
        System.out.println(expResult_1);
        System.out.println(result_1);
        assertEquals(expResult_1, result_1);
    }

    /**
     * Test of testSplitToLines method, of class StringUtils.
     */
    @Test
    public void testSplitToLines() {
        System.out.println("splitToLines");
        Vector vector_1 = new Vector();
        String text_1 = "This is a a really long line of text";
        int maxWidth_1 = 100;

        fontUtils.splitToLines(vector_1, text_1, maxWidth_1, false);
        assertEquals(3, vector_1.size());
    }
}
