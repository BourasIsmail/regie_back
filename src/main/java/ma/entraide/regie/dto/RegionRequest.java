package ma.entraide.regie.dto;

import jakarta.validation.constraints.NotBlank;

public class RegionRequest {

    @NotBlank(message = "Name is required")
    private String name;

    public RegionRequest() {
    }

    public RegionRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
