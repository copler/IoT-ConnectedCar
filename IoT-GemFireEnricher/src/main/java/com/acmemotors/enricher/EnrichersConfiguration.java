/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acmemotors.enricher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

import com.acmemotors.enricher.model.JourneyHistory;
import com.acmemotors.enricher.model.JourneySite;
import com.acmemotors.rest.domain.CarPosition;
import com.acmemotors.rest.domain.CarPosition.PredictedSite;

/**
 * Converts the JSON string provided upstream into a
 * {@link com.acmemotors.rest.domain.CarPosition} object to be serialized in Gemfire.
 * This allows Spring Data Gemfire to retrieve POJOs instead of working with
 * {@link com.gemstone.gemfire.pdx.PdxInstance} objects.
 *
 * @author Michael Minella
 */
@Configuration
@EnableIntegration
public class EnrichersConfiguration {

    @Autowired
    private JourneySitesEnricher jorneyStopsEnricher;

    @Bean
    public MessageChannel input() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel output() {
        return new DirectChannel();
    }

    @MessageEndpoint
    public static class JourneySitesEnricher {

        private final ObjectMapper mapper;

        public JourneySitesEnricher() {
            this.mapper = new ObjectMapper();
            this.mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        }

        @Value("${modelPath}")
        private String modelPath;

        private Map<String, JourneyHistory> journeyHistoryMap;

        @PostConstruct
        protected void setup() {
            if (modelPath != null) {
                List<JourneyHistory> journeyHistories;
                try (FileInputStream fis = new FileInputStream(new File(modelPath))) {
                    journeyHistories = mapper.readValue(fis, new TypeReference<List<JourneyHistory>>() {});
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read specified model: " + modelPath, e);
                }
                journeyHistoryMap = new HashMap<>();
                for (JourneyHistory journeyHistory : journeyHistories) {
                    journeyHistoryMap.put(journeyHistory.getVin(), journeyHistory);
                }
            }
        }

        @Transformer(inputChannel = "input", outputChannel = "output")
        public CarPosition enrich(/*CarPosition*/Object object) {

            // TODO this is a serious issue of XD that the same class cannot be matched due to different class loaders
            Map<?, ?> map = mapper.convertValue(object, Map.class);
            CarPosition payload = mapper.convertValue(map, CarPosition.class);

            try {
                if (payload != null) {
                    doEnrich(payload);
                }
            } catch (final Exception e) {
                logger.error("Error enriching a CarPosition object", e);
            }

            return payload;
        }

        private void doEnrich(CarPosition payload) {
            if (payload.getVin() == null) {
                return;
            }
            JourneyHistory journeyHistory = journeyHistoryMap.get(payload.getVin());
            if (journeyHistory == null) {
                return;
            }

            // initialize list with default weight
            Map<String, PredictedSite> sites = new LinkedHashMap<>();
            for (String id : journeyHistory.getSites().keySet()) {
                JourneySite journeySite = journeyHistory.getSites().get(id);
                sites.put(id, new PredictedSite(journeySite.getLatitude(), journeySite.getLongitude(), journeySite.getCount()));
            }

            // apply site skipping
            for (PredictedSite site : sites.values()) {
                double c = getVectorsCos(payload.getLatitude(), payload.getLongitude(),
                    site.getLatitude(),
                    site.getLongitude(),
                    payload.getPredictions().get("2").getLatitude(),
                    payload.getPredictions().get("2").getLongitude()
                );
                if (c < 0) { // ~ 100 degrees
                    site.setProbability(site.getProbability() / (1 - Math.log(1 + c)));
                }
            }

            // normalize as weight-average
            double sum = 0;
            for (PredictedSite site : sites.values()) {
                sum += site.getProbability();
            }
            if (sum != 0) {
                for (PredictedSite site : sites.values()) {
                    site.setProbability(site.getProbability() / sum);
                }
            }

            payload.setSitePredictions(sites);
        }

        private static double getVectorsCos(Double x0, Double y0, double x1, double y1, Double x2, Double y2) {
            double dx1 = x1 - x0;
            double dy1 = y1 - y0;
            double dx2 = x2 - x0;
            double dy2 = y2 - y0;
            return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2));
        }

//        public static void main(String[] args) {
//            System.out.println(getVectorsCos(1.0, 1.0, 3.0, 2.0, 4.0, 4.0));
//            System.out.println(getVectorsCos(1.0, 1.0, 3.0, 1.0, 1.0, 4.0));
//            System.out.println(getVectorsCos(2.0, 1.0, 3.0, 1.0, 1.0, 4.0));
//        }

        private static final Logger logger = LoggerFactory.getLogger(JourneySitesEnricher.class);

    }
}
