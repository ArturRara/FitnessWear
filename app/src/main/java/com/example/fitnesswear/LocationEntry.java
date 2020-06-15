package com.example.fitnesswear;

import java.util.Calendar;

/**
 * A class that models a GPS location point with additional information about the time that the data
 * was obtained.
 */
public class LocationEntry {

    public double latitude;
    public double longitude;
    public Calendar calendar;
    public String day;

    public LocationEntry(Calendar calendar, double latitude, double longitude) {
        this.calendar = calendar;
        this.latitude = latitude;
        this.longitude = longitude;
        this.day = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocationEntry that = (LocationEntry) o;

        if (calendar.getTimeInMillis() != that.calendar.getTimeInMillis()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return calendar.hashCode();
    }
}
