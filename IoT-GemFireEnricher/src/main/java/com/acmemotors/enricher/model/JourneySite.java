package com.acmemotors.enricher.model;

import java.io.Serializable;

/**
 * A model object that represents the site properties.
 */
public class JourneySite implements Serializable {

    private static final long serialVersionUID = 1;
    private String name;
    private double latitude;
    private double longitude;
    private int count;

    public JourneySite() {
    }

    public JourneySite(String name, double latitude, double longitude, int count) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
    }

    /**
     * The name of the destination.  May be null
     *
     * @return The name of the destination
     */
    public String getName() {
        return name;
    }

    /**
     * The lattitude of the destination
     *
     * @return latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * The longitude of the destination
     *
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * The number of visits of the site.
     *
     * @return count
     */
    public int getCount() {
        return count;
    }
}
