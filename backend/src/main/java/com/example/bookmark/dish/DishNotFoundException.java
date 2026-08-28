package com.example.bookmark.dish;

public class DishNotFoundException extends RuntimeException {

    public DishNotFoundException(Long id) {
        super("Dish not found: " + id);
    }

    public DishNotFoundException(String message) {
        super(message);
    }
}
