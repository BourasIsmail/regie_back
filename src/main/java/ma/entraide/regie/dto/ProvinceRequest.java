package ma.entraide.regie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProvinceRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Region ID is required")
    private Long regionId;

    public ProvinceRequest() {
    }

    public ProvinceRequest(String name, Long regionId) {
        this.name = name;
        this.regionId = regionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }
}

