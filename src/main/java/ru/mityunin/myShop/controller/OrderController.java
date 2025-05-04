package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mityunin.myShop.model.ActionWithProduct;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.service.OrderService;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/change")
    public String changeProductToBasket(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            @ModelAttribute FilterRequest filterRequest,
            RedirectAttributes redirectAttributes) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        filterRequest.addToRedirectAttributes(redirectAttributes);
        return "redirect:/";
    }
}
