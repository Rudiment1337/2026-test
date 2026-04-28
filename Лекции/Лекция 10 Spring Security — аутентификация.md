# Лекция 9: Spring Boot и файлы

## 1. Spring Security — основы аутентификации
**Теоретический материал:**
**Spring Security** — это мощный фреймворк для аутентификации и авторизации в Spring-приложениях.

**Основные понятия:**

- **Аутентификация** — проверка подлинности пользователя (кто ты?)

- **Авторизация** — проверка прав доступа (что тебе разрешено?)

**Базовая настройка SecurityConfig:**
``` java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Настройка доступа к URL
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/public/**", "/login", "/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Кастомная форма логина
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/perform_logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
## 2. UserDetailsService
**Теоретический материал:**
**UserDetailsService** — это интерфейс, который Spring Security использует для загрузки данных пользователя по имени пользователя.

**Реализация UserDetailsService:**
``` java
@Service
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(getAuthorities(user.getRoles()))
            .accountExpired(!user.isAccountNonExpired())
            .accountLocked(!user.isAccountNonLocked())
            .credentialsExpired(!user.isCredentialsNonExpired())
            .disabled(!user.isEnabled())
            .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .collect(Collectors.toList());
    }
}
```
**Сущности User и Role:**
``` java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String email;
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
    
    // Конструкторы, геттеры, сеттеры
}

public enum Role {
    USER, ADMIN, MODERATOR
}
```
## 3. Сессии и Cookies
**Теоретический материал:**
**Сессия** — это способ хранения состояния пользователя на сервере между запросами.

**Cookies** — небольшие фрагменты данных, которые сервер отправляет браузеру для хранения.

**Как работают сессии в Spring Security:**
``` java
@Controller
public class SessionController {
    
    // Получение информации о текущей сессии
    @GetMapping("/session-info")
    public String sessionInfo(HttpSession session, Authentication authentication) {
        System.out.println("ID сессии: " + session.getId());
        System.out.println("Время создания: " + new Date(session.getCreationTime()));
        System.out.println("Последний доступ: " + new Date(session.getLastAccessedTime()));
        System.out.println("Макс. время бездействия: " + session.getMaxInactiveInterval() + " сек");
        
        if (authentication != null && authentication.isAuthenticated()) {
            System.out.println("Текущий пользователь: " + authentication.getName());
            System.out.println("Роли: " + authentication.getAuthorities());
        }
        
        return "session-info";
    }
    
    // Управление сессией
    @PostMapping("/invalidate-session")
    public String invalidateSession(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
```

**Настройка сессий в SecurityConfig:**
``` java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Создавать сессию при необходимости
        .maximumSessions(1) // Максимум 1 сессия на пользователя
        .maxSessionsPreventsLogin(false) // Не блокировать новую авторизацию
        .expiredUrl("/login?expired=true") // URL при истечении сессии
    );
    
    return http.build();
}
```

**Работа с Cookies:**
``` java
@Controller
public class CookieController {
    
    // Установка кастомной cookie
    @GetMapping("/set-cookie")
    public String setCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("user_preference", "dark_theme");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 дней
        cookie.setHttpOnly(true); // Защита от XSS
        cookie.setSecure(true); // Только по HTTPS
        cookie.setPath("/"); // Доступно для всех путей
        response.addCookie(cookie);
        
        return "redirect:/dashboard";
    }
    
    // Чтение cookie
    @GetMapping("/read-cookie")
    public String readCookie(@CookieValue(value = "user_preference", defaultValue = "light") String theme) {
        System.out.println("Тема пользователя: " + theme);
        return "dashboard";
    }
    
    // Удаление cookie
    @GetMapping("/remove-cookie")
    public String removeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("user_preference", null);
        cookie.setMaxAge(0); // Удалить cookie
        cookie.setPath("/");
        response.addCookie(cookie);
        
        return "redirect:/dashboard";
    }
}
```
## 4. Хэширование паролей (bcrypt vs SHA-256)
**Теоретический материал:**
**Хэширование** — это преобразование данных в фиксированную строку, которую нельзя обратно расшифровать.

**Сравнение алгоритмов:**
Критерий|bcrypt|SHA-256
---|---|---
Назначение|Специально для паролей|Общая хэш-функция
Соль (salt)|Автоматическая, встроенная|Требуется ручная реализация
Адаптивность|Можно увеличивать сложность|Фиксированная сложность
Скорость|Замедленная (защита от брутфорса)|Быстрая
Защита от GPU/ASIC|Хорошая|Слабая
**Почему bcrypt лучше для паролей:**
``` java
@Service
public class PasswordService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Хэширование пароля
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    // Проверка пароля
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
    
    // Демонстрация различий
    public void demonstrateHashing() {
        String password = "mySecretPassword";
        
        // bcrypt (рекомендуется)
        String bcryptHash = passwordEncoder.encode(password);
        System.out.println("bcrypt hash: " + bcryptHash);
        // Пример: $2a$10$N9qo8uLOickgx2ZMRZoMye
        
        // SHA-256 (не рекомендуется для паролей)
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String sha256Hash = bytesToHex(hash);
            System.out.println("SHA-256 hash: " + sha256Hash);
            // Пример: 5e884898da28047151d0e56f8dc6292773603d0d6aabbddad...
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
```
**Настройка PasswordEncoder:**
``` java
@Configuration
public class PasswordConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Рекомендуемая сила (cost factor) = 10-12
        return new BCryptPasswordEncoder(12);
    }
    
    // Для миграции со старых хэшей
    @Bean
    public PasswordEncoder delegatingPasswordEncoder() {
        String encodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, new BCryptPasswordEncoder());
        encoders.put("sha256", new MessageDigestPasswordEncoder("SHA-256"));
        
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }
}
```
## 5. CSRF защита
**Теоретический материал:**
**CSRF (Cross-Site Request Forgery)** — атака, когда злоумышленник заставляет браузер пользователя выполнить нежелательное действие на доверенном сайте.

**Почему CSRF токены нужны:**
- **Без токена:** Злоумышленник может создать форму, которая отправится от имени авторизованного пользователя

- **С токеном:** Сервер проверяет уникальный токен, сгенерированный для каждой сессии

**Настройка CSRF защиты:**

``` java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringRequestMatchers("/api/public/**") // Отключить CSRF для API
    );
    
    return http.build();
}
```
**Использование CSRF токенов в формах:**
``` html
<!-- Thymeleaf форма -->
<form th:action="@{/transfer}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
    
    <label>Сумма: <input type="number" name="amount"></label>
    <label>Получатель: <input type="text" name="recipient"></label>
    <button type="submit">Перевести</button>
