package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.service.ProductService;

import java.net.URI;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

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
    public Mono<ServerResponse> crateTestProducts() {
        return productService.createTestProducts().then(ServerResponse.seeOther(URI.create("/")).build());
    }
}
