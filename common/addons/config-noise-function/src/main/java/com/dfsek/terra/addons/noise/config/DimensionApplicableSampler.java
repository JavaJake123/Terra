/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.noise.config;

import com.dfsek.seismic.type.sampler.Sampler;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;

import com.dfsek.terra.api.config.meta.Meta;


public class DimensionApplicableSampler implements ObjectTemplate<DimensionApplicableSampler> {
    @Value("dimensions")
    private @Meta int dimensions;

    @Value(".")
    private @Meta Sampler sampler;

    @Override
    public DimensionApplicableSampler get() {
        return this;
    }

    public int getDimensions() {
        return dimensions;
    }

    public Sampler getSampler() {
        return sampler;
    }
}
