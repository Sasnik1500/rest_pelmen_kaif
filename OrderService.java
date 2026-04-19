package service.rest21.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import service.rest21.models.CartItem;
import service.rest21.models.Order;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private static OrderService instance;

    private static final String URL = "https://gnfqhtwpcharjvphatcr.supabase.co/rest/v1/orders";
    private static final String API_KEY = "sb_publishable_9GzbmLd1pREmTMpUvI9K3A_fKFrU4T9";

    private final OkHttpClient client = new OkHttpClient();

    private OrderService() {
    }

    public static OrderService getInstance() {
        if (instance == null) {
            instance = new OrderService();
        }
        return instance;
    }

    public boolean createOrder(String username, List<CartItem> items, double total) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("total", total);
            json.put("status", "Принят");

            Request request = new Request.Builder()
                    .url(URL)
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("CREATE ORDER STATUS: " + response.code());
                System.out.println("CREATE ORDER BODY: " + body);

                if (!response.isSuccessful()) {
                    return false;
                }
            }

            LogService.getInstance().addLog(
                    username,
                    "Создание заказа",
                    "Заказы",
                    total + " ₽"
            );

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();

        try {
            Request request = new Request.Builder()
                    .url(URL + "?select=*&order=created_at.desc&limit=100")
                    .get()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("ORDERS RESPONSE: " + body);

                if (!body.trim().startsWith("[")) {
                    System.out.println("Supabase вернул не массив заказов.");
                    return list;
                }

                JSONArray arr = new JSONArray(body);
                return parseOrders(arr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Order> getOrdersByUsername(String username) {
        List<Order> list = new ArrayList<>();

        if (username == null || username.isBlank()) {
            return list;
        }

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(URL + "?username=eq." + encodedUsername + "&select=*&order=created_at.desc")
                    .get()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "[]";
                System.out.println("USER ORDERS STATUS: " + response.code());
                System.out.println("USER ORDERS BODY: " + body);

                if (!response.isSuccessful() || !body.trim().startsWith("[")) {
                    return list;
                }

                JSONArray arr = new JSONArray(body);
                return parseOrders(arr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private List<Order> parseOrders(JSONArray arr) {
        List<Order> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String username = o.optString("username", "Неизвестно");
            Order order = new Order(username);

            order.setId(o.optInt("id", 0));
            order.setStatus(o.optString("status", "Принят"));

            if (o.has("total")) {
                order.setTotalAmount(o.optDouble("total", 0));
            } else if (o.has("total_amount")) {
                order.setTotalAmount(o.optDouble("total_amount", 0));
            }

            String createdAt = o.optString("created_at", "");
            if (!createdAt.isBlank()) {
                try {
                    createdAt = createdAt.replace("Z", "");
                    if (createdAt.contains(".")) {
                        createdAt = createdAt.substring(0, createdAt.indexOf("."));
                    }
                    order.setOrderTime(LocalDateTime.parse(createdAt));
                } catch (Exception ignored) {
                    order.setOrderTime(LocalDateTime.now());
                }
            } else {
                order.setOrderTime(LocalDateTime.now());
            }

            list.add(order);
        }

        return list;
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        try {
            JSONObject json = new JSONObject();
            json.put("status", newStatus);

            Request request = new Request.Builder()
                    .url(URL + "?id=eq." + orderId)
                    .patch(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                System.out.println("UPDATE ORDER STATUS: " + response.code());
                System.out.println("UPDATE ORDER BODY: " + body);
                return response.isSuccessful();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}