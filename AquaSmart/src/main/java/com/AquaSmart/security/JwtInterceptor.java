package com.AquaSmart.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Permitir peticiones CORS Preflight (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token JWT faltante o formato incorrecto.\"}");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT jwt = jwtUtil.validateToken(token);
            String tokenEmail = jwt.getClaim("email").asString();
            String tokenRole = jwt.getClaim("role").asString();

            // Guardar datos en el request para uso en controladores
            request.setAttribute("tokenEmail", tokenEmail);
            request.setAttribute("tokenRole", tokenRole);

            // Validar protección contra IDOR:
            // Si el rol es DOMESTICO o COMERCIO, no puede consultar datos de otros correos
            String reqEmailParam = request.getParameter("email");
            if (reqEmailParam != null && !reqEmailParam.trim().isBlank()) {
                boolean isDomesticOrComercio = "DOMESTICO".equalsIgnoreCase(tokenRole) || "COMERCIO".equalsIgnoreCase(tokenRole);
                if (isDomesticOrComercio && !reqEmailParam.trim().equalsIgnoreCase(tokenEmail)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"No tienes permiso para consultar datos de otro usuario.\"}");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token JWT inválido o expirado.\"}");
            return false;
        }
    }
}
