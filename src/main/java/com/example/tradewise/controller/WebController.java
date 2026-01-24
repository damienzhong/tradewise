package com.example.tradewise.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/traders")
    public String traders() {
        return "traders";
    }

    @GetMapping("/emails")
    public String emails() {
        return "emails";
    }

    @GetMapping("/features")
    public String features() {
        return "features";
    }

    @GetMapping("/system")
    public String system() {
        return "system";
    }
}
