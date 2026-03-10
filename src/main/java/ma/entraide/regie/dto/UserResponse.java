package ma.entraide.regie.dto;

public class UserResponse {

    private Long id;
    private String email;
    private String role;
    private Long regionId;
    private String regionName;
    private Long provinceId;
    private String provinceName;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String role, Long regionId, String regionName,
                        Long provinceId, String provinceName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.regionId = regionId;
        this.regionName = regionName;
        this.provinceId = provinceId;
        this.provinceName = provinceName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public Long getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
}

