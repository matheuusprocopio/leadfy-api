package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.dto.request.LoginRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.dto.response.AuthResponse;
import com.leadfy.api.entity.User;
import com.leadfy.api.exception.EmailAlreadyRegisteredException;
import com.leadfy.api.exception.InvalidCredentialsException;
import com.leadfy.api.repository.UserRepository;
import com.leadfy.api.security.JwtTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenService jwtTokenService;

	@InjectMocks
	private AuthServiceImpl authService;

	@Test
	void registerShouldCreateUserWithEncodedPasswordAndReturnToken() {
		RegisterRequest request = new RegisterRequest(" Jane Doe ", "JANE@EXAMPLE.COM", "password123");

		when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", 1L);
			return user;
		});
		when(jwtTokenService.generateToken(any(User.class))).thenReturn("jwt-token");

		AuthResponse response = authService.register(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Jane Doe");
		assertThat(response.email()).isEqualTo("jane@example.com");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getName()).isEqualTo("Jane Doe");
		assertThat(savedUser.getEmail()).isEqualTo("jane@example.com");
		assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
	}

	@Test
	void registerShouldRejectDuplicatedEmail() {
		RegisterRequest request = new RegisterRequest("Jane Doe", "JANE@EXAMPLE.COM", "password123");

		when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(EmailAlreadyRegisteredException.class)
				.hasMessageContaining("jane@example.com");

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void loginShouldAuthenticateUserAndReturnToken() {
		LoginRequest request = new LoginRequest("JANE@EXAMPLE.COM", "password123");
		User user = new User("Jane Doe", "jane@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", 1L);

		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
		when(jwtTokenService.generateToken(user)).thenReturn("jwt-token");

		AuthResponse response = authService.login(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("jane@example.com");

		ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
				ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(authenticationCaptor.capture());

		UsernamePasswordAuthenticationToken authentication = authenticationCaptor.getValue();
		assertThat(authentication.getName()).isEqualTo("jane@example.com");
		assertThat(authentication.getCredentials()).isEqualTo("password123");
	}

	@Test
	void loginShouldRejectInvalidCredentials() {
		LoginRequest request = new LoginRequest("jane@example.com", "wrong-password");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(InvalidCredentialsException.class);

		verify(userRepository, never()).findByEmail("jane@example.com");
	}
}