</form>

<!-- JavaScript для AJAX запросов -->
<script>
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    fetch('/api/transfer', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({ amount: 100, recipient: 'user2' })
    });
</script>
```
**Кастомный CSRF токен репозиторий:**
``` java
@Component
public class CustomCsrfTokenRepository implements CsrfTokenRepository {
    
    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", 
            UUID.randomUUID().toString());
    }
    
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            response.setHeader("X-CSRF-TOKEN", "");
        } else {
            response.setHeader("X-CSRF-TOKEN", token.getToken());
        }
    }
    
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String token = request.getHeader("X-CSRF-TOKEN");
        if (token != null && !token.isEmpty()) {
            return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", token);
        }
        return null;
    }
}
```
## 6. Сессии vs Stateless (JWT)
**Сравнение подходов:**
Критерий|Stateful (Сессии)|Stateless (JWT)
---|---|---
Хранение состояния|На сервере|На клиенте (в токене)
Масштабируемость|Требует sticky sessions или shared storage|Легко масштабируется
Безопасность|Сервер контролирует сессии|Токен может быть украден
Производительность|Сервер проверяет сессию|Сервер проверяет подпись токена
Подход|Традиционный, для монолитов|Современный, для микросервисов

**Stateful аутентификация (сессии):**
``` java
@Configuration
@EnableWebSecurity
public class StatefulSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults());
        
        return http.build();
    }
}
```
**Stateless аутентификация (JWT):**
``` java
@Configuration
@EnableWebSecurity
public class StatelessSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable()); // CSRF не нужен для API
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}
```
**JWT Authentication Filter:**
``` java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJWT(jwt);
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```
## 7. Защита /admin с кастомной формой логина
**Полная реализация защиты админки:**

``` java
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    
    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**") // Применять только к /admin
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/assets/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/perform_login")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/admin/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/admin/access-denied")
            )
            .sessionManagement(session -> session
                .invalidSessionUrl("/admin/login?invalid-session=true")
            );
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
**Кастомная форма логина для админки:**
``` html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Admin Login</title>
    <style>
        .login-container { max-width: 400px; margin: 100px auto; padding: 20px; }
        .error { color: red; margin-bottom: 15px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; }
        input[type="text"], input[type="password"] { width: 100%; padding: 8px; }
        button { width: 100%; padding: 10px; background: #007bff; color: white; border: none; }
    </style>
</head>
<body>
    <div class="login-container">
        <h2>Вход в админ-панель</h2>
        
        <div th:if="${param.error}" class="error">
            Неверное имя пользователя или пароль
        </div>
        
        <div th:if="${param.logout}" class="success">
            Вы успешно вышли из системы
        </div>
        
        <div th:if="${param.invalid-session}" class="error">
            Сессия истекла, войдите снова
        </div>
        
        <form th:action="@{/admin/perform_login}" method="post">
            <div class="form-group">
                <label for="username">Имя пользователя:</label>
                <input type="text" id="username" name="username" required autofocus>
            </div>
            
            <div class="form-group">
                <label for="password">Пароль:</label>
                <input type="password" id="password" name="password" required>
            </div>
            
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
            
            <button type="submit">Войти</button>
        </form>
    </div>
</body>
</html>
```
**Контроллер для админки:**
``` java
@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @GetMapping("/login")
    public String adminLogin() {
        return "admin/login";
    }
    
    @GetMapping("/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        // Добавляем информацию о пользователе в модель
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("authorities", authentication.getAuthorities());
        }
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String manageUsers() {
        return "admin/users";
    }
    
    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String adminSettings() {
        return "admin/settings";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "admin/access-denied";
    }
}
```
**Кастомный Authentication Success Handler:**
``` java
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        // Логирование успешного входа
        System.out.println("Пользователь " + authentication.getName() + " вошел в систему");
        
        // Перенаправление в зависимости от роли
        if (authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            
            setDefaultTargetUrl("/admin/dashboard");
        } else {
            setDefaultTargetUrl("/user/dashboard");
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

**Защита методов с аннотациями:**
``` java
@RestController
@RequestMapping("/admin/api")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {
    
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<User> getUsers() {
        return userService.findAll();
    }
    
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```
**Включение аннотаций безопасности:**
``` java
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {
    // Аннотации @PreAuthorize, @PostAuthorize, @Secured теперь доступны
}
```
Эта реализация обеспечивает полную защиту админ-панели с кастомной формой входа, ролевой моделью и всеми необходимыми механизмами безопасности.