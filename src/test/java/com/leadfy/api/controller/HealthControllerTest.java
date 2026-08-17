package com.leadfy.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leadfy.api.config.SecurityConfig;
import com.leadfy.api.security.JwtAuthenticationFilter;
import com.leadfy.api.security.JwtTokenService;
import com.leadfy.api.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenService jwtTokenService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void checkShouldAllowUnauthenticatedRequests() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("leadfy-api"))
				.andExpect(jsonPath("$.timestamp").exists());
	}
}
