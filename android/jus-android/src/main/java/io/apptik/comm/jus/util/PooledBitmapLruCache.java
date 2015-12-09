package io.apptik.comm.jus.util;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * True pooled Bitmap Cache. It works as follows.
 *
 * BitmapLruCache gets full --> overflowed bitmaps goes to the BitmapLruPool
 * BitmapLruPool gets full --> overflowed bitmaps goes to the SoftRef reusable Bitmap Set
 *
 */
public class PooledBitmapLruCache extends DefaultBitmapLruCache {

    LruCache<String, Bitmap> bPool;

    public PooledBitmapLruCache() {
        this(getDefaultLruCacheSize(), (int) (getDefaultLruCacheSize()*1.25));
    }

    public PooledBitmapLruCache(int maxSizeCache, int maxSizePool) {
        super(maxSizeCache);
        bPool = new LruCache<String, Bitmap>(maxSizePool) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return PooledBitmapLruCache.super.sizeOf(key, value);
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap
                    newValue) {
                //passes to the soft ref set
                PooledBitmapLruCache.super.addToPool(oldValue);
            }
        };
    }

    public LruCache<String, Bitmap> pool() {
        return bPool;
    }

    public void preFill(int w, int h) {
        int fill = size();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int size = getBitmapSize(bmp) / 1024;
        addToPool(bmp);
        fill+=size;
        while (fill<maxSize()) {
            addToPool(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            fill+=size;
        }
    }

    @Override
    public synchronized Bitmap getReusableBitmap(BitmapFactory.Options options) {
        Set<Map.Entry<String, Bitmap>> bitmaps = bPool.snapshot().entrySet();
        Bitmap bitmap = null;

        if (bitmaps != null && !bitmaps.isEmpty()) {
            for (Map.Entry<String, Bitmap> bmpEntry : bitmaps) {
                Bitmap item = bmpEntry.getValue();
                if (null != item && canBePooled(item)) {
                    // Check to see it the item can be used for inBitmap
                    if (canUseForInBitmap(item, options)) {
                        bitmap = item;
                        // Remove from reusable set so it can't be used again
                        bPool.remove(bmpEntry.getKey());
                        break;
                    }
                } else {
                    // Remove from the set if the reference has been cleared.
                    bPool.remove(bmpEntry.getKey());
                }
            }
        }

        if (bitmap != null) {
            return bitmap;
        } else {
            return super.getReusableBitmap(options);
        }
    }

    /**
     * adds bitmap to the LRU pool. If limit is reached value will be thrown at
     * {@link LruCache#entryRemoved(boolean, Object, Object, Object)} where it will be handled by
     * {@link DefaultBitmapLruCache#addToPool(Bitmap)}.
     */
    @Override
    public synchronized void addToPool(Bitmap bitmap) {
        if (canBePooled(bitmap)) {
            bPool.put(UUID.randomUUID().toString(), bitmap);
        }
    }
}