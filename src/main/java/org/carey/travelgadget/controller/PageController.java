package org.carey.travelgadget.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("activeNav", "home");
        return "index";
    }

    @GetMapping("/trip/{id}")
    public String tripDetail(@PathVariable Long id, Model model) {
        model.addAttribute("activeNav", "home");
        model.addAttribute("tripId", id);
        return "trip-detail";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("activeNav", "settings");
        return "settings";
    }

    @GetMapping("/trips")
    public String trips(Model model) {
        model.addAttribute("activeNav", "trips");
        return "trips";
    }

    @GetMapping("/s/{token}")
    public String sharePage(@PathVariable String token, Model model) {
        model.addAttribute("shareToken", token);
        return "share";
    }
}
