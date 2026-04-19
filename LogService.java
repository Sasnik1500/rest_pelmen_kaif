package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogService {

    private static LogService instance;

    private static final String BASE_URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/logs";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();

    private LogService() {
    }

    public static LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }

    public boolean addLog(String username, String action, String tableName, String details) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username == null || username.isBlank() ? "Неизвестно" : username);
            json.put("action", action == null ? "" : action);
            json.put("table_name", tableName == null ? "Система" : tableName);
            json.put("details", details == null ? "" : details);

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

    public List<LogRowData> getAllLogs() {
        List<LogRowData> result = new ArrayList<>();

        String url = BASE_URL + "?select=id,created_at,username,action,table_name,details&order=id.desc&limit=500";

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

                result.add(new LogRowData(
                        o.optLong("id", 0),
                        formatCreatedAt(o.optString("created_at", "")),
                        o.optString("username", ""),
                        o.optString("action", ""),
                        o.optString("table_name", ""),
                        o.optString("details", "")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String formatCreatedAt(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            OffsetDateTime dt = OffsetDateTime.parse(raw);
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return raw;
        }
    }

    public static class LogRowData {
        private final long id;
        private final String createdAt;
        private final String username;
        private final String action;
        private final String tableName;
        private final String details;

        public LogRowData(long id, String createdAt, String username, String action, String tableName, String details) {
            this.id = id;
            this.createdAt = createdAt;
            this.username = username;
            this.action = action;
            this.tableName = tableName;
            this.details = details;
        }

        public long getId() {
            return id;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUsername() {
            return username;
        }

        public String getAction() {
            return action;
        }

        public String getTableName() {
            return tableName;
        }

        public String getDetails() {
            return details;
        }
    }
}