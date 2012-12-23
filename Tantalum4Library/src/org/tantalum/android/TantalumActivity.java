/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tantalum.android;

import android.app.Activity;
import android.os.Bundle;
import javax.microedition.midlet.MIDletStateChangeException;
import org.tantalum.PlatformUtils;
import org.tantalum.Worker;

/**
 *
 * @author phou
 */
public abstract class TantalumActivity extends Activity {

    public TantalumActivity() {
        PlatformUtils.setProgram(this);
    }

    /**
     * Call this to close your MIDlet in an orderly manner, exactly the same way
     * it is closed if the system sends you a destoryApp().
     *
     * Ongoing Work tasks will complete, or if you set unconditional then they
     * will complete within 3 seconds.
     *
     * @param unconditional
     */
    public void exitMIDlet(final boolean unconditional) {
        Worker.shutdown(unconditional);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AndroidCache.setContext(((Activity) this).getApplicationContext());
    }

    protected void onDestroy() {
        super.onDestroy();
        
        Worker.shutdown(true);
    }
    
    /**
     * Do not call this directly. Instead, call exitMidlet(false) to avoid
     * common errors.
     *
     * If you do for some reason call this directly, realize the MIDlet will
     * exit immediately after the call is complete, rather than wait for you to
     * call doNotifyDestroyed() once an ongoing file or RMS write actions are
     * completed. Usually, this is not desirable, call exitMidlet() instead and
     * ongoing tasks will complete.
     *
     * If you want something done during shutdown, use
     * Worker.queueShutdownTask(Workable) and it will be handled for you.
     *
     * @param unconditional
     * @throws MIDletStateChangeException
     */
    protected final void destroyApp(final boolean unconditional) {
        exitMIDlet(unconditional);
    }

    /**
     * Do nothing, not generally used
     *
     */
    protected void pauseApp() {
    }
}