package com.wms.auth.application;

import com.wms.userInfo.domain.model.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

	private static final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30; // 30분
	private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7; // 7일
	private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

	public String generateAccessToken(UserInfo user) {
		return Jwts.builder()
				.setSubject(user.getUsername())
				.claim("userId", user.getUsername())
				.claim("userType", user.getType().name())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
				.signWith(key)
				.compact();
	}

	public String generateRefreshToken(UserInfo user) {
		return Jwts.builder()
				.setSubject(user.getUsername())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
				.signWith(key)
				.compact();
	}

	public Claims validateAndGetClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}

