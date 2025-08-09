package com.lytvest.audiotts.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для перенаправления на главную страницу
 */
@Controller
public class HomeController {
    
    /**
     * Редирект с корневого URL на веб-интерфейс
     */
    @GetMapping("/")
    public String redirectToWeb() {
        return "redirect:/web/dashboard";
    }
    
    /**
     * Редирект с /web на дашборд
     */
    @GetMapping("/web")
    public String redirectToDashboard() {
        return "redirect:/web/dashboard";
    }
}