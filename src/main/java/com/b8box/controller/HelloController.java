package com.b8box.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/api/hello")
	public String hello() {
	    System.out.println("✅ API /api/hello foi chamada!");
	    return "B8Box está no ar! 🎵";
	}
}