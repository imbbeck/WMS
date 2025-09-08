package com.wms.applicationInfra.config;

import com.wms.userInfo.application.JwtProvider;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserInfoRepository userInfoRepository;
    
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String jwt = extractToken(request);

			if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
				String username = jwtProvider.getUsernameFromToken(jwt);

				UserInfo userInfo = userInfoRepository.findByUsername(username).orElse(null);
				if (userInfo != null) {
					// 사용자 권한 설정
					Collection<GrantedAuthority> authorities = getUserAuthorities(userInfo);

					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(userInfo, null, authorities);
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);

					log.debug("인증 성공: {} (권한: {})", username, authorities);
				}
			}
		} catch (Exception ex) {
			log.error("JWT 인증 처리 중 오류 발생", ex);
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * 사용자 권한을 GrantedAuthority 컬렉션으로 변환
	 */
	private Collection<GrantedAuthority> getUserAuthorities(UserInfo userInfo) {
		// UserInfo에서 역할 정보를 가져와 Spring Security 권한으로 변환

		// 예시 3: UserInfo에 UserRole enum이 있는 경우
        if (userInfo.getType() != null) {
            String role = "ROLE_" + userInfo.getType().name();
            return Collections.singletonList(new SimpleGrantedAuthority(role));
        }

		// 기본값: 빈 권한 리스트
		return Collections.emptyList();
	}

    /**
     * 헤더 또는 쿠키에서 JWT 토큰 추출
     * 1. Authorization 헤더에서 Bearer 토큰 확인
     * 2. 없으면 쿠키에서 accessToken 확인
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        // 2. 쿠키에서 토큰 추출
        return getTokenFromCookie(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * 쿠키에서 토큰 추출
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
