package ru.mityunin.myShop.model;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public record FilterRequest(int page, int size, String textFilter, String sortBy, String sortDirection) {
    public RedirectAttributes addToRedirectAttributes(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("textFilter", textFilter);
        redirectAttributes.addAttribute("sortBy", sortBy);
        redirectAttributes.addAttribute("sortDirection", sortDirection);
        return redirectAttributes;
    }
}
