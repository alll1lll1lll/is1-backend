package ru.itmo.is1.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/login", "/register", "/routes", "/routes/{id}", "/locations", "/coordinates", "/special"})
    public String index() {
        return "forward:/index.html";
    }
}
