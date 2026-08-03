package com.leadfy.api.service.impl;

import com.leadfy.api.dto.request.LoginRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.dto.response.AuthResponse;
import com.leadfy.api.entity.User;
import com.leadfy.api.exception.EmailAlreadyRegisteredException;
import com.leadfy.api.exception.InvalidCredentialsException;
import com.leadfy.api.repository.UserRepository;
import com.leadfy.api.security.JwtTokenService;
import com.leadfy.api.service.AuthService;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;

	public AuthServiceImpl(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());

		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyRegisteredException(email);
		}

		User user = new User(
				request.name().trim(),
				email,
				passwordEncoder.encode(request.password())
		);

		User savedUser = userRepository.save(user);

		return buildAuthResponse(savedUser);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.password())
			);
		} catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(InvalidCredentialsException::new);

		return buildAuthResponse(user);
	}

	private AuthResponse buildAuthResponse(User user) {
		String token = jwtTokenService.generateToken(user);
		return new AuthResponse(token, TOKEN_TYPE, user.getId(), user.getName(), user.getEmail());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
