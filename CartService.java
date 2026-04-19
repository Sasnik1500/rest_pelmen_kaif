package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import service.rest21.models.CartItem;
import service.rest21.models.Product;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CartService {

    private static CartService instance;

    private static final String URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/cart_items";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();
    private final ProductService productService = ProductService.getInstance();

    private CartService() {
    }

    public static CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public List<CartItem> loadCart(String username) {
        List<CartItem> result = new ArrayList<>();

        if (username == null || username.isBlank()) {
            return result;
        }

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(URL + "?username=eq." + encodedUsername + "&select=product_id,quantity")
                    .get()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "[]";

                System.out.println("LOAD CART STATUS: " + response.code());
                System.out.println("LOAD CART BODY: " + body);

                if (!response.isSuccessful() || !body.trim().startsWith("[")) {
                    return result;
                }

                JSONArray array = new JSONArray(body);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    int productId = obj.optInt("product_id", 0);
                    int quantity = obj.optInt("quantity", 1);

                    Product product = productService.getProductById(productId);
                    if (product != null) {
                        result.add(new CartItem(product, quantity));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean saveCartItem(String username, Product product, int quantity) {
        if (username == null || username.isBlank() || product == null) {
            return false;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("product_id", product.getId());
            json.put("quantity", quantity);

            System.out.println("SAVE CART JSON: " + json);

            Request request = new Request.Builder()
                    .url(URL + "?on_conflict=username,product_id")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("SAVE CART STATUS: " + response.code());
                System.out.println("SAVE CART BODY: " + body);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCartItemQuantity(String username, Product product, int quantity) {
        if (username == null || username.isBlank() || product == null) {
            return false;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("quantity", quantity);

            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(URL + "?username=eq." + encodedUsername + "&product_id=eq." + product.getId())
                    .patch(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("UPDATE CART STATUS: " + response.code());
                System.out.println("UPDATE CART BODY: " + body);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCartItem(String username, Product product) {
        if (username == null || username.isBlank() || product == null) {
            return false;
        }

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(URL + "?username=eq." + encodedUsername + "&product_id=eq." + product.getId())
                    .delete()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("DELETE CART STATUS: " + response.code());
                System.out.println("DELETE CART BODY: " + body);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean clearCart(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(URL + "?username=eq." + encodedUsername)
                    .delete()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("CLEAR CART STATUS: " + response.code());
                System.out.println("CLEAR CART BODY: " + body);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}