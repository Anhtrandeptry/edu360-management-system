//package fpt.capstone.edu360managementsystem.security.jwt;
//
//
//import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseCookie;
//import org.springframework.stereotype.Component;
//import org.springframework.web.util.WebUtils;
//
//import java.security.Key;
//import java.util.Date;
//
//@Component
//public class JwtUtils {
//  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
//
//  @Value("${edu360.app.jwtSecret}")
//  private String jwtSecret;
//
//  @Value("${edu360.app.jwtExpirationMs}")
//  private int jwtExpirationMs;
//
//  @Value("${edu360.app.jwtCookieName}")
//  private String jwtCookie;
//
//  public String getJwtFromCookies(HttpServletRequest request) {
//    Cookie cookie = WebUtils.getCookie(request, jwtCookie);
//    if (cookie != null) {
//      return cookie.getValue();
//    } else {
//      return null;
//    }
//  }
//
//  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
//    String jwt = generateTokenFromUsername(userPrincipal.getUsername());
//    ResponseCookie cookie = ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
//    return cookie;
//  }
//
//  public ResponseCookie getCleanJwtCookie() {
//    ResponseCookie cookie = ResponseCookie.from(jwtCookie, null).path("/api").build();
//    return cookie;
//  }
//
//  public String getUserNameFromJwtToken(String token) {
//    return Jwts.parserBuilder().setSigningKey(key()).build()
//        .parseClaimsJws(token).getBody().getSubject();
//  }
//
//  private Key key() {
//    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
//  }
//
//  public boolean validateJwtToken(String authToken) {
//    try {
//      Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
//      return true;
//    } catch (MalformedJwtException e) {
//      logger.error("Invalid JWT token: {}", e.getMessage());
//    } catch (ExpiredJwtException e) {
//      logger.error("JWT token is expired: {}", e.getMessage());
//    } catch (UnsupportedJwtException e) {
//      logger.error("JWT token is unsupported: {}", e.getMessage());
//    } catch (IllegalArgumentException e) {
//      logger.error("JWT claims string is empty: {}", e.getMessage());
//    }
//
//    return false;
//  }
//
//  public String generateTokenFromUsername(String username) {
//    return Jwts.builder()
//              .setSubject(username)
//              .setIssuedAt(new Date())
//              .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
//              .signWith(key(), SignatureAlgorithm.HS256)
//              .compact();
//  }
//}


package fpt.capstone.edu360managementsystem.security.jwt;

import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${edu360.app.jwtSecret}")
  private String jwtSecret;

  @Value("${edu360.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  @Value("${edu360.app.jwtCookieName}")
  private String jwtCookieName;


  public String getJwtFromCookies(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
    return (cookie != null) ? cookie.getValue() : null;
  }


  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
    String jwt = generateTokenFromUsername(userPrincipal.getUsername());
    return ResponseCookie.from(jwtCookieName, jwt)
            .path("/")
            .httpOnly(true)
            // FIXED: 2025-12-22 - Cookie config for production HTTPS deployment
            // Issue: secure(false) prevented cookies from working on HTTPS
            // Solution: secure(true) - cookie only sent over HTTPS
            .secure(true)
            // FIXED: 2025-12-22 - SameSite config for cross-domain requests
            // Issue: sameSite("Lax") blocked cross-domain cookies
            // Solution: sameSite("None") - allows cross-domain (requires secure=true)
            .sameSite("None")
            .maxAge(24 * 60 * 60)
            .build();
  }


  public ResponseCookie getCleanJwtCookie() {
    return ResponseCookie.from(jwtCookieName, "")
            .path("/")
            .httpOnly(true)
            // FIXED: 2025-12-22 - Must match generateJwtCookie() config
            // secure(true) required for HTTPS production
            .secure(true)
            // sameSite("None") required for cross-domain logout
            .sameSite("None")
            .maxAge(0)
            .build();
  }


  public String generateTokenFromUsername(String username) {
    return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
  }


  public String getUserNameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build()
            .parseClaimsJws(token).getBody().getSubject();
  }

  private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }


  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }
}