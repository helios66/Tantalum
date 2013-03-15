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
package org.tantalum.j2me;

import java.util.Vector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import org.tantalum.Task;
import org.tantalum.Worker;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.util.L;
import org.tantalum.util.LengthLimitedVector;

/**
 * RMS Utility methods
 *
 * @author ssaa
 */
public final class RMSUtils {

    private static final int MAX_RECORD_NAME_LENGTH = 32;
    private static final int MAX_OPEN_RECORD_STORES = 10;
    private static final char RECORD_HASH_PREFIX = '@';
    private static final LengthLimitedVector openRecordStores = new LengthLimitedVector(MAX_OPEN_RECORD_STORES) {
        protected synchronized void lengthExceeded(final Object extra) {
            /**
             * We exceeded the maximum number of open record stores specified as
             * a constant. Close the least-recently-used record store.
             *
             * TODO Although this works under must circumstances, under extreme
             * conditions it may be that even the 10th least recently used
             * record store is still in use. Look for a more positive-proof way
             * to ensure we don't close anything we are still using.
             */
            final RecordStore rs = (RecordStore) extra;
            String rsName = "";

            try {
                rsName = rs.getName();
                //#debug
                L.i("Closing LRU record store", rsName + " open=" + openRecordStores.size());
                rs.closeRecordStore();
                //#debug
                L.i("LRU record store closed", rsName + " open=" + openRecordStores.size());
                removeElement(rs);
            } catch (Exception ex) {
                //#debug
                L.e("Can not close extra record store", rsName, ex);
            }
        }
    };

    private static class RMSUtilsHolder {

        private static RMSUtils instance = new RMSUtils();
    }

    /**
     * Access the singleton
     * 
     * @return 
     */
    public static RMSUtils getInstance() {
        return RMSUtilsHolder.instance;
    }

    /**
     * Singleton constructor
     *
     */
    private RMSUtils() {
        Worker.forkShutdownTask(new Task() {
            public Object exec(final Object in) {
                //#debug
                L.i("Closing record stores during shutdown", "open=" + openRecordStores.size());
                openRecordStores.setMaxLength(0);
                //#debug
                L.i("Closed record stores during shutdown", "open=" + openRecordStores.size());

                return in;
            }
        });
    }

    /**
     * Return of a list of record stores whose name indicates that they are
     * caches
     *
     * @return
     */
    public Vector getCacheRecordStoreNames() {
        final String[] rs = RecordStore.listRecordStores();

        if (rs == null) {
            return new Vector();
        }
        final Vector v = new Vector(rs.length);

        for (int i = 0; i < rs.length; i++) {
            String name = rs[i];
            if (name.charAt(0) == RECORD_HASH_PREFIX) {
                name = name.substring(1); // Cut off initial '@'
                v.addElement(name);
            }
        }

        return v;
    }

    /**
     * Return of a list of record stores which are not part of a cache
     *
     * @return
     */
    public Vector getNoncacheRecordStoreNames() {
        final String[] rs = RecordStore.listRecordStores();

        if (rs == null) {
            return new Vector();
        }
        final Vector v = new Vector(rs.length);

        for (int i = 0; i < rs.length; i++) {
            final String name = rs[i];
            if (name.charAt(0) != RECORD_HASH_PREFIX) {
                v.addElement(name);
            }
        }

        return v;
    }

    /**
     * Remove all record stores
     *
     * This is rather violent. Use only as a last resort, for example when
     * corruption is detected.
     */
    public void wipeRMS() {
        synchronized (openRecordStores) {
            while (!openRecordStores.isEmpty()) {
                final RecordStore rs = (RecordStore) openRecordStores.firstElement();
                String rsName = "";

                try {
                    rsName = rs.getName();
                    //#debug
                    L.i("Closing record store before wipeRMS", rsName + " open=" + openRecordStores.size());
                    rs.closeRecordStore();
                    //#debug
                    L.i("Record store closed before wipeRMS", rsName + " open=" + openRecordStores.size());
                    openRecordStores.removeElement(rs);
                } catch (Exception ex) {
                    //#debug
                    L.e("Can not close record store before wipeRMS", rsName, ex);
                }
            }
        }

        final String[] rs = RecordStore.listRecordStores();

        for (int i = 0; i < rs.length; i++) {
            try {
                RecordStore.deleteRecordStore(rs[i]);
            } catch (Exception ex) {
                //#debug
                L.e("wipeRMS(), problem deleting record store", rs[i], ex);
            }
        }
    }

