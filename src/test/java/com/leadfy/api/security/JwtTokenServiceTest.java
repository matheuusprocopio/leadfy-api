package com.leadfy.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.leadfy.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

	private final JwtTokenService jwtTokenService = new JwtTokenService(
			"leadfy-test-secret",
			"leadfy-api-test",
			120
	);

	@Test
	void generateTokenShouldCreateTokenWithUserEmailAsSubject() {
		User user = new User("Jane Doe", "jane@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", 1L);

		String token = jwtTokenService.generateToken(user);

		assertThat(jwtTokenService.extractSubject(token)).contains("jane@example.com");
	}

	@Test
	void extractSubjectShouldReturnEmptyWhenTokenIsInvalid() {
		assertThat(jwtTokenService.extractSubject("invalid-token")).isEmpty();
	}
}
