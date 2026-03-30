package org.example.hrmsystem.controller;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping("/api/public/status")
    @ResponseBody
    public Map<String, String> home() {
        return publicStatus();
    }

    private Map<String, String> publicStatus() {
        return Map.of(
                "app", "HRM API",
                "status", "running",
                "loginEndpoint", "/api/auth/login"
        );
    }
}
