package org.example.ensembled_project;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/api/public/status")
    public Map<String, String> home() {
        return Map.of(
                "app", "HRM API",
                "status", "running",
                "loginEndpoint", "/api/auth/login"
        );
    }
}
