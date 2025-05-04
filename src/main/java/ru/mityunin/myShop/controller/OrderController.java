package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mityunin.myShop.model.ActionWithProduct;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.ProductService;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;

    @PostMapping("/change")
    public String changeProductInBasketFull(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            @ModelAttribute FilterRequest filterRequest,
            RedirectAttributes redirectAttributes) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        filterRequest.addToRedirectAttributes(redirectAttributes);
        return "redirect:/";
    }

    @PostMapping("/shortChange")
    public String changeProductInBasketShort(
            @RequestParam Long product_id,
            @RequestParam ActionWithProduct actionWithProduct,
            Model model) {
        orderService.updateProductInBasketBy(product_id, actionWithProduct);
        model.addAttribute("product",productService.getProductBy(product_id));
        return "redirect:/products/" + product_id;
    }
}
