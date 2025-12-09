//package fpt.capstone.edu360managementsystem.security.jwt;
//
//
//import fpt.capstone.edu360managementsystem.service.UserDetailsServiceImpl;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//public class AuthTokenFilter extends OncePerRequestFilter {
//  @Autowired
//  private JwtUtils jwtUtils;
//
//  @Autowired
//  private UserDetailsServiceImpl userDetailsService;
//
//  private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
//
//  @Override
//  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//      throws ServletException, IOException {
//    try {
//      String jwt = parseJwt(request);
//      if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
//        String username = jwtUtils.getUserNameFromJwtToken(jwt);
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//        UsernamePasswordAuthenticationToken authentication =
//            new UsernamePasswordAuthenticationToken(userDetails,
//                                                    null,
//                                                    userDetails.getAuthorities());
//
//        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//      }
//    } catch (Exception e) {
//      logger.error("Cannot set user authentication: {}", e);
//    }
//
//    filterChain.doFilter(request, response);
//  }
//
//  private String parseJwt(HttpServletRequest request) {
//    String jwt = jwtUtils.getJwtFromCookies(request);
//    logger.info("JWT from request: {}", jwt);
//    return jwt;
//  }
//}


package fpt.capstone.edu360managementsystem.security.jwt;

import fpt.capstone.edu360managementsystem.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    try {
      // Try to get JWT from Authorization header first, then from cookie
      String jwt = getJwtFromRequest(request);
    logger.info("JWT from request: {} | method={} path={} authHeader={}", 
        jwt != null ? "present" : "null",
        request.getMethod(),
        request.getRequestURI(),
        request.getHeader("Authorization") != null ? "present" : "absent");

      if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      logger.error("Cannot set user authentication", e);
    }
    filterChain.doFilter(request, response);
  }
  
  /**
   * Get JWT from Authorization header first, then fallback to cookie
   */
  private String getJwtFromRequest(HttpServletRequest request) {
    // 1. Try Authorization header (Bearer token)
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    
    // 2. Fallback to cookie
    return jwtUtils.getJwtFromCookies(request);
  }
}
