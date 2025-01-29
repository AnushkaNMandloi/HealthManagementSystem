package com.edu.neu.INFO7255Demo2.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.edu.neu.INFO7255Demo2.filters.JwtFilter;
import com.edu.neu.INFO7255Demo2.service.MyUserDetailsService;
import com.edu.neu.INFO7255Demo2.exception.JwtAuthenticationEntryPoint;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
	@Autowired
	private RedisConfig redisConfig;
	
	
	@Autowired
	private MyUserDetailsService userDetails;
	
	@Autowired
	private JwtFilter jwtRequestFilter;
	
	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	
	
	@Bean
	public PasswordEncoder passwordEncoder(){
		return NoOpPasswordEncoder.getInstance();
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()
		// dont authenticate this particular request
		.authorizeRequests().antMatchers("/authenticate").permitAll().antMatchers(HttpMethod.OPTIONS, "/**")
		.permitAll().
		// all other requests need to be authenticated
				anyRequest().authenticated().and()
				.exceptionHandling()
	            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
	            .and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//				.and().
//		// make sure we use stateless session; session won't be used to
//		// store user's state.
//		exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
//		.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
				
		// Adding JWTRequest Filter
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
	}
	

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception  {
		return super.authenticationManagerBean();
	}
	

}
