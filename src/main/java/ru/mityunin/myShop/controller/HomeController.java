package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.service.ProductService;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String getProducts(Model model,
                              @ModelAttribute FilterRequest filterRequest
                              ) {
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
