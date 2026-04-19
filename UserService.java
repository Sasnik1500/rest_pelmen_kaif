package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import service.rest21.models.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static UserService instance;

    private static final String BASE_URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/users";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();

    private UserService() {}

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public boolean register(String username, String password, String email, String phone) {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
        json.put("email", email);
        json.put("phone", phone);
        json.put("role", "Покупатель");
        json.put("blocked", false);

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
            String responseBody = response.body() != null ? response.body().string() : "";
            System.out.println("REGISTER STATUS: " + response.code());
            System.out.println("REGISTER BODY: " + responseBody);
            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User login(String username, String password) {
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String url = BASE_URL + "?username=eq." + encodedUsername;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;

            String responseBody = response.body() != null ? response.body().string() : "";
            JSONArray users = new JSONArray(responseBody);

            if (users.length() == 0) return null;

            JSONObject userJson = users.getJSONObject(0);

            boolean blocked = userJson.optBoolean("blocked", false);
            if (blocked) {
                throw new BlockedUserException("Ваш аккаунт заблокирован");
            }

            String storedPassword = userJson.getString("password");
            boolean passwordMatch = org.mindrot.jbcrypt.BCrypt.checkpw(password, storedPassword);

            if (!passwordMatch) return null;

            return new User(
                    userJson.getString("username"),
                    storedPassword,
                    userJson.getString("email"),
                    userJson.optString("phone", "")
            );

        } catch (BlockedUserException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<AdminUserRowData> getAllUsersAdmin() {
        List<AdminUserRowData> result = new ArrayList<>();

        String url = BASE_URL + "?select=id,username,email,role,blocked,created_at&order=id.asc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            // System.out.println("GET USERS STATUS: " + response.code());
            // System.out.println("GET USERS BODY: " + responseBody);

            if (!response.isSuccessful()) {
                return result;
            }

            JSONArray users = new JSONArray(responseBody);

            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);

                int id = u.optInt("id", 0);
                String username = u.optString("username", "");
                String email = u.optString("email", "");
                String role = u.optString("role", "Покупатель");
                boolean blocked = u.optBoolean("blocked", false);
                String createdAt = formatCreatedAt(u.optString("created_at", ""));

                result.add(new AdminUserRowData(
                        id,
                        i + 1,
                        username,
                        email,
                        role,
                        blocked,
                        createdAt
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean updateBlockedStatus(String username, boolean blocked) {
        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String url = BASE_URL + "?username=eq." + encodedUsername;

            JSONObject json = new JSONObject();
            json.put("blocked", blocked);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("UPDATE BLOCK STATUS: " + response.code());
                System.out.println("UPDATE BLOCK BODY: " + responseBody);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    public static class AdminUserRowData {
        private final int id;
        private final int number;
        private final String username;
        private final String email;
        private final String role;
        private final boolean blocked;
        private final String createdAt;

        public AdminUserRowData(int id, int number, String username, String email, String role, boolean blocked, String createdAt) {
            this.id = id;
            this.number = number;
            this.username = username;
            this.email = email;
            this.role = role;
            this.blocked = blocked;
            this.createdAt = createdAt;
        }

        public int getId() {
            return id;
        }

        public int getNumber() {
            return number;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class BlockedUserException extends RuntimeException {
        public BlockedUserException(String message) {
            super(message);
        }
    }
}