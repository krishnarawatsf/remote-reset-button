package com.mock.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Domain model representing a Network Interface in the IETF YANG data model
 * (ietf-interfaces:interfaces).
 */
public class NetworkInterface {

    @NotBlank(message = "Interface name cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9/_.-]+$", message = "Interface name contains invalid characters")
    private String name;

    private String description;

    @NotBlank(message = "Interface type cannot be blank")
    private String type;

    private Boolean enabled;

    @Pattern(
        regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$|^$",
        message = "Invalid IPv4 address format"
    )
    private String ipAddress;

    @Min(value = 0, message = "Prefix length must be >= 0")
    @Max(value = 128, message = "Prefix length must be <= 128")
    private Integer prefixLength;

    public NetworkInterface() {
        this.enabled = true;
    }

    public NetworkInterface(String name, String description, String type, Boolean enabled, String ipAddress, Integer prefixLength) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.enabled = enabled != null ? enabled : true;
        this.ipAddress = ipAddress;
        this.prefixLength = prefixLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(Integer prefixLength) {
        this.prefixLength = prefixLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkInterface that = (NetworkInterface) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "NetworkInterface{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", enabled=" + enabled +
                ", ipAddress='" + ipAddress + '\'' +
                ", prefixLength=" + prefixLength +
                '}';
    }
}
