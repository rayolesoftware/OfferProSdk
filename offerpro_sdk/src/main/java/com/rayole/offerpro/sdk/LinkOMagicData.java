package com.rayole.offerpro.sdk;
import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LinkOMagicData {

    private final int id;
    @NonNull private final String name;
    @NonNull private final String description;
    @NonNull private final String offerImage;
    @NonNull private final String estTime;
    private final double rewardCoins;
    @NonNull private final String source;
    @NonNull private final String offerLink;
    @NonNull private final String directOfferLink;

    public LinkOMagicData(
            int id,
            @NonNull String name,
            @NonNull String description,
            @NonNull String offerImage,
            @NonNull String estTime,
            double rewardCoins,
            @NonNull String source,
            @NonNull String offerLink,
            @NonNull String directOfferLink
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.offerImage = offerImage;
        this.estTime = estTime;
        this.rewardCoins = rewardCoins;
        this.source = source;
        this.offerLink = offerLink;
        this.directOfferLink = directOfferLink;
    }

    // ---------------- Getters ----------------

    public int getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getDescription() { return description; }
    @NonNull public String getOfferImage() { return offerImage; }
    @NonNull public String getEstTime() { return estTime; }
    public double getRewardCoins() { return rewardCoins; }
    @NonNull public String getSource() { return source; }
    @NonNull public String getOfferLink() { return offerLink; }
    @NonNull public String getDirectOfferLink() { return directOfferLink; }

    // ---------------- JSON helpers ----------------

    @NonNull
    public static LinkOMagicData fromJson(@NonNull JSONObject json) throws JSONException {
        return new LinkOMagicData(
                json.optInt("id", 0),
                json.optString("name", ""),
                json.optString("description", ""),
                json.optString("offer_image", ""),
                json.optString("est_time", ""),
                json.optDouble("reward_coins", 0.0),
                json.optString("source", ""),
                json.optString("offer_link", ""),
                json.optString("direct_offer_link", "")
        );
    }

    /**
     * Converts to Map<String, Object> for Flutter
     */
    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("offer_image", offerImage);
        map.put("est_time", estTime);
        map.put("reward_coins", rewardCoins);
        map.put("source", source);
        map.put("offer_link", offerLink);
        map.put("direct_offer_link", directOfferLink);
        return map;
    }
}

