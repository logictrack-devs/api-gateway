package com.example.gatewayserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Seguridad del API Gateway: valida el JWT con la clave pública y corta temprano
 * los pedidos sin token o sin el rol necesario. Los servicios de negocio siguen
 * validando el token por su cuenta.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("classpath:certs/public.pem")
    private Resource publicKeyResource;

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/applications/apisecurity/api/auth/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/gateway/routes").permitAll()
                        .pathMatchers("/applications/apisecurity/api/admin/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/applications/apicourier/**").hasAnyRole("ADMIN", "REPARTIDOR")
                        .pathMatchers("/applications/apicourier/**").hasRole("ADMIN")
                        .pathMatchers("/applications/apishipment/**").hasAnyRole("CLIENTE", "REPARTIDOR", "ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    /**
     * Toma los roles del claim "roles" del token. Security ya los emite con el
     * prefijo (ROLE_ADMIN, ROLE_CLIENTE...), por eso el prefijo acá va vacío.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");

        var conv= new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(conv);
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() throws Exception {
        String key = new String(publicKeyResource.getInputStream().readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        RSAPublicKey pub = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));

        return NimbusReactiveJwtDecoder.withPublicKey(pub).build();
    }
}
