package com.example.socialfeed.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class HostController {

    @GetMapping("/api/public/host")
    public Map<String, String> host() throws UnknownHostException {
        return Map.of("host", InetAddress.getLocalHost().getHostName());
    }
}
