/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.tantalum2.log;

/**
 * Utility class for logging.
 *
 * @author mark voit, paul houghton
 */
public class Log {

//#if UsbDebug
//#     public final static Log l = new UsbLog();
//#else
    public final static Log l = new Log();
//#endif
    private static long startTime = System.currentTimeMillis();

    /**
     * Logs given message.
     *
     * @param tag name of the class logging this message
     * @param message message to log
     */
    public final synchronized void log(final String tag, final String message) {
        //#debug
        printMessage(getMessage(tag, message));
    }

    /**
     * Logs given throwable and message.
     *
     * @param tag name of the class logging this message
     * @param message message to log
     * @param th throwable to log
     */
    public final synchronized void log(final String tag, final String message, final Throwable th) {
        //#mdebug
        printMessage(getMessage(tag, message) + ", EXCEPTION: " + th);
        if (th != null) {
            th.printStackTrace();
        }
        //#enddebug
    }

    /**
     * Prints given string to system out.
     *
     * @param string string to print
     */
    protected void printMessage(final String string) {
        System.out.println(string);
    }

    /**
     * Get formatted message string.
     *
     * @return message string
     */
    private String getMessage(final String tag, final String message) {
        return currentTime() + " (" + Thread.currentThread().getName() + "): " + tag + ": " + message;
    }

    /**
     * Return current time and thread name as string.
     *
     * @return current time as string
     */
    private String currentTime() {
        final long t = System.currentTimeMillis() - startTime;
        return (t / 1000) + "." + (t % 1000);
    }

    /**
     * Close any open resources. This is the last action before the MIDlet calls
     * MIDlet.notifyDestoryed(). This is used by UsbLog.
     *
     */
    public void shutdown() {
    }
}
