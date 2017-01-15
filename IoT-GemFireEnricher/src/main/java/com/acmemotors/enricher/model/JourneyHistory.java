package com.acmemotors.enricher.model;

import java.io.Serializable;
import java.util.List;

/**
 * A model object that represents the journeys site visits for a given VIN.
 */
public class JourneyHistory implements Serializable {

    private static final long serialVersionUID = 1;

    private String vin;

    private List<JourneySite> sites;

    public JourneyHistory() {

    }

    public JourneyHistory(String vin, List<JourneySite> sites) {
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
    public List<JourneySite> getSites() {
        return sites;
    }
}
