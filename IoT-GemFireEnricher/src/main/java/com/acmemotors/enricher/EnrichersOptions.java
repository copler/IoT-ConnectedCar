package com.acmemotors.enricher;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

public class EnrichersOptions implements ProfileNamesProvider {

    private String modelPath;

    @NotNull
    public String getModelPath() {
        return modelPath;
    }

    @ModuleOption("the path to sites model")
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public String[] profilesToActivate() {
        return new String[] { "default" };
    }

}