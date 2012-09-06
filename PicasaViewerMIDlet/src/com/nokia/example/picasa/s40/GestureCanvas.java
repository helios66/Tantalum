/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nokia.example.picasa.s40;

import com.futurice.tantalum3.log.L;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Canvas;

/**
 * A helper for creating gesture-based UIs that also work on older S40 phones
 *
 * @author phou
 */
public abstract class GestureCanvas extends Canvas {

    private final Timer spinTimer = new Timer();
    private TimerTask spinTimerTask = null; // Access within synchronized blocks only
    private long spinTimerDelay;
    protected int friction = GestureHandler.FRAME_ANIMATOR_FRICTION_LOW;
    protected final PicasaViewer midlet;
    protected final GestureHandler gestureHandler = new GestureHandler(this);
    protected int scrollY = 0;
    protected int top = getHeight();

    public GestureCanvas(final PicasaViewer midlet) {
        this.midlet = midlet;
        this.setTitle("GestureCanvas");
    }

    public int getScrollY() {
        return scrollY;
    }

    /**
     * Two fingers, moving in or out
     *
     * @param pinchDistanceStartingp
     * @param pinchDistanceCurrent
     * @param pinchDistanceChange
     * @param centerX
     * @param centerY
     * @param centerChangeX
     * @param centerChangeY
     */
    public void gesturePinch(
            int pinchDistanceStarting,
            int pinchDistanceCurrent,
            int pinchDistanceChange,
            int centerX,
            int centerY,
            int centerChangeX,
            int centerChangeY) {
    }

    /**
     * Short press
     *
     * @param startX
     * @param startY
     */
    public void gestureTap(int startX, int startY) {
    }

    /**
     * Hold
     *
     * @param startX
     * @param startY
     */
    public void gestureLongPress(int startX, int startY) {
    }

    /**
     * Hold, then hold again
     *
     * @param startX
     * @param startY
     */
    public void gestureLongPressRepeated(int startX, int startY) {
    }

    /**
     * The user is moving their finger and has not yet lifted it
     *
     * @param startX
     * @param startY
     * @param dragDistanceX
     * @param dragDistanceY
     */
    public void gestureDrag(int startX, int startY, int dragDistanceX, int dragDistanceY) {
        gestureHandler.animateDrag(dragDistanceX, dragDistanceY);
    }

    /**
     * The user finished a animateDrag motion by lifting their finger when not
     * moving
     *
     * @param startX
     * @param startY
     * @param dragDistanceX
     * @param dragDistanceY
     */
    public void gestureDrop(int startX, int startY, int dragDistanceX, int dragDistanceY) {
        gestureHandler.animateDrag(dragDistanceX, dragDistanceY);
    }

    /**
     * The user's finger was still moving when the lifted it from the screen
     *
     * The default implementation does kinetic scrolling on both X and Y. You
     * can reduce the computational load and thus slightly increase the frame
     * rate by overriding this if you are only interested in animation on one
     * axis.
     *
     * @param startX
     * @param startY
     * @param flickDirection
     * @param flickSpeed
     * @param flickSpeedX
     * @param flickSpeedY
     */
    public void gestureFlick(int startX, int startY, float flickDirection, int flickSpeed, int flickSpeedX, int flickSpeedY) {
        gestureHandler.kineticScroll(flickSpeed, GestureHandler.FRAME_ANIMATOR_FREE_ANGLE, friction, flickDirection);
    }

    public void showNotify() {
        gestureHandler.register();
        resumeSpin();

        super.showNotify();
    }

    public void hideNotify() {
        gestureHandler.stopAnimation();
        gestureHandler.unregister();
        pauseSpin();

        super.hideNotify();
    }

    public void sizeChanged(final int w, final int h) {
        gestureHandler.updateCanvasSize();

        super.sizeChanged(w, h);
    }

    public synchronized void startSpin(final long delay) {
        if (spinTimerTask != null || delay != spinTimerDelay) {
            // Spin speed has changed
            stopSpin();
        }
        spinTimerTask = new TimerTask() {
            public void run() {
                repaint();
            }
        };
        spinTimer.scheduleAtFixedRate(spinTimerTask, delay, delay);
    }

    public synchronized void stopSpin() {
        if (spinTimerTask != null) {
            spinTimerTask.cancel();
            spinTimerTask = null;
        }
    }

    private synchronized void pauseSpin() {
        if (spinTimerTask != null) {
            spinTimerTask.cancel();
        }
    }

    private synchronized void resumeSpin() {
        if (spinTimerTask != null) {
            startSpin(spinTimerDelay);
        }
    }

    /**
     * Update by painting at the new scroll position
     *
     * @param x
     * @param y
     * @param delta
     * @param deltaX
     * @param deltaY
     * @param lastFrame
     */
    public void animate(int x, int y, short delta, short deltaX, short deltaY, boolean lastFrame) {
        L.i("animate", "top y=" + y + " top=" + top + " scrollY=" + scrollY + " screenHeight=" + getHeight());
//        if (y > top) {
//            //#debug
//            L.i("animate", "bang top y=" + y + " top=" + top + " scrollY=" + scrollY + " screenHeight=" + getHeight());
//            scrollY = top;
////            gestureHandler.stopAnimation();
//        } else if (y < 0) {
//            //#debug
//            L.i("animate", "bang bottom y=" + y + " top=" + top + " scrollY=" + scrollY + " screenHeight=" + getHeight());
//            scrollY = 0;
////            gestureHandler.stopAnimation();
//        } else {
            scrollY = -y;
//        }
        repaint();
    }
}
