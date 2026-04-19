package service.rest21.utils;

import service.rest21.models.CartItem;
import service.rest21.models.Product;
import service.rest21.models.User;
import service.rest21.services.CartService;

import java.util.ArrayList;
import java.util.List;

public class Session {

    private static Session instance;

    private User currentUser;
    private final List<CartItem> cart = new ArrayList<>();
    private boolean guestMode = false;

    private final CartService cartService = CartService.getInstance();

    private Session() {
    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.guestMode = false;
        this.cart.clear();

        if (user != null) {
            System.out.println("LOAD CART FOR: " + user.getUsername());
            this.cart.addAll(cartService.loadCart(user.getUsername()));
        }
    }

    public void loginAsGuest() {
        this.currentUser = null;
        this.guestMode = true;
        this.cart.clear();
    }

    public boolean isGuest() {
        return guestMode;
    }

    public boolean isAuthorized() {
        return currentUser != null && !guestMode;
    }

    public void logout() {
        currentUser = null;
        guestMode = false;
        cart.clear();
    }

    public List<CartItem> getCart() {
        return cart;
    }


    public void addToCart(CartItem item) {
        if (item == null) return;

        for (CartItem c : cart) {
            if (c.getProduct().getId() == item.getProduct().getId()) {
                c.setQuantity(c.getQuantity() + item.getQuantity());

                if (isAuthorized()) {
                    boolean ok = cartService.updateCartItemQuantity(
                            currentUser.getUsername(),
                            c.getProduct(),
                            c.getQuantity()
                    );

                    if (!ok) {
                        cartService.saveCartItem(
                                currentUser.getUsername(),
                                c.getProduct(),
                                c.getQuantity()
                        );
                    }
                }
                return;
            }
        }

        cart.add(item);

        if (isAuthorized()) {
            cartService.saveCartItem(
                    currentUser.getUsername(),
                    item.getProduct(),
                    item.getQuantity()
            );
        }
    }

    public void addToCart(Product product, int quantity) {
        if (product == null || quantity <= 0) return;

        for (CartItem c : cart) {
            if (c.getProduct().getId() == product.getId()) {
                c.setQuantity(c.getQuantity() + quantity);

                if (isAuthorized()) {
                    boolean ok = cartService.updateCartItemQuantity(
                            currentUser.getUsername(),
                            c.getProduct(),
                            c.getQuantity()
                    );

                    if (!ok) {
                        cartService.saveCartItem(
                                currentUser.getUsername(),
                                c.getProduct(),
                                c.getQuantity()
                        );
                    }
                }
                return;
            }
        }

        CartItem item = new CartItem(product, quantity);
        cart.add(item);

        if (isAuthorized()) {
            cartService.saveCartItem(
                    currentUser.getUsername(),
                    product,
                    quantity
            );
        }
    }

    public void removeFromCart(CartItem item) {
        if (item == null) return;

        cart.remove(item);

        if (isAuthorized()) {
            cartService.deleteCartItem(
                    currentUser.getUsername(),
                    item.getProduct()
            );
        }
    }

    public void clearCart() {
        cart.clear();

        if (isAuthorized()) {
            cartService.clearCart(currentUser.getUsername());
        }
    }

    public void updateQuantity(Product product, int quantity) {
        if (product == null) return;

        for (CartItem item : cart) {
            if (item.getProduct().getId() == product.getId()) {

                if (quantity <= 0) {
                    cart.remove(item);

                    if (isAuthorized()) {
                        cartService.deleteCartItem(
                                currentUser.getUsername(),
                                product
                        );
                    }

                } else {
                    item.setQuantity(quantity);

                    if (isAuthorized()) {
                        boolean ok = cartService.updateCartItemQuantity(
                                currentUser.getUsername(),
                                product,
                                quantity
                        );

                        if (!ok) {
                            cartService.saveCartItem(
                                    currentUser.getUsername(),
                                    product,
                                    quantity
                            );
                        }
                    }
                }
                return;
            }
        }
    }


    public int getCartItemsCount() {
        int count = 0;
        for (CartItem item : cart) {
            count += item.getQuantity();
        }
        return count;
    }

    public double getCartTotal() {
        double total = 0;
        for (CartItem item : cart) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }
}