package com.global.order_api.core.security;

import com.global.order_api.feature.user.entity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// FOR GENERATING TOKEN
/// AND DECODE TOKEN TO GET CLAIMS
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    /// Make Signature
//    public JwtService() throws NoSuchAlgorithmException {
//        KeyGenerator keyGenerator=KeyGenerator.getInstance("HmacSHA256");
//        SecretKey sk=keyGenerator.generateKey();
//        /// key is a bytes we convert it into text base64 to store it into string
//        secretKey= Base64.getEncoder().encodeToString(sk.getEncoded());
//    }
    /// GENERATE ACCESS TOKEN
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpiration);
    }
    /////////////////////
    /// GENERATE REFRESH TOKEN
    public String generateRefreshToken(UserDetails userDetails)
    {
        return buildToken(userDetails, refreshExpiration);
    }
    ///////////////////////
    /// HELPER METHOD FOR BUILDING TOKEN
    private String buildToken(UserDetails userDetails, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        var authorities = userDetails.getAuthorities().iterator();
        if (authorities.hasNext()) {
            claims.put("role", authorities.next().getAuthority());
        }
        if (userDetails instanceof UserPrincipal principal) {
            claims.put("userId", principal.getId());
        }
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .and()
                .signWith(getKey())
                .compact();
    }


    /// because signWith() don't accept a String
    /// want Secret Key Object
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        /// take the fixed key and hash it
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserEmail(String token) {
        /// get subject of from token may be ID or user email or email
        return extractClaim(token, Claims::getSubject);
    }

    // check the expiration
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        /// get expiration date
        /// :: => method reference  /// sugar syntax ///
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic Function take token and function will applied on this token
    // Functional Interface => Function<input,output>   /// Java 8 ///
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        // first get all claims
        final Claims claims = extractAllClaims(token);
        // then apply our function (will we take as a parameter) on claims object
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // take token and get signature using secret key
        // to check if any change happens
        return Jwts.parser().verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /// JWT filter calls this function to validate
    /// first => compare user email from token and from DB
    /// second => check expiration
    public boolean validateToken(String token, UserDetails userDetails) {

        final String userName = extractUserEmail(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /// check expiration of refresh token
    public boolean isRefreshTokenValid(String token) {
        return !isTokenExpired(token);
    }


}
