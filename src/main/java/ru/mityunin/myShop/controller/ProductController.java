package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mityunin.myShop.model.ActionWithProduct;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.ProductService;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public String getProduct(@PathVariable Long id, Model model) {

        model.addAttribute("product",productService.getProductBy(id));

        return "Product";
    }

}
