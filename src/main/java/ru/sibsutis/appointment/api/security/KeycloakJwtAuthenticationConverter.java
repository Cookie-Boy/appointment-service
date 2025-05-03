package ru.sibsutis.appointment.api.security;

import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

@AllArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final List<String> clientIds;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
        Collection<GrantedAuthority> clientRoles = extractResourceRoles(jwt);

        Collection<GrantedAuthority> authorities = Stream.concat(
                new JwtGrantedAuthoritiesConverter().convert(jwt).stream(),
                Stream.concat(realmRoles.stream(), clientRoles.stream())
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return emptySet();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null) {
            return emptySet();
        }

        List<GrantedAuthority> result = new ArrayList<>();

        for (String clientId : clientIds) {
            if (resourceAccess.containsKey(clientId)) {
                Map<String, Object> client = (Map<String, Object>) resourceAccess.get(clientId);
                List<String> roles = (List<String>) client.get("roles");

                roles.forEach(role ->
                        result.add(new SimpleGrantedAuthority("ROLE_" + clientId + "_" + role)));
            }
        }

        return result;
    }
}
