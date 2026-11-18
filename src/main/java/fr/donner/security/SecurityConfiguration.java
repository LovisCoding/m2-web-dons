package fr.donner.security;

import fr.donner.domain.Person;
import fr.donner.repo.PersonRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PersonRepository people) {
        return username -> {
            Person person = people.findByEmailIgnoreCase(username)
                    .or(() -> people.findByNameIgnoreCase(username))
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));
            String role = (person.getRole() == null || person.getRole().isBlank()) ? "USER" : person.getRole();
            String hash = person.getPasswordHash() == null ? "" : person.getPasswordHash();
            return User.withUsername(person.getEmail())
                    .password(hash)
                    .roles(role)
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher apiOrJsonRequest = this::isApiOrJsonRequest;

        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/", "/style.css", "/login", "/inscription", "/error",
                                "/annonces", "/annonces/{id:[0-9]+}",
                                "/personnes/*/lots",
                                "/api/annonces", "/api/annonces/*",
                                "/api/personnes", "/api/personnes/*",
                                "/api/lots/*", "/api/personnes/*/lots")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/inscription", "/api/personnes")
                        .permitAll()
                        .requestMatchers("/h2-console/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiOrJsonRequest)
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                AnyRequestMatcher.INSTANCE))
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/annonces", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/annonces")
                        .permitAll())
                .build();
    }

    private boolean isApiOrJsonRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && !accept.contains("text/html")
                && (accept.contains("application/json") || accept.contains("application/hal+json"));
    }
}
