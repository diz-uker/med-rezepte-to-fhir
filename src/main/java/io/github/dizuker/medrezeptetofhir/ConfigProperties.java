package io.github.dizuker.medrezeptetofhir;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties
@ConfigurationPropertiesScan
public record ConfigProperties(String appVersion) {}
