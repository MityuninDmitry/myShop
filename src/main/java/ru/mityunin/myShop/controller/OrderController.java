package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mityunin.myShop.model.ActionWithProduct;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.ProductService;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;

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
    public String changeProductInBasketFromProducts(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            @ModelAttribute FilterRequest filterRequest,
            RedirectAttributes redirectAttributes) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        filterRequest.addToRedirectAttributes(redirectAttributes);
        return "redirect:/";
    }

    @PostMapping("/shortChange")
    public String changeProductInBasketFromProduct(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            Model model) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        model.addAttribute("product",productService.getProductBy(product_id));
        return "redirect:/products/" + product_id;
    }

    @PostMapping("/basketChange")
    public String changeProductInBasketFromBasket(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            Model model) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        model.addAttribute("products", orderService.getBasketProducts());
        model.addAttribute("totalPrice", orderService.getBasketPrice());
        return "redirect:/order/basket";

    }
}
