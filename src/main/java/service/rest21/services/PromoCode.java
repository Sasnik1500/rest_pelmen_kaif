package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PromoCodeService {

    private static PromoCodeService instance;

    private static final String BASE_URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/promocodes";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();

    private PromoCodeService() {
    }

    public static PromoCodeService getInstance() {
        if (instance == null) {
            instance = new PromoCodeService();
        }
        return instance;
    }

    public List<PromoCodeData> getAllPromoCodes() {
        List<PromoCodeData> result = new ArrayList<>();

        String url = BASE_URL + "?select=id,code,discount,used_count,usage_limit,expires_at,active,created_at&order=id.asc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return result;
            }

            String responseBody = response.body() != null ? response.body().string() : "[]";
            JSONArray array = new JSONArray(responseBody);

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);

                result.add(new PromoCodeData(
                        o.optLong("id", 0),
                        o.optString("code", ""),
                        o.optInt("discount", 0),
                        o.optInt("used_count", 0),
                        o.optInt("usage_limit", 0),
                        formatDate(o.optString("expires_at", "")),
                        o.optBoolean("active", true)
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean addPromoCode(String code, int discount, int usageLimit, String expiresAt, boolean active) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", code);
            json.put("discount", discount);
            json.put("used_count", 0);
            json.put("usage_limit", usageLimit);

            if (expiresAt == null || expiresAt.isBlank() || "Бессрочно".equalsIgnoreCase(expiresAt)) {
                json.put("expires_at", JSONObject.NULL);
            } else {
                json.put("expires_at", expiresAt);
            }

            json.put("active", active);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePromoCode(long id, String code, int discount, int usageLimit, String expiresAt, boolean active) {
        try {
            JSONObject json = new JSONObject();
            json.put("code", code);
            json.put("discount", discount);
            json.put("usage_limit", usageLimit);

            if (expiresAt == null || expiresAt.isBlank() || "Бессрочно".equalsIgnoreCase(expiresAt)) {
                json.put("expires_at", JSONObject.NULL);
            } else {
                json.put("expires_at", expiresAt);
            }

            json.put("active", active);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            String url = BASE_URL + "?id=eq." + id;

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setPromoActive(long id, boolean active) {
        try {
            JSONObject json = new JSONObject();
            json.put("active", active);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            String url = BASE_URL + "?id=eq." + id;

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePromoCode(long id) {
        try {
            String url = BASE_URL + "?id=eq." + id;

            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public PromoCodeData findActivePromoCode(String code) {
        try {
            String encodedCode = URLEncoder.encode(code.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "?select=id,code,discount,used_count,usage_limit,expires_at,active"
                    + "&code=eq." + encodedCode
                    + "&active=eq.true";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String responseBody = response.body() != null ? response.body().string() : "[]";
                JSONArray array = new JSONArray(responseBody);

                if (array.length() == 0) return null;

                JSONObject o = array.getJSONObject(0);

                return new PromoCodeData(
                        o.optLong("id", 0),
                        o.optString("code", ""),
                        o.optInt("discount", 0),
                        o.optInt("used_count", 0),
                        o.optInt("usage_limit", 0),
                        formatDate(o.optString("expires_at", "")),
                        o.optBoolean("active", true)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) return "Бессрочно";
        try {
            if (raw.length() >= 10) {
                return raw.substring(0, 10);
            }
            return raw;
        } catch (Exception e) {
            return raw;
        }
    }

    public static class PromoCodeData {
        private final long id;
        private final String code;
        private final int discount;
        private final int usedCount;
        private final int usageLimit;
        private final String expiresAt;
        private final boolean active;

        public PromoCodeData(long id, String code, int discount, int usedCount, int usageLimit, String expiresAt, boolean active) {
            this.id = id;
            this.code = code;
            this.discount = discount;
            this.usedCount = usedCount;
            this.usageLimit = usageLimit;
            this.expiresAt = expiresAt;
            this.active = active;
        }

        public long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public int getDiscount() {
            return discount;
        }

        public int getUsedCount() {
            return usedCount;
        }

        public int getUsageLimit() {
            return usageLimit;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public boolean isActive() {
            return active;
        }
    }
}