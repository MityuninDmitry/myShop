package ru.mityunin.myShop.controller.DTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class FilterRequest {
    private Integer page;
    private Integer size;
    private String textFilter;
    private String sortBy;
    private String sortDirection;

    public FilterRequest() {
        page = 1;
        size = 10;
        textFilter = "";
        sortBy = "name";
        sortDirection = "asc";
    }

    public FilterRequest(Integer page, Integer size, String textFilter, String sortBy, String sortDirection) {
        this.page = page;
        this.size = size;
        this.textFilter = textFilter;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    @Override
    public String toString() {
        return "page: " + page + ", size: " + size + ", textFilter: " + textFilter + ", sortBy: " + sortBy + ", sortDirection: " + sortDirection;
    }

    public Integer page() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer size() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String textFilter() {
        return textFilter;
    }

    public void setTextFilter(String textFilter) {
        this.textFilter = textFilter;
    }

    public String sortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String sortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