    private String getRecordStoreCacheName(final String key) {
        final StringBuffer sb = new StringBuffer(MAX_RECORD_NAME_LENGTH);

        sb.append(RECORD_HASH_PREFIX);
        if (key.length() > MAX_RECORD_NAME_LENGTH - 1) {
            sb.append(Integer.toString(key.hashCode(), Character.MAX_RADIX));
            final int fullLength = sb.length() + key.length();
            if (fullLength > MAX_RECORD_NAME_LENGTH) {
                sb.append(key.substring(0, MAX_RECORD_NAME_LENGTH));
            } else {
                sb.append(key);
            }
        } else {
            // Short key, just prepend the 
            sb.append(key);
        }

        return sb.toString();
    }

    /**
     * Write to the record store a cached value based on the hashcode of the key
     * to the data
     *
     * @param key
     * @param data
     * @throws RecordStoreFullException
     * @throws FlashDatabaseException 
     */
    public void cacheWrite(final String key, final byte[] data) throws RecordStoreFullException, FlashDatabaseException {
        write(getRecordStoreCacheName(key), data);
    }

    /**
     * Writes the byte array to the record store. Deletes the previous data.
     *
     * @param key
     * @param data
     * @throws RecordStoreFullException
     * @throws FlashDatabaseException 
     */
    public void write(final String key, final byte[] data) throws RecordStoreFullException, FlashDatabaseException {
        final RecordStore rs;
        final String recordStoreName = truncateRecordStoreNameToLast32(key);

        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Can not RMSUtils.write(), null or trivial key: " + key);
        }
        if (data == null) {
            throw new IllegalArgumentException("Can not RMSUtils.write(), data is null: " + key);
        }
        try {
            //delete old value
            //#debug
            L.i("Add to RMS", key + " (" + data.length + " bytes)");
            rs = getRecordStore(recordStoreName, true);

            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
            //#debug
            L.i("Added to RMS", recordStoreName + " (" + data.length + " bytes)");
        } catch (RecordStoreFullException e) {
            //#debug
            L.i("RMS FULL when writing", key + " " + recordStoreName);
            throw e;
        } catch (Exception e) {
            try {
                //#debug
                L.e("RMS write problem, will attempt to delete record", key + " " + recordStoreName, e);
                delete(key);
            } finally {
                throw new FlashDatabaseException("RMS write problem, delete was attempted: " + key + " : " + e);
            }
        }
    }

    /**
     * Read from the record store a cached value based on the hashcode of the
     * key to the data
     *
     * @param key
     * @return
     * @throws FlashDatabaseException
     */
    public byte[] cacheRead(final String key) throws FlashDatabaseException {
        return read(getRecordStoreCacheName(key));
    }

    /**
     * Reads the data from the given record store.
     *
     * @param key
     * @return
     * @throws FlashDatabaseException
     */
    public byte[] read(final String key) throws FlashDatabaseException {
        final RecordStore rs;
        final String recordStoreName = truncateRecordStoreNameToLast32(key);
        byte[] data = null;

        try {
            //#debug
            L.i("Read from RMS", recordStoreName);
            rs = getRecordStore(recordStoreName, false);
            if (rs != null && rs.getNumRecords() > 0) {
                data = rs.getRecord(1);
                //#debug
                L.i("End read from RMS", recordStoreName + " (" + data.length + " bytes)");
            } else {
                //#debug
                L.i("End read from RMS", recordStoreName + " (NOTHING TO READ)");
            }
        } catch (Exception e) {
            //#debug
            L.e("Can not read RMS", recordStoreName, e);
            throw new FlashDatabaseException("Can not read record from RMS: " + key + " - " + recordStoreName + " : " + e);
        }

        return data;
    }

    /**
     * Delete one item from a cache
     *
     * @param key
     * @throws FlashDatabaseException 
     */
    public void cacheDelete(final String key) throws FlashDatabaseException {
        delete(getRecordStoreCacheName(key));
    }

    /**
     * Get a RecordStore. This method supports a global pool of open record
     * stores and thereby avoids repeated opening and closing of record stores
     * which are used several times.
     *
     * @param recordStoreName
     * @param createIfNecessary
     * @return null if the record store does not exist
     * @throws RecordStoreException
     */
    private RecordStore getRecordStore(final String recordStoreName, final boolean createIfNecessary) throws FlashDatabaseException {
        RecordStore rs = null;
        boolean success = false;

        try {
            rs = RecordStore.openRecordStore(recordStoreName, createIfNecessary);
            openRecordStores.addElement(rs);
            success = true;
        } catch (RecordStoreNotFoundException e) {
            success = !createIfNecessary;
            rs = null;
        } catch (RecordStoreException e) {
            throw new FlashDatabaseException("Can not get RMS: " + recordStoreName + " : " + e);
        } finally {
            if (!success) {
                //#debug
                L.i("Can not open record store", "Attempting RMS delete " + recordStoreName);
                delete(recordStoreName);
            }
        }

        return rs;
    }

    /**
     * Delete one item
     *
     * @param key
     * @throws FlashDatabaseException 
     */
    public void delete(final String key) throws FlashDatabaseException {
        final String truncatedRecordStoreName = truncateRecordStoreNameToLast32(key);

        try {
            final RecordStore[] recordStores;

            synchronized (openRecordStores) {
                recordStores = new RecordStore[openRecordStores.size()];
                openRecordStores.copyInto(recordStores);
            }

            /**
             * Close existing references to the record store
             *
             * NOTE: This does not absolutely guarantee that there is no other
             * thread accessing this record store at this exact moment. If that
             * happens, you will be prevented from deleting the record store and
             * "RMS delete problem" message will show up in your debug. For this
             * reason, your application's logic may need to take into account
             * that a delete might not be completed.
             *
             * TODO Without adding complexity, a file locking mechanism or other
             * solution may be added in future. Another solution might be to
             * read and remember the RMS contents on startup, use that as an
             * in-memory index... Still expensive. -paul
             */
            for (int i = 0; i < recordStores.length; i++) {
                try {
                    if (recordStores[i].getName().equals(truncatedRecordStoreName)) {
                        openRecordStores.markAsExtra(recordStores[i]);
                    }
                } catch (RecordStoreNotOpenException ex) {
                    //#debug
                    L.e("Mark as extra close of record store failed", truncatedRecordStoreName, ex);
                }
            }
            RecordStore.deleteRecordStore(key);
        } catch (RecordStoreNotFoundException ex) {
            //#debug
            L.i("RMS not found (normal result)", key);
        } catch (RecordStoreException ex) {
            //#debug
            L.e("RMS delete problem", key, ex);
            throw new FlashDatabaseException("Can not delete RMS: " + key + " : " + ex);
        }
    }

    /**
     * Shorten the name to fit within the 32 character limit imposed by RMS.
     *
     * @param recordStoreName
     * @return
     */
    private String truncateRecordStoreNameToLast32(String recordStoreName) {
        if (recordStoreName == null || recordStoreName.length() == 0) {
            throw new IllegalArgumentException("null or trivial record store name");
        }
        if (recordStoreName.length() > MAX_RECORD_NAME_LENGTH) {
            recordStoreName = recordStoreName.substring(recordStoreName.length() - MAX_RECORD_NAME_LENGTH);
        }

        return recordStoreName;
    }
}
