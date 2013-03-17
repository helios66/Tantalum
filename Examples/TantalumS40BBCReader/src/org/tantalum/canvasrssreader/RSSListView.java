/*
 Copyright © 2012 Paul Houghton and Futurice on behalf of the Tantalum Project.
 All rights reserved.

 Tantalum software shall be used to make the world a better place for everyone.

 This software is licensed for use under the Apache 2 open source software license,
 http://www.apache.org/licenses/LICENSE-2.0.html

 You are kindly requested to return your improvements to this library to the
 open source community at http://projects.developer.nokia.com/Tantalum

 The above copyright and license notice notice shall be included in all copies
 or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package org.tantalum.canvasrssreader;

import org.tantalum.Task;
import org.tantalum.Worker;
import org.tantalum.net.StaticWebCache;
import org.tantalum.net.xml.RSSModel;
import org.tantalum.storage.DataTypeHandler;
import org.tantalum.util.L;
import org.xml.sax.SAXException;

/**
 *
 * @author phou
 */
public abstract class RSSListView extends View {

    static boolean prefetchImages = false;
    protected final RSSListView.LiveUpdateRSSModel rssModel = new RSSListView.LiveUpdateRSSModel();
    protected final StaticWebCache feedCache;

    public RSSListView(final RSSReaderCanvas canvas) {
        super(canvas);

        feedCache = StaticWebCache.getWebCache('5', new DataTypeHandler() {
	public Object convertToUseForm(final Object key, byte[] bytes) {
                try {
                    rssModel.setXML(bytes);

                    return rssModel;
                } catch (Exception e) {
                    //#debug
                    L.i("Error in parsing XML", rssModel.toString());
                    return null;
                }
            }
        });
    }

    protected void clearCache() {
        feedCache.clearAsync(new Task() {

            protected Object exec(final Object in) {
                reloadAsync(true);
                
                return in;
            }
            
        });
        DetailsView.imageCache.clearAsync(null);
    }

    /**
     * Reloads the feed
     */
    public Task reloadAsync(final boolean forceNetLoad) {
        this.renderY = 0;
        rssModel.removeAllElements();
        final Task rssResult = new Task() {
            public Object exec(final Object params) {
                canvas.refresh();

                return null;
            }
        };

        String feedUrl = RSSReader.INITIAL_FEED_URL;
        if (forceNetLoad) {
            feedCache.getAsync(feedUrl, Worker.HIGH_PRIORITY, StaticWebCache.GET_WEB, rssResult);
        } else {
            feedCache.getAsync(feedUrl, Worker.HIGH_PRIORITY, StaticWebCache.GET_ANYWHERE, rssResult);
        }

        return rssResult;
    }

    /**
     * Network access type of active connection or a set default access point.
     *
     * pd, pd.EDGE, pd.3G, pd.HSDPA, csd, bt_pan, wlan, na (can't tell)
     *
     * @return
     */
    public static void checkForWLAN() {
        final String status = System.getProperty("com.nokia.network.access");

        prefetchImages |= status != null && status.equals("wlan");
    }

    protected final class LiveUpdateRSSModel extends RSSModel {

        LiveUpdateRSSModel() {
            super(60);
        }

        public void setXML(final byte[] xml) throws SAXException, IllegalArgumentException {
            checkForWLAN(); // If this just came in over a WLAN net, getAsync the images also
            super.setXML(xml);
        }

        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (currentItem != null && qName.equals("item")) {
                if (items.size() < maxLength) {
                    if (prefetchImages) {
                        DetailsView.imageCache.prefetch(currentItem.getThumbnail());
                    }
                    canvas.refresh();
                }
            }
        }
    }

    public abstract boolean setSelectedIndex(int i);

    public abstract void selectItem(final int x, final int y, boolean tapped);

    public abstract void deselectItem();
}
