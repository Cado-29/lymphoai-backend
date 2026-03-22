package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoaderIoController {

    @GetMapping("/loaderio-c098800bc5bbbad2b0425ed388101fde.txt")
    public String loaderIoVerification() {
        return "loaderio-c098800bc5bbbad2b0425ed388101fde";
    }
}