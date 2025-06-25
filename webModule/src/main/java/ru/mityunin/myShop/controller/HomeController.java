package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.service.ProductService;

import java.net.URI;

@Controller
public class HomeController {

    private ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public Mono<Rendering> getProducts(@ModelAttribute FilterRequest filterRequest) {
        Flux<Product> products = productService.findAll(filterRequest);
        Rendering r = Rendering.view("Products")
                        .modelAttribute("products",products)
                        .modelAttribute("filterRequest",filterRequest)
                                .build();
        return Mono.just(r);
    }

    @GetMapping("/createTestProducts")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RedirectView> createTestProducts() {
        return productService.createTestProducts()
                .thenReturn(new RedirectView("/"));
    }
}
