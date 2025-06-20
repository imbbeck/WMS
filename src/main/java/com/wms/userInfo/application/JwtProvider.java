package com.wms.userInfo.application;

import com.wms.userInfo.domain.model.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

	private final Key key;
	private final long accessTokenValidityInMs = 1000 * 60 * 60 * 24; // 1일
	private final long refreshTokenValidityInMs = 1000 * 60 * 60 * 24 * 7; // 7일

	public JwtProvider(@Value("${jwt.secret}") String secretKey) {
		this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	// AccessToken 생성 예
	public String generateAccessToken(UserInfo user) {
		return Jwts.builder()
				.setSubject(user.getUsername()) // username 담기
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + accessTokenValidityInMs))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	// RefreshToken 생성 예
	public String generateRefreshToken(UserInfo user) {
		return Jwts.builder()
				.setSubject(user.getUsername())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidityInMs))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	// 토큰 유효성 검사
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			// 토큰 만료됨
		} catch (UnsupportedJwtException | MalformedJwtException | SecurityException  | IllegalArgumentException e) {
			// 잘못된 토큰
		}
		return false;
	}

	// 토큰에서 username 추출
	public String getUsernameFromToken(String token) {
		Claims claims = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
		return claims.getSubject();
	}
}

