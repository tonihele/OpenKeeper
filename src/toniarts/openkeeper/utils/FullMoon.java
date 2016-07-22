/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Converted from c http://www.voidware.com/phase.c
 */
package toniarts.openkeeper.utils;

import java.time.LocalDateTime;

public class FullMoon {
    private static final double RAD = (Math.PI/180.0);
    private static final double SMALL_FLOAT = (1e-12);
    // size of the deviation to be recognised as full moon
    private static final double DEVIATION = 0.005;

    /**
     * Checks if the current date has a full moon
     * @return true if it is full moon
     */
    public static boolean isFullMoon() {
        LocalDateTime now = LocalDateTime.now();
        return isFullMoon(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    /**
     * Checks if the specific date is on full moon
     * 
     * @param year
     * @param month
     * @param day
     * @param hour
     * @return true if it is full moon
     */
    public static boolean isFullMoon(final int year, final int month, final int day, final int hour) {
        LocalDateTime now = LocalDateTime.now();
        
        // percentiage to 100% phase
        double diffToFull = (1 - getMoonPhase(year, month, day, hour)) * 100;
        return diffToFull < DEVIATION;
    }

    /**
     * Returns the number of julian days for the specified day.
     * 
     * @param year
     * @param month
     * @param day
     * @return the number of julian days for the specified day.
     */
    private static double getJulianDays(int year,int month, final double day) {
        int a,
            b = 0,
            c,
            e;
        if (month < 3) {
            year--;
            month += 12;
        }
        if (year > 1582 || (year == 1582 && month > 10) ||
            (year == 1582 && month == 10 && day > 15)) {
            a = year / 100;
            b = 2 - a + a / 4;
        }
        c = (int) (365.25 * year);
        e = (int) (30.6001 * (month + 1));
        return b + c + e + day + 1720994.5;
    }

    private static double getSunPosition(double j) {
        double n,x,e,l,dl,v;
        double m2;
        int i;

        n = 360 / 365.2422*j;
        i = (int) (n / 360);
        n = n - i * 360.0;
        x = n - 3.762863;
        if (x < 0) x += 360;
        x *= RAD;
        e = x;
        do {
            dl = e - .016718 * Math.sin(e) - x;
            e = e - dl/(1 - .016718 * Math.cos(e));
        } while (Math.abs(dl) >= SMALL_FLOAT);
        v = 360 / Math.PI * Math.atan(1.01686011182 * Math.tan(e / 2));
        l = v + 282.596403;
        i = (int) (l / 360);
        l = l - i * 360.0;
        return l;
    }

    private static double getMoonPosition(final double j, final double ls) {

        double ms,l,mm,n,ev,sms,z,x,lm,bm,ae,ec;
        double d;
        double ds, as, dm;
        int i;

        ms = 0.985647332099 * j - 3.762863;
        if (ms < 0) ms += 360.0;
        l = 13.176396 * j + 64.975464;
        i = (int) (l / 360);
        l = l - i*360.0;
        if (l < 0) l += 360.0;
        mm = l - 0.1114041 * j - 349.383063;
        i = (int) mm / 360;
        mm -= i * 360.0;
        n = 151.950429 - 0.0529539 * j;
        i = (int) n / 360;
        n -= i * 360.0;
        ev = 1.2739 * Math.sin((2 * (l - ls) - mm) * RAD);
        sms = Math.sin(ms * RAD);
        ae = 0.1858*sms;
        mm += ev - ae - 0.37*sms;
        ec = 6.2886 * Math.sin(mm * RAD);
        l += ev + ec - ae + 0.214 * Math.sin(2 * mm * RAD);
        l = 0.6583 * Math.sin(2 * (l - ls) * RAD) + l;
        return l;
    }

    /**
     * Calculates more accurately than Moon_phase , the phase of the moon at 
     * the given epoch.
     * @param year
     * @param month
     * @param day
     * @param hour
     * @return the moon phase as a real number (0-1)
     */
    private static double getMoonPhase(final int year, final int month, final int day, final int hour) {
        double j = getJulianDays(year, month, (double)day + hour / 24.0) - 2444238.5;
        double ls = getSunPosition(j);
        double lm = getMoonPosition(j, ls);

        double t = lm - ls;
        if (t < 0) t += 360;
        return (1.0 - Math.cos((lm - ls) * RAD)) / 2;
    }
}
