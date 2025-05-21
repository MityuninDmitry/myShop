package ru.mityunin.myShop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.service.OrderService;

@Controller
@RequestMapping("/order")
public class OrderController {

    private OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/basket")
    public Mono<Rendering> getBasket() {
        Rendering r = Rendering.view("Basket")
                .modelAttribute("products", orderService.getBasketProducts())
                .modelAttribute("totalPrice", orderService.getBasketPrice())
                .modelAttribute("order", orderService.getBasket())
                .build();
        return Mono.just(r);
    }
    @GetMapping("/{id}")
    public Mono<Rendering> getOrderInfo(@PathVariable Long id) {
        Rendering r = Rendering.view("Order")
                .modelAttribute("products", orderService.getProductsByOrderId(id))
                .modelAttribute("totalPrice", orderService.getOrderTotalPriceBy(id))
                .build();
        return Mono.just(r);
    }
    @GetMapping("/orders")
    public Mono<Rendering>  getOrders() {
        Flux<Order> orders = orderService.findOrdersBy(OrderStatus.PAID);

        return orderService.getTotalPriceOrders(orders)
                .map(totalPrice -> Rendering.view("orders")
                        .modelAttribute("orders", orders)
                        .modelAttribute("totalPrice", totalPrice)
                        .build()
                );
    }
    @PostMapping("/change")
    public Mono<String> change(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    if (!formData.containsKey("product_id") || !formData.containsKey("actionWithProduct") || !formData.containsKey("source")) {
                        throw new IllegalArgumentException("Empty parameters product_id or actionWithProduct or source");
                    }
                    Long productId = Long.parseLong(formData.getFirst("product_id"));
                    String action = formData.getFirst("actionWithProduct") ;
                    String source = formData.getFirst("source");

                    String sortBy = formData.containsKey("sortBy") ? formData.getFirst("sortBy") : "name";
                    String sortDirection = formData.containsKey("sortDirection") ?  formData.getFirst("sortDirection") : "asc";

                    int page = formData.containsKey("page") ? Integer.parseInt(formData.getFirst("page")) : 1;
                    int size = formData.containsKey("size") ? Integer.parseInt(formData.getFirst("size")) : 10;
                    String textFilter = formData.containsKey("textFilter") ? formData.getFirst("textFilter") : "";

                    return orderService.updateProductInBasketBy(productId, ActionWithProduct.valueOf(action))
                            .thenReturn(buildRedirectUrl(source, productId, page, size, textFilter, sortBy, sortDirection));
                })
                .onErrorResume(e -> {
                    System.err.println("Error processing request: " + e.getMessage());
                    return Mono.just("redirect:/error");
                });
    }

    private String buildRedirectUrl(String source, Long productId, int page, int size, String textFilter, String sortBy, String sortDirection) {
        return switch (source) {
            case "products" -> String.format("redirect:/?page=%s&size=%s&textFilter=%s&sortBy=%s&sortDirection=%s", page, size, textFilter, sortBy, sortDirection);
            case "product" ->  "redirect:/product/" + productId;
            case "basket" -> "redirect:/order/basket";
            default -> "redirect:/";
        };
    }

}
