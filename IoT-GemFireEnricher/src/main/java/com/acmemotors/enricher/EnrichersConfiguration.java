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
import java.util.ArrayList;
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
        public CarPosition enrich(CarPosition payload) {

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
            Map<JourneySite, PredictedSite> sites = new LinkedHashMap<>();
            for (JourneySite site : journeyHistory.getSites()) {
                sites.put(site, new PredictedSite(site.getLatitude(), site.getLongitude(), site.getCount()));
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

            payload.setSitePredictions(new ArrayList<>(sites.values()));
        }

        private static final Logger logger = LoggerFactory.getLogger(JourneySitesEnricher.class);

    }
}
