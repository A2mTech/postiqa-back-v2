package fr.postiqa.features.socialaccounts.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object representing the result of a connection test.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {

    private Boolean isValid;
    private String message;
    private String errorDetails;
    private Object platformResponse;

    public static ConnectionTestResult success(String message) {
        return ConnectionTestResult.builder()
            .isValid(true)
            .message(message)
            .build();
    }

    public static ConnectionTestResult failure(String message, String errorDetails) {
        return ConnectionTestResult.builder()
            .isValid(false)
            .message(message)
            .errorDetails(errorDetails)
            .build();
    }
}
