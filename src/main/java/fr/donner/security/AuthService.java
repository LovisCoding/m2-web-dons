package fr.donner.security;

import fr.donner.domain.Person;
import fr.donner.repo.PersonRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final PersonRepository people;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PersonRepository people, PasswordEncoder passwordEncoder) {
        this.people = people;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Person> currentPerson(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String principal = authentication.getName();
        return people.findByEmailIgnoreCase(principal)
                .or(() -> people.findByNameIgnoreCase(principal));
    }

    public Person requirePerson(Authentication authentication) {
        return currentPerson(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise"));
    }

    public Person requireSamePerson(Authentication authentication, Long targetPersonId) {
        Person connected = requirePerson(authentication);
        if (targetPersonId != null && !connected.getId().equals(targetPersonId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé pour ce profil");
        }
        return connected;
    }

    public Person register(String name, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (people.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet e-mail");
        }
        String effectivePassword = (rawPassword == null || rawPassword.isBlank()) ? "password" : rawPassword;
        Person person = new Person(name.trim(), normalizedEmail, passwordEncoder.encode(effectivePassword));
        return people.save(person);
    }
}
