package com.eventcampus.backend.config;

import com.eventcampus.backend.service.AccountSessionService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class AccountSessionFilter extends OncePerRequestFilter {
    private final AccountSessionService accounts;
    public AccountSessionFilter(AccountSessionService accounts) { this.accounts = accounts; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken) && !accounts.isCurrent(authentication)) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Session expirée ou permissions modifiées. Reconnectez-vous.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
