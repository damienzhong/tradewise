package com.example.tradewise.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/symbol-config")
    public String symbolConfig() {
        return "symbol-config";
    }
}
