// android/offerpro_sdk/src/main/java/com/offerpro/sdk/SdkConfig.java
package com.rayole.offerpro.sdk;

public final class SdkConfig {
    public final String deviceId;
    public final String advertisingId;
    public final String userEmail;
    public final String userId;
    public final int appId;
    public final String userCountry;
    public final String encKey;     // secret – used only to encrypt

    private SdkConfig(Builder b) {
        this.deviceId = b.deviceId;
        this.advertisingId = b.advertisingId;
        this.userEmail = b.userEmail;
        this.userId = b.userId;
        this.appId = b.appId;
        this.userCountry = b.userCountry;
        this.encKey = b.encKey;
    }

    public static class Builder {
        private String deviceId;
        private String advertisingId;
        private String userEmail;
        private String userId;
        private int appId;
        private String userCountry;
        private String encKey;

        public Builder deviceId(String v) { this.deviceId = v; return this; }
        public Builder advertisingId(String v) { this.advertisingId = v; return this; }
        public Builder userEmail(String v) { this.userEmail = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder appId(int v) { this.appId = v; return this; }
        public Builder userCountry(String v) { this.userCountry = v; return this; }
        public Builder encKey(String v) { this.encKey = v; return this; }

        public SdkConfig build() {
//            if (appId == null || appId.isEmpty()) throw new IllegalStateException("appId is required");
            if (encKey == null || encKey.isEmpty()) throw new IllegalStateException("encKey is required");
            return new SdkConfig(this);
        }
    }
}
