package fr.postiqa.core.domain.model.analysis;

/**
 * Business identity extracted from website analysis.
 * Represents the core business identity: name, tagline, value proposition, elevator pitch.
 */
public record BusinessIdentity(
    String businessName,
    String tagline,
    String valueProposition,
    String elevatorPitch
) {
    public BusinessIdentity {
        // Defensive copying not needed for strings (immutable)
    }

    public boolean hasValueProposition() {
        return valueProposition != null && !valueProposition.isBlank();
    }

    public boolean hasElevatorPitch() {
        return elevatorPitch != null && !elevatorPitch.isBlank();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String businessName;
        private String tagline;
        private String valueProposition;
        private String elevatorPitch;

        public Builder businessName(String businessName) {
            this.businessName = businessName;
            return this;
        }

        public Builder tagline(String tagline) {
            this.tagline = tagline;
            return this;
        }

        public Builder valueProposition(String valueProposition) {
            this.valueProposition = valueProposition;
            return this;
        }

        public Builder elevatorPitch(String elevatorPitch) {
            this.elevatorPitch = elevatorPitch;
            return this;
        }

        public BusinessIdentity build() {
            return new BusinessIdentity(
                businessName,
                tagline,
                valueProposition,
                elevatorPitch
            );
        }
    }
}
