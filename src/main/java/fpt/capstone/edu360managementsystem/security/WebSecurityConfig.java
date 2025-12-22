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
                .cors(cors -> {
                })
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                // Allow Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // Allow Spring Boot error endpoint so 404 won't be masked as 401
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/rooms/**").permitAll()
                .requestMatchers("/api/subjects/**").permitAll()
                .requestMatchers("/api/semesters/**").permitAll()
                .requestMatchers("/api/timeslots/**").permitAll()
                // Classes: only allow GET for public viewing, other methods require auth
                .requestMatchers(HttpMethod.GET, "/api/classes/**").permitAll()
                .requestMatchers("/api/classes/**").authenticated()
                // Allow search endpoints for public access
                .requestMatchers("/api/search/**").permitAll()
                // Teachers: only allow GET for public viewing (list, detail, paginated), other methods require auth
                .requestMatchers(HttpMethod.GET, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/teachers/paginated").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/teachers/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/teachers/{id}/free-busy").permitAll()
                .requestMatchers("/api/teachers/**").authenticated()
                // News: only allow GET for public access, POST/PUT/DELETE require auth
                .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                .requestMatchers("/api/news/**").authenticated()
                // Allow course detail view for all users (students viewing enrolled courses)
                .requestMatchers(HttpMethod.GET, "/api/courses/{id}").permitAll()
                // File upload endpoints require authentication (prevent DoS/abuse)
                .requestMatchers("/api/upload/**").authenticated()
                // Allow serving uploaded files (read-only)
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // Allow payment webhooks (Casso, VietQR, PayOS) - verified by secret key
                .requestMatchers("/api/payments/casso/webhook").permitAll()
                .requestMatchers("/api/payments/vietqr/callback").permitAll()
                .requestMatchers("/api/payments/payos/webhook").permitAll()
                // Student profile endpoints (require STUDENT role - handled by @PreAuthorize)
                .requestMatchers("/api/students/profile/**").authenticated()
                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
