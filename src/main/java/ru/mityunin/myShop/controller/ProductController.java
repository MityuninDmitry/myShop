package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mityunin.myShop.service.ProductService;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public String getProduct(@PathVariable Long id, Model model) {

        model.addAttribute("product",productService.getProductBy(id));

        return "Product";
    }

}
