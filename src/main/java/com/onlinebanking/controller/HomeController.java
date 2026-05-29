package com.onlinebanking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = {"/", "/dashboard", "/accounts", "/transactions", "/transfer"})
    public String index() {
        return "forward:/index.html";
    }
}
