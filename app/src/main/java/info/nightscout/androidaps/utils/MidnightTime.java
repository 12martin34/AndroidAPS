package info.nightscout.androidaps.utils;

import android.util.LongSparseArray;

import java.util.Calendar;

public class MidnightTime {
    private static final LongSparseArray<Long> times = new LongSparseArray<>();

    private static long hits = 0;
    private static long misses = 0;

    public static long calc() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long calc(long time) {
        Long m;
        synchronized (times) {
            m = times.get(time);
            if (m != null) {
                ++hits;
                return m;
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            m = c.getTimeInMillis();
            times.append(time, m);
            ++misses;
        }
        return m;
    }

    public static String log() {
        return "Hits: " + hits + " misses: " + misses + " stored: " + times.size();
    }
}
