package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.service.ProductService;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String getProducts(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "") String textFilter,
                              @RequestParam(defaultValue = "name") String sortBy,
                              @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        FilterRequest filterRequest = new FilterRequest(page, size, textFilter, sortBy, sortDirection);

        model.addAttribute("products", productService.findAll(filterRequest));
        model.addAttribute("filterRequest", filterRequest);
        return "Products";
    }

    @GetMapping("/createTestProducts")
    public String crateTestProducts() {
        productService.createTestProducts();
        return "redirect:/";
    }
}
