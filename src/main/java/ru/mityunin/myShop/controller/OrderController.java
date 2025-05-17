package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.service.OrderService;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

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
    public Mono<RedirectView> changeProductCountInBasket(@RequestParam Long product_id,
                                                         @RequestParam ActionWithProduct actionWithProduct,
                                                         @ModelAttribute Mono<FilterRequest> filterRequest,
                                                         @RequestParam String source,
                                                         ServerWebExchange exchange) {

        return orderService.updateProductInBasketBy(product_id, actionWithProduct)
                .then(filterRequest.flatMap(fr -> {
                    RedirectView redirectView = new RedirectView();

                    switch (source) {
                        case "products":
                            // Добавляем параметры запроса для редиректа
                            fr.getParameters().forEach((key, value) ->
                                    exchange.getAttributes().put(key, value));
                            redirectView.setUrl("/");
                            break;
                        case "product":
                            redirectView.setUrl("/product/" + product_id);
                            break;
                        case "basket":
                            redirectView.setUrl("/order/basket");
                            break;
                        default:
                            redirectView.setUrl("/");
                    }

                    return Mono.just(redirectView);
                }));
    }
}
