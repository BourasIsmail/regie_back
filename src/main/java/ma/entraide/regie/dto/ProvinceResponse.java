package ma.entraide.regie.dto;

public class ProvinceResponse {

    private Long id;
    private String name;
    private Long regionId;
    private String regionName;

    public ProvinceResponse() {
    }

    public ProvinceResponse(Long id, String name, Long regionId, String regionName) {
        this.id = id;
        this.name = name;
        this.regionId = regionId;
        this.regionName = regionName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}

