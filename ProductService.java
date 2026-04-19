package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import service.rest21.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {

    private static ProductService instance;

    private static final String URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/products";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();

    private ProductService() {
    }

    public static ProductService getInstance() {
        if (instance == null) {
            instance = new ProductService();
        }
        return instance;
    }

    // =========================
    // ДЛЯ ПОЛЬЗОВАТЕЛЯ (только активные)
    // =========================
    public List<Product> getAllProducts() {
        List<Product> result = new ArrayList<>();

        try {
            Request request = new Request.Builder()
                    .url(URL + "?select=*&active=eq.true&order=id.asc")
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";

                if (!body.trim().startsWith("[")) {
                    return result;
                }

                JSONArray arr = new JSONArray(body);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    result.add(new Product(
                            o.optInt("id"),
                            o.optString("name"),
                            o.optString("category"),
                            o.optDouble("price"),
                            o.optString("description"),
                            o.optString("image"),
                            o.optString("detailed"),
                            true
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // =========================
    // ДЛЯ АДМИНА (все товары)
    // =========================
    public List<Product> getAllProductsForAdmin() {
        List<Product> result = new ArrayList<>();

        try {
            Request request = new Request.Builder()
                    .url(URL + "?select=*&order=id.asc")
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";

                if (!body.trim().startsWith("[")) {
                    return result;
                }

                JSONArray arr = new JSONArray(body);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    result.add(new Product(
                            o.optInt("id"),
                            o.optString("name"),
                            o.optString("category"),
                            o.optDouble("price"),
                            o.optString("description"),
                            o.optString("image"),
                            o.optString("detailed"),
                            o.optBoolean("active", true)
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // =========================
    // ФИЛЬТРЫ
    // =========================
    public List<Product> getProductsByCategory(String category) {
        return getAllProducts().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<String> getCategories() {
        return getAllProducts().stream()
                .map(Product::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    public Product getProductById(int id) {
        return getAllProductsForAdmin().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public boolean addProduct(Product product) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", product.getName());
            json.put("category", product.getCategory());
            json.put("price", product.getPrice());
            json.put("description", product.getDescription());
            json.put("image", product.getImagePath());
            json.put("detailed", product.getDetailedDescription());
            json.put("active", product.isActive());

            System.out.println("ADD PRODUCT JSON: " + json);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("ADD PRODUCT STATUS: " + response.code());
                System.out.println("ADD PRODUCT BODY: " + responseBody);

                return response.isSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProduct(Product product) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", product.getName());
            json.put("category", product.getCategory());
            json.put("price", product.getPrice());
            json.put("description", product.getDescription());
            json.put("image", product.getImagePath());
            json.put("detailed", product.getDetailedDescription());
            json.put("active", product.isActive());

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL + "?id=eq." + product.getId())
                    .patch(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("UPDATE PRODUCT STATUS: " + response.code());
                System.out.println("UPDATE PRODUCT BODY: " + responseBody);
                return response.isSuccessful();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteProduct(int id) {
        try {
            Request request = new Request.Builder()
                    .url(URL + "?id=eq." + id)
                    .delete()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            client.newCall(request).execute();

            LogService.getInstance().addLog("Admin", "Удаление товара", "Товары", "ID=" + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // СКРЫТЬ / ПОКАЗАТЬф
    // =========================
    public void toggleProduct(int id, boolean active) {
        try {
            JSONObject json = new JSONObject();
            json.put("active", active);

            Request request = new Request.Builder()
                    .url(URL + "?id=eq." + id)
                    .patch(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            client.newCall(request).execute();

            LogService.getInstance().addLog(
                    "Admin",
                    active ? "Показ товара" : "Скрытие товара",
                    "Товары",
                    "ID=" + id
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNextProductId() {
        return getAllProductsForAdmin().stream()
                .mapToInt(Product::getId)
                .max()
                .orElse(0) + 1;
    }
}