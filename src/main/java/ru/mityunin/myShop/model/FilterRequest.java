package ru.mityunin.myShop.model;

public record FilterRequest(int page, int size, String textFilter, String sortBy, String sortDirection) {
}
