package com.leadfy.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.leadfy.api.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final String secret;
	private final String issuer;
	private final long expirationMinutes;

	public JwtTokenService(
			@Value("${security.jwt.secret}") String secret,
			@Value("${security.jwt.issuer}") String issuer,
			@Value("${security.jwt.expiration-minutes}") long expirationMinutes
	) {
		this.secret = secret;
		this.issuer = issuer;
		this.expirationMinutes = expirationMinutes;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();

		return JWT.create()
				.withIssuer(issuer)
				.withSubject(user.getEmail())
				.withClaim("userId", user.getId())
				.withClaim("name", user.getName())
				.withIssuedAt(now)
				.withExpiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
				.sign(Algorithm.HMAC256(secret));
	}

	public Optional<String> extractSubject(String token) {
		try {
			String subject = JWT.require(Algorithm.HMAC256(secret))
					.withIssuer(issuer)
					.build()
					.verify(token)
					.getSubject();

			return Optional.ofNullable(subject);
		} catch (JWTVerificationException exception) {
			return Optional.empty();
		}
	}
}
