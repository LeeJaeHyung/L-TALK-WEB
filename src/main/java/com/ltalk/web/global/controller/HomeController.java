package com.ltalk.web.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping
    public String  home(){
        return "home";
    }

    @GetMapping("/home")
    public String home2(){
        return "home";
    }

    @GetMapping("/chatrooms/view")
    public String chatrooms(){return "chatrooms";}

    @GetMapping("/chatroomtest")
    public String chatroomTest(){
        return "chatroomtest";
    }
}
