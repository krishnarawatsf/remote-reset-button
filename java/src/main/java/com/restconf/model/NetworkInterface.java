package com.restconf.model;

public class NetworkInterface {
    private String name;
    private String description;
    private String type;
    private boolean enabled;
    private String ipAddress;
    private int prefixLength;

    public NetworkInterface() {}

    public NetworkInterface(String name, String description, String type,
                            boolean enabled, String ipAddress, int prefixLength) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.enabled = enabled;
        this.ipAddress = ipAddress;
        this.prefixLength = prefixLength;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPrefixLength() { return prefixLength; }
    public void setPrefixLength(int prefixLength) { this.prefixLength = prefixLength; }
}
