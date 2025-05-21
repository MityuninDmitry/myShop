package ru.mityunin.myShop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.service.ProductService;

@Controller
@RequestMapping("/product")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Mono<Rendering> getProduct(@PathVariable Long id, Model model) {

        Rendering r = Rendering.view("Product")
                .modelAttribute("product",productService.getProductBy(id))
                .build();
        return Mono.just(r);
    }

}
