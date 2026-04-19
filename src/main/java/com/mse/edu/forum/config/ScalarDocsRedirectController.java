package com.mse.edu.forum.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ScalarDocsRedirectController {

	/** Springdoc Scalar UI is registered at {@code /scalar} by default. */
	@GetMapping("/docs")
	public String scalarDocs() {
		return "redirect:/scalar";
	}
}
