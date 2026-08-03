package com.leadfy.api.security;

import com.leadfy.api.entity.User;
import com.leadfy.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LeadfyUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public LeadfyUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return new AuthenticatedUser(user.getId(), user.getName(), user.getEmail(), user.getPassword());
	}
}
