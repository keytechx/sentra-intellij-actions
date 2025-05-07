package org.intellij.sdk.action.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CancellationToken {
    private boolean canceled = false;

}
