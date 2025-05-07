package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.service.OrderService;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/basket")
    public String getBasket(Model model) {
        model.addAttribute("products", orderService.getBasketProducts());
        model.addAttribute("totalPrice", orderService.getBasketPrice());
        model.addAttribute("order", orderService.getBasket());
        return "Basket";
    }
    @GetMapping("/{id}")
    public String getOrderInfo(Model model, @PathVariable Long id) {
        model.addAttribute("products", orderService.getProductsByOrderId(id));
        model.addAttribute("totalPrice", orderService.getOrderTotalPriceBy(id));
        return "Order";
    }
    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<Order> orders = orderService.findOrdersBy(OrderStatus.PAID);
        model.addAttribute("orders", orders);
        model.addAttribute("totalPrice", orderService.getTotalPriceOrders(orders));
        return "Orders";
    }
    @PostMapping("/change")
    public String changeProductCountInBasket(@RequestParam Long product_id,
                                             @RequestParam ActionWithProduct actionWithProduct,
                                             @ModelAttribute FilterRequest filterRequest,
                                             @RequestParam String source,
                                             RedirectAttributes redirectAttributes,
                                             Model model) {

        orderService.updateProductInBasketBy(product_id, actionWithProduct);

        return switch (source) {
            case "products" -> {
                filterRequest.addToRedirectAttributes(redirectAttributes);
                yield "redirect:/";
            }
            case "product" -> "redirect:/product/" + product_id;
            case "basket" ->  "redirect:/order/basket";
            default -> "redirect:/";
        };
    }
}
