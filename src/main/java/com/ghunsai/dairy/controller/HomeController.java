package com.ghunsai.dairy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/api/home")
    public String home() {
        return "Ghunsai Dairy Management System is running!";
    }
}
