/*
 Copyright (c) 2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.tantalum.net.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.SAXParserFactory;
import org.tantalum.PlatformUtils;
import org.tantalum.Task;
import org.tantalum.util.L;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMLModel is a data structure extracted from XML text. The simplified parsing
 * and integration to utility methods allows these to work online and offline
 * (using RMS) transparent to the application developer using the caching
 * classes.
 *
 * For an example of how to use this for your XML data format, see RSSModel
 *
 * @author phou
 */
public abstract class XMLModel extends DefaultHandler {

    /**
     * A hierarchical list of XML tags appearing lower in a DOM tree than the
     * current tag during parsing. The entire DOM model is not saved, but you
     * can use this hierarchy for similar parse-time hierarchical condition
     * logic. In most simple XML cases you do not need this information.
     */
    protected String[] qnameStack = new String[100];
    /**
     * A hierarchical list of XML bodies lower in the DOM tree than the current
     * element during SAX parsing.
     */
    protected String[] charStack = new String[100];
    /**
     * A hierarchical list of XML attributes lower in the DOM tree than the
     * current element during SAX parsing.
     */
    protected XMLAttributes[] attributeStack = new XMLAttributes[100];
    /**
     * During SAX parsing, the current XML tag history depth. This increases as
     * we enter each tag, and decreases as we leave.
     */
    protected int currentDepth;

    private final Object MUTEX = PlatformUtils.getInstance().isSingleCore() ? Task.LARGE_MEMORY_MUTEX : new Object();

    /**
     * Parse the XML document using a SAX parser.
     *
     * @param xml
     * @throws SAXException
     */
    public void setXML(final byte[] xml) throws SAXException{
        if (xml == null) {
            throw new NullPointerException("Attempt to XML parse a null or zero byte value");
        }
        if (xml.length == 0) {
            throw new IllegalArgumentException("Attempt to XML parse a null or zero byte value");
        }
        synchronized (MUTEX) {
            final InputStream in = new ByteArrayInputStream(xml);

            qnameStack = new String[100];
            charStack = new String[100];
            attributeStack = new XMLAttributes[100];
            currentDepth = 0;

            try {
                //#mdebug
                L.i(this, "Start XML parse", "length=" + xml.length);
                final long t = System.currentTimeMillis();
                //#enddebug
                SAXParserFactory.newInstance().newSAXParser().parse(in, this);
                //#debug
                L.i(this, "End XML parse", "elapsedTime=" + (System.currentTimeMillis() - t) + "ms");
            } catch (SAXException t) {
                //#mdebug
                try {
                    L.e("SAX Parse error", new String(xml, "UTF-8"), t);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
                //#enddebug
                throw t;
            } catch (Throwable t) {
                //#debug
                L.e("Parse error", "", t);
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                    //#debug
                    L.e("Can not close", "bytearrayStream", e);
                }
                qnameStack = null;
                charStack = null;
                attributeStack = null;
            }
        }
    }

    /**
     * Implement this method to store fields of interest in your value object
     * from the qnameStack, charStack, and
     *
     * @param qname
     * @param chars
     * @param attributes
     */
    abstract protected void parseElement(String qname, String chars, XMLAttributes attributes);

    /**
     * Being SAX parsing an XML tag
     *
     * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        synchronized (MUTEX) {
            qnameStack[currentDepth] = qName;
            if (attributeStack[currentDepth] == null) {
                attributeStack[currentDepth] = new XMLAttributes(attributes);
            } else {
                attributeStack[currentDepth].setAttributes(attributes);
            }
            charStack[currentDepth] = "";
            ++currentDepth;
        }
    }

    /**
     * SAX parse the body of an XML element
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        synchronized (MUTEX) {
            charStack[currentDepth - 1] = new String(ch, start, length);
        }
    }

    /**
     * SAX parse the XML element closing tag
     *
     * @param uri
     * @param localName
     * @param qname
     * @throws SAXException
     */
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {
        synchronized (MUTEX) {
            --currentDepth;
            parseElement(qname, charStack[currentDepth], attributeStack[currentDepth]);
        }
    }
}
