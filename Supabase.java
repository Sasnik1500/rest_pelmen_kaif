package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Supabase {

    private static final String SUPABASE_URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";
    private static final OkHttpClient client = new OkHttpClient();

    // -------------------- PROMO --------------------

    public static Map<String, Object> getPromoByCode(String code) {
        String url = SUPABASE_URL + "promocodes?code=eq." + code;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                System.out.println("PROMO ERROR: " + response.code());
                return null;
            }

            String responseBody = response.body().string();

            JSONArray array = new JSONArray(responseBody);

            if (array.isEmpty()) {
                return null;
            }

            JSONObject obj = array.getJSONObject(0);

            Map<String, Object> promo = new HashMap<>();
            promo.put("code", obj.getString("code"));
            promo.put("discount", obj.getDouble("discount"));
            promo.put("active", obj.getBoolean("active"));

            return promo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------------------- REGISTER --------------------

    public static boolean registerUser(String username, String email, String password) {

        String url = SUPABASE_URL + "users";

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("email", email);
        json.put("password", password);
        json.put("role", "Покупатель");
        json.put("blocked", false);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}