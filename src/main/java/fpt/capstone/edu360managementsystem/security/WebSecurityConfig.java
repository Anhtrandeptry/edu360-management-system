package fpt.capstone.edu360managementsystem.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fpt.capstone.edu360managementsystem.security.jwt.AuthEntryPointJwt;
import fpt.capstone.edu360managementsystem.security.jwt.AuthTokenFilter;
import fpt.capstone.edu360managementsystem.service.UserDetailsServiceImpl;

@Configuration
//@EnableWebSecurity
@EnableMethodSecurity
//(securedEnabled = true,
//jsr250Enabled = true,
//prePostEnabled = true) // by default
public class WebSecurityConfig { // extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                // Allow Spring Boot error endpoint so 404 won't be masked as 401
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/rooms/**").permitAll()
                .requestMatchers("/api/subjects/**").permitAll()
                .requestMatchers("/api/semesters/**").permitAll()
                .requestMatchers("/api/timeslots/**").permitAll()
                .requestMatchers("/api/classes/**").permitAll()
                // Temporary: allow teacher endpoints until role rules are finalized
                .requestMatchers("/api/teachers/**").permitAll()
                // Allow news endpoints for public access (GET only)
                .requestMatchers("/api/news/**").permitAll()
                // Allow course detail view for all users (students viewing enrolled courses)
                .requestMatchers(HttpMethod.GET, "/api/courses/{id}").permitAll()
                // Allow file upload endpoints
                .requestMatchers("/api/upload/**").permitAll()
                // Allow serving uploaded files
                .requestMatchers("/uploads/**").permitAll()
                // Student profile endpoints (require STUDENT role - handled by @PreAuthorize)
                .requestMatchers("/api/students/profile/**").authenticated()
                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}