// android/offerpro_sdk/src/main/java/com/rayole/offerpro/sdk/MegaOffer.java
package com.rayole.offerpro.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MegaOffer {

    private final int id;
    @NonNull private final String name;
    @NonNull private final String imageUrl;
    private final double rewardCoins;
    @NonNull private final String taskTypeName;
    @NonNull private final String directOfferLink;

    public MegaOffer(
            int id,
            @NonNull String name,
            @NonNull String imageUrl,
            double rewardCoins,
            @NonNull String taskTypeName,
            @NonNull String directOfferLink
    ) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.rewardCoins = rewardCoins;
        this.taskTypeName = taskTypeName;
        this.directOfferLink = directOfferLink;
    }

    public int getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getImageUrl() { return imageUrl; }
    public double getRewardCoins() { return rewardCoins; }
    @NonNull public String getTaskTypeName() { return taskTypeName; }
    @NonNull public String getDirectOfferLink() { return directOfferLink; }

    // ---- Helpers to work with JSON / Dart ----

    @NonNull
    public static MegaOffer fromJson(@NonNull JSONObject json) throws JSONException {
        int id = json.getInt("id");
        String name = json.optString("name", "");
        String image = json.optString("offer_image", "");
        double reward = json.optDouble("reward_coins", 0.0);

        String taskTypeName = "";
        JSONObject taskType = json.optJSONObject("task_type");
        if (taskType != null) {
            taskTypeName = taskType.optString("name", "");
        }

        String link = json.optString("direct_offer_link", "");

        return new MegaOffer(
                id,
                name,
                image,
                reward,
                taskTypeName,
                link
        );
    }

    /** Map that becomes Map<String, dynamic> on Dart side. */
    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("offer_image", imageUrl);
        m.put("reward_coins", rewardCoins);

        Map<String, Object> taskType = new HashMap<>();
        taskType.put("name", taskTypeName);
        m.put("task_type", taskType);

        m.put("direct_offer_link", directOfferLink);
        return m;
    }
}
