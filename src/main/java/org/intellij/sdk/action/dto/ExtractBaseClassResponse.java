package org.intellij.sdk.action.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExtractBaseClassResponse {
    // Getters and Setters
    @JsonProperty("base_class")
    private String baseClass;

}
