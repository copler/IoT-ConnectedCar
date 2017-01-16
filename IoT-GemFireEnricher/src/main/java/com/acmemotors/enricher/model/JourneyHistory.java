package com.acmemotors.enricher.model;

import java.io.Serializable;
import java.util.Map;

/**
 * A model object that represents the journeys site visits for a given VIN.
 */
public class JourneyHistory implements Serializable {

    private static final long serialVersionUID = 1;

    private String vin;

    private Map<String, JourneySite> sites;

    public JourneyHistory() {

    }

    public JourneyHistory(String vin, Map<String, JourneySite> sites) {
        this.vin = vin;
        this.sites = sites;
    }

    /**
     * The VIN the journeys are associated with
     *
     * @return the vehicle's VIN
     */
    public String getVin() {
        return vin;
    }

    /**
     * The list of {@link JourneySite} values, one per possible site to visit for
     * the associated VIN.
     *
     * @return the possible destinations
     */
    public Map<String, JourneySite> getSites() {
        return sites;
    }
}
