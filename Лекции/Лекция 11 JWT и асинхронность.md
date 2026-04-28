# Лекция 10: JWT и асинхронность

## Генерация JWT и хранение в cookies
1. Генерация JWT токенов
**Теоретический материал:**
**JWT (JSON Web Token)** — это стандарт для создания токенов доступа, основанный на JSON. Состоит из трех частей:

- **Header** — алгоритм и тип токена
- **Payload** — данные (claims)
- **Signature** — подпись для проверки подлинности

**Зависимости Maven/Gradle:**
``` xml
<!-- Maven -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**Класс для работы с JWT:**
``` java
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24 часа по умолчанию
    private long jwtExpirationMs;
    
    @Value("${app.jwt.refresh-expiration:604800000}") // 7 дней
    private long refreshTokenExpirationMs;
    
    // Генерация access token
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .claim("type", "ACCESS")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    // Генерация refresh token
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "REFRESH")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    // Получение имени пользователя из токена
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }
    
    // Получение ролей из токена
    public List<String> getRolesFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        return roles != null ? roles : new ArrayList<>();
    }
    
    // Валидация токена
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
    
    // Получение оставшегося времени жизни токена
    public long getRemainingExpirationTime(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
    
    // Обновление access token с помощью refresh token
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new JwtException("Invalid refresh token");
        }
        
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(refreshToken)
            .getBody();
        
        String tokenType = claims.get("type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new JwtException("Not a refresh token");
        }
        
        String username = claims.getSubject();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        // Здесь можно загрузить актуальные роли из базы
        UserDetails userDetails = ... // Загрузка пользователя
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .claim("type", "ACCESS")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

2. Хранение JWT в cookies
**Теоретический материал:**
Почему cookies лучше для JWT чем LocalStorage:

- **HttpOnly** флаг — защита от XSS атак    
- **Secure** флаг — передача только по HTTPS
- **SameSite** политика — защита от CSRF
- **Автоматическая** отправка браузером

**Настройка cookies для JWT:**
``` java
@Component
public class JwtCookieManager {
    
    @Value("${app.jwt.access-token-name:accessToken}")
    private String accessTokenCookieName;
    
    @Value("${app.jwt.refresh-token-name:refreshToken}")
    private String refreshTokenCookieName;
    
    @Value("${app.jwt.domain:localhost}")
    private String domain;
    
    // Создание cookie для access token
    public void createAccessTokenCookie(HttpServletResponse response, String token, long maxAge) {
        Cookie cookie = new Cookie(accessTokenCookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Только для HTTPS в production
        cookie.setPath("/");
        cookie.setMaxAge((int) (maxAge / 1000)); // Конвертация в секунды
        cookie.setDomain(domain);
        
        // SameSite protection
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; HttpOnly; Secure; Path=/; Max-Age=%d; SameSite=Strict", 
                accessTokenCookieName, token, (int) (maxAge / 1000)));
    }
    
    // Создание cookie для refresh token
    public void createRefreshTokenCookie(HttpServletResponse response, String token, long maxAge) {
        Cookie cookie = new Cookie(refreshTokenCookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh"); // Ограниченный путь
        cookie.setMaxAge((int) (maxAge / 1000));
        cookie.setDomain(domain);
        
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; HttpOnly; Secure; Path=/api/auth/refresh; Max-Age=%d; SameSite=Strict", 
                refreshTokenCookieName, token, (int) (maxAge / 1000)));
    }
    
    // Получение токена из cookie
    public String getTokenFromCookie(HttpServletRequest request, String tokenName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    // Удаление cookies (логаут)
    public void deleteAuthCookies(HttpServletResponse response) {
        // Удаление access token cookie
        Cookie accessTokenCookie = new Cookie(accessTokenCookieName, "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);
        
        // Удаление refresh token cookie
        Cookie refreshTokenCookie = new Cookie(refreshTokenCookieName, "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
        
        // Дополнительно через заголовки
        response.addHeader("Set-Cookie", 
            String.format("%s=; HttpOnly; Secure; Path=/; Max-Age=0; SameSite=Strict", 
                accessTokenCookieName));
        response.addHeader("Set-Cookie", 
            String.format("%s=; HttpOnly; Secure; Path=/api/auth/refresh; Max-Age=0; SameSite=Strict", 
                refreshTokenCookieName));
    }
    
    // Получение access token из cookie
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getTokenFromCookie(request, accessTokenCookieName);
    }
    
    // Получение refresh token из cookie
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return getTokenFromCookie(request, refreshTokenCookieName);
    }
}
```
3. JWT Authentication Filter
``` java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private JwtCookieManager cookieManager;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Пытаемся получить токен из cookie
            String jwt = cookieManager.getAccessTokenFromCookie(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJWT(jwt);
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Проверяем, не изменились ли роли пользователя
                if (!hasSameRoles(jwt, userDetails)) {
                    // Роли изменились - генерируем новый токен
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    String newToken = tokenProvider.generateAccessToken(authentication);
                    
                    // Обновляем cookie
                    cookieManager.createAccessTokenCookie(response, newToken, 
                        tokenProvider.getJwtExpirationMs());
                    
                    jwt = newToken;
                }
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            
            // Если токен невалидный - удаляем cookie
            if (ex instanceof ExpiredJwtException || ex instanceof MalformedJwtException) {
                cookieManager.deleteAuthCookies(response);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean hasSameRoles(String jwt, UserDetails userDetails) {
        List<String> tokenRoles = tokenProvider.getRolesFromJWT(jwt);
        List<String> currentRoles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        
        return new HashSet<>(tokenRoles).equals(new HashSet<>(currentRoles));
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Не фильтруем публичные endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/public/");
    }
}
```

4. Auth Controller для работы с JWT и cookies

``` java
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private JwtCookieManager cookieManager;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                            HttpServletResponse response) {
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Генерация токенов
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);
            
            // Установка cookies
            cookieManager.createAccessTokenCookie(response, accessToken, 
                tokenProvider.getJwtExpirationMs());
            cookieManager.createRefreshTokenCookie(response, refreshToken, 
                tokenProvider.getRefreshTokenExpirationMs());
            
            // Получение информации о пользователе
            User user = userService.findByUsername(loginRequest.getUsername());
            
            return ResponseEntity.ok(new JwtResponse(
                true,
                "Login successful",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList())
            ));
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(false, "Invalid username or password"));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                        HttpServletResponse response) {
        
        try {
            String refreshToken = cookieManager.getRefreshTokenFromCookie(request);
            
            if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(false, "Invalid refresh token"));
            }
            
            // Генерация нового access token
            String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
            
            // Обновление cookie
            cookieManager.createAccessTokenCookie(response, newAccessToken, 
                tokenProvider.getJwtExpirationMs());
            
            return ResponseEntity.ok(new MessageResponse(true, "Token refreshed successfully"));
            
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            cookieManager.deleteAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(false, "Token refresh failed"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request,
                                      HttpServletResponse response) {
        
        // Очищаем Security Context
        SecurityContextHolder.clearContext();
        
        // Удаляем cookies
        cookieManager.deleteAuthCookies(response);
        
        return ResponseEntity.ok(new MessageResponse(true, "Logout successful"));
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(false, "Not authenticated"));
        }
        
        User user = userService.findByUsername(authentication.getName());
        
        return ResponseEntity.ok(new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        ));
    }
    
    // Проверка валидности токена
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String token = cookieManager.getAccessTokenFromCookie(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            return ResponseEntity.ok(new MessageResponse(true, "Token is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(false, "Token is invalid"));
        }
    }
}

```
5. DTO классы

``` java
// Запрос на логин
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    // геттеры и сеттеры
}

// Ответ с JWT
public class JwtResponse {
    private boolean success;
    private String message;
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    
    // конструкторы, геттеры, сеттеры
}

// Профиль пользователя
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    
    // конструкторы, геттеры, сеттеры
}

// Базовый ответ
public class MessageResponse {
    private boolean success;
    private String message;
    
    // конструкторы, геттеры, сеттеры
}
```
6. Security Configuration для JWT

``` java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Отключаем CSRF для API
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                .accessDeniedHandler(jwtAccessDeniedHandler())
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
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "https://*.mydomain.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "Authentication failed");
            body.put("error", authException.getMessage());
            body.put("path", request.getServletPath());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), body);
        };
    }
    
    @Bean
    public AccessDeniedHandler jwtAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "Access denied");
            body.put("error", accessDeniedException.getMessage());
            body.put("path", request.getServletPath());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), body);
        };
    }
}
```
7. Настройки application.properties

``` properties
# JWT Configuration
app.jwt.secret=mySuperSecretKeyThatIsAtLeast512BitsLongForHS512Algorithm
app.jwt.expiration=86400000 # 24 hours in milliseconds
app.jwt.refresh-expiration=604800000 # 7 days in milliseconds
app.jwt.access-token-name=auth_token
app.jwt.refresh-token-name=refresh_token
app.jwt.domain=localhost

# CORS for frontend
app.cors.allowed-origins=http://localhost:3000,https://mydomain.com

# Security
spring.security.oauth2.resourceserver.jwt.secret-value=${app.jwt.secret}
```

**Inter-process Communication (IPC) - Синхронные vs Асинхронные вызовы**
1. Теоретический материал
**Что такое Inter-process Communication (IPC)?**
**IPC** — это механизмы, которые позволяют процессам обмениваться данными и координационно работать. Процессы могут быть на одном компьютере или распределены по сети.

**Классификация IPC:**

Тип|Синхронный|Асинхронный
---|---|---
Блокирующий|Блокирует поток до получения ответа|Не блокирует основной поток
Время ответа|Предсказуемое|Непредсказуемое
Сложность|Проще в реализации|Сложнее, требует обработки колбэков
Производительность|Меньшая пропускная способность|Высокая пропускная способность

2. Синхронные вызовы
**Теоретический материал:**
Синхронные вызовы — блокируют выполнение потока до получения ответа. Клиент ждет, пока сервер обработает запрос и вернет результат.

**Характеристики:**
- ✅ Простота реализации и понимания
- ✅ Предсказуемое поведение
- ✅ Последовательное выполнение
- ❌ Блокировка потоков
- ❌ Низкая производительность при высокой нагрузке
- ❌ Плохая масштабируемость

**Примеры синхронной коммуникации:**
1. HTTP/REST API (Spring Boot)
``` java
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private InventoryService inventoryService;
    
    // СИНХРОННЫЙ вызов - блокирующий
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        log.info("Создание заказа для пользователя: {}", request.getUserId());
        
        // 1. Синхронная проверка inventory
        InventoryResponse inventoryResponse = inventoryService.checkAvailability(
            request.getProductId(), 
            request.getQuantity()
        );
        
        if (!inventoryResponse.isAvailable()) {
            return ResponseEntity.badRequest().body(
                new OrderResponse(false, "Товар недоступен")
            );
        }
        
        // 2. Синхронная обработка платежа
        PaymentResponse paymentResponse = paymentService.processPayment(
            request.getPaymentDetails(), 
            request.getAmount()
        );
        
        if (!paymentResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(
                new OrderResponse(false, "Ошибка оплаты: " + paymentResponse.getMessage())
            );
        }
        
        // 3. Синхронное создание заказа
        Order order = orderService.createOrder(request);
        
        log.info("Заказ успешно создан: {}", order.getId());
        return ResponseEntity.ok(new OrderResponse(true, "Заказ создан", order.getId()));
    }
}

// Сервис для работы с инвентарем
@Service
public class InventoryService {
    
    // Синхронный REST вызов используя RestTemplate
    public InventoryResponse checkAvailability(String productId, int quantity) {
        RestTemplate restTemplate = new RestTemplate();
        
        String url = "http://inventory-service/api/inventory/check";
        InventoryCheckRequest checkRequest = new InventoryCheckRequest(productId, quantity);
        
        // БЛОКИРУЮЩИЙ ВЫЗОВ - поток ждет ответа
        ResponseEntity<InventoryResponse> response = restTemplate.postForEntity(
            url, checkRequest, InventoryResponse.class);
        
        return response.getBody();
    }
    
    // Синхронный вызов используя WebClient (может быть асинхронным)
    public InventoryResponse checkAvailabilityWebClient(String productId, int quantity) {
        WebClient webClient = WebClient.create("http://inventory-service");
        
        // Даже с WebClient мы делаем синхронный блокирующий вызов
        return webClient.post()
            .uri("/api/inventory/check")
            .bodyValue(new InventoryCheckRequest(productId, quantity))
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .block(); // БЛОКИРОВКА потока!
    }
}

// DTO классы
public class OrderRequest {
    private String userId;
    private String productId;
    private int quantity;
    private BigDecimal amount;
    private PaymentDetails paymentDetails;
    // getters/setters
}

public class InventoryResponse {
    private boolean available;
    private String message;
    // getters/setters
}

public class PaymentResponse {
    private boolean success;
    private String message;
    private String transactionId;
    // getters/setters
}
```
2. Синхронный gRPC

``` java
// proto файл (order.proto)
syntax = "proto3";

service OrderService {
  rpc CreateOrder(CreateOrderRequest) returns (CreateOrderResponse);
}

service InventoryService {
  rpc CheckAvailability(InventoryRequest) returns (InventoryResponse);
}

message CreateOrderRequest {
  string user_id = 1;
  string product_id = 2;
  int32 quantity = 3;
  double amount = 4;
}

message InventoryRequest {
  string product_id = 1;
  int32 quantity = 2;
}

message InventoryResponse {
  bool available = 1;
  string message = 2;
}

// Java реализация
@Service
public class SynchronousOrderService {
    
    @Autowired
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
    
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // СИНХРОННЫЙ gRPC вызов
        InventoryResponse inventoryResponse = inventoryStub.checkAvailability(
            InventoryRequest.newBuilder()
                .setProductId(request.getProductId())
                .setQuantity(request.getQuantity())
                .build()
        );
        
        if (!inventoryResponse.getAvailable()) {
            return CreateOrderResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Товар недоступен")
                .build();
        }
        
        // Дополнительные синхронные вызовы...
        return CreateOrderResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Заказ создан")
            .build();
    }
}
```

3. Синхронная база данных

``` java
@Service
@Transactional // Синхронная транзакция
public class SynchronousDatabaseService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    public Order processUserOrder(OrderRequest request) {
        // Синхронные операции с БД
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException());
        
        // БЛОКИРУЮЩИЕ операции
        Order order = new Order(user, request.getProductId(), request.getQuantity());
        orderRepository.save(order);
        
        // Синхронное обновление инвентаря
        updateInventory(request.getProductId(), request.getQuantity());
        
        return order;
    }
    
    private void updateInventory(String productId, int quantity) {
        // Синхронный вызов другого сервиса
        inventoryService.decreaseQuantity(productId, quantity);
    }
}
```

3. Асинхронные вызовы
**Теоретический материал:**
Асинхронные вызовы — не блокируют выполнение потока. Клиент отправляет запрос и продолжает работу, получая ответ через колбэк или будущее (Future).

**Характеристики:**
- ✅ Высокая производительность
- ✅ Лучшая масштабируемость
- ✅ Не блокирует потоки
- ❌ Сложность реализации
- ❌ Сложнее отладка
- ❌ Требует обработки ошибок

**Примеры асинхронной коммуникации:**
1. Асинхронный REST с CompletableFuture
``` java 
@Service
@Slf4j
public class AsyncOrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    
    // АСИНХРОННАЯ обработка заказа
    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request) {
        log.info("Асинхронное создание заказа для: {}", request.getUserId());
        
        return CompletableFuture
            // 1. Проверка доступности товара
            .supplyAsync(() -> inventoryService.checkAvailabilityAsync(
                request.getProductId(), request.getQuantity()), asyncExecutor)
            
            // 2. Затем обработка платежа
            .thenCompose(inventoryResponse -> {
                if (!inventoryResponse.isAvailable()) {
                    throw new InventoryException("Товар недоступен");
                }
                return paymentService.processPaymentAsync(
                    request.getPaymentDetails(), request.getAmount());
            })
            
            // 3. Затем создание заказа
            .thenCompose(paymentResponse -> {
                if (!paymentResponse.isSuccess()) {
                    throw new PaymentException("Ошибка оплаты: " + paymentResponse.getMessage());
                }
                return orderService.createOrderAsync(request);
            })
            
            // 4. Параллельно отправка уведомления (не ждем завершения)
            .thenApply(order -> {
                notificationService.sendOrderConfirmationAsync(order.getId());
                return new OrderResponse(true, "Заказ создан", order.getId());
            })
            
            // Обработка исключений
            .exceptionally(throwable -> {
                log.error("Ошибка при создании заказа: {}", throwable.getMessage());
                return new OrderResponse(false, "Ошибка: " + throwable.getMessage());
            });
    }
    
    // Асинхронный вызов нескольких сервисов параллельно
    public CompletableFuture<OrderPreparation> prepareOrderParallel(OrderRequest request) {
        CompletableFuture<InventoryResponse> inventoryFuture = 
            inventoryService.checkAvailabilityAsync(request.getProductId(), request.getQuantity());
        
        CompletableFuture<UserValidationResponse> userFuture = 
            userService.validateUserAsync(request.getUserId());
        
        CompletableFuture<PricingResponse> pricingFuture = 
            pricingService.calculatePriceAsync(request.getProductId(), request.getQuantity());
        
        // Ждем завершения всех задач
        return CompletableFuture.allOf(inventoryFuture, userFuture, pricingFuture)
            .thenApply(ignored -> {
                try {
                    InventoryResponse inventory = inventoryFuture.get();
                    UserValidationResponse user = userFuture.get();
                    PricingResponse pricing = pricingFuture.get();
                    
                    return new OrderPreparation(inventory, user, pricing);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
    }
}

// Асинхронный InventoryService
@Service
public class AsyncInventoryService {
    
    private final WebClient webClient;
    
    public AsyncInventoryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://inventory-service").build();
    }
    
    // АСИНХРОННЫЙ вызов - не блокирует поток
    public CompletableFuture<InventoryResponse> checkAvailabilityAsync(String productId, int quantity) {
        return webClient.post()
            .uri("/api/inventory/check")
            .bodyValue(new InventoryCheckRequest(productId, quantity))
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .toFuture(); // Конвертируем в CompletableFuture
    }
    
    // Альтернатива с Mono (Reactive)
    public Mono<InventoryResponse> checkAvailabilityReactive(String productId, int quantity) {
        return webClient.post()
            .uri("/api/inventory/check")
            .bodyValue(new InventoryCheckRequest(productId, quantity))
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .doOnNext(response -> log.info("Получен ответ от inventory service"))
            .doOnError(error -> log.error("Ошибка вызова inventory service", error));
    }
}
```
2. Message Queues (RabbitMQ/Kafka)
``` java
@Component
@Slf4j
public class MessageQueueOrderService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private final Map<String, CompletableFuture<OrderResponse>> pendingOrders = new ConcurrentHashMap<>();
    
    // Асинхронная отправка заказа через RabbitMQ
    public CompletableFuture<OrderResponse> createOrderViaMessageQueue(OrderRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        CompletableFuture<OrderResponse> future = new CompletableFuture<>();
        pendingOrders.put(correlationId, future);
        
        // Сохраняем заказ в статусе PENDING
        Order order = new Order(request);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        
        // Отправляем сообщение в очередь - НЕ БЛОКИРУЕМ поток
        rabbitTemplate.convertAndSend(
            "order.exchange",
            "order.create",
            new OrderMessage(correlationId, order.getId(), request),
            message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                return message;
            }
        );
        
        log.info("Заказ отправлен в очередь, correlationId: {}", correlationId);
        
        // Таймаут для future
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30000); // 30 секунд таймаут
                if (!future.isDone()) {
                    future.completeExceptionally(new TimeoutException("Timeout processing order"));
                    pendingOrders.remove(correlationId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return future;
    }
    
    // Обработчик ответов из очереди
    @RabbitListener(queues = "order.response.queue")
    public void handleOrderResponse(OrderResponseMessage response) {
        String correlationId = response.getCorrelationId();
        CompletableFuture<OrderResponse> future = pendingOrders.remove(correlationId);
        
        if (future != null) {
            if (response.isSuccess()) {
                future.complete(new OrderResponse(true, response.getMessage(), response.getOrderId()));
            } else {
                future.completeExceptionally(new OrderException(response.getMessage()));
            }
        } else {
            log.warn("Не найден pending order для correlationId: {}", correlationId);
        }
    }
}

// Классы сообщений
public class OrderMessage implements Serializable {
    private String correlationId;
    private Long orderId;
    private OrderRequest orderRequest;
    // constructor, getters/setters
}

public class OrderResponseMessage implements Serializable {
    private String correlationId;
    private boolean success;
    private String message;
    private Long orderId;
    // constructor, getters/setters
}
```
3. Reactive Programming with WebFlux

``` java
@RestController
@RequestMapping("/api/reactive/orders")
public class ReactiveOrderController {
    
    @Autowired
    private ReactiveOrderService orderService;
    
    // АСИНХРОННЫЙ реактивный endpoint
    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> createOrderReactive(@RequestBody OrderRequest request) {
        return orderService.createOrderReactive(request)
            .map(orderResponse -> ResponseEntity.ok(orderResponse))
            .onErrorResume(InventoryException.class, error -> 
                Mono.just(ResponseEntity.badRequest()
                    .body(new OrderResponse(false, error.getMessage()))))
            .onErrorResume(PaymentException.class, error -> 
                Mono.just(ResponseEntity.badRequest()
                    .body(new OrderResponse(false, error.getMessage()))))
            .doOnSubscribe(subscription -> 
                log.info("Начало обработки заказа для: {}", request.getUserId()))
            .doOnSuccess(response -> 
                log.info("Успешная обработка заказа"));
    }
}

@Service
public class ReactiveOrderService {
    
    private final WebClient inventoryWebClient;
    private final WebClient paymentWebClient;
    
    public ReactiveOrderService(WebClient.Builder webClientBuilder) {
        this.inventoryWebClient = webClientBuilder.baseUrl("http://inventory-service").build();
        this.paymentWebClient = webClientBuilder.baseUrl("http://payment-service").build();
    }
    
    public Mono<OrderResponse> createOrderReactive(OrderRequest request) {
        // Реактивная цепочка вызовов - НЕТ БЛОКИРОВКИ
        return checkInventoryReactive(request.getProductId(), request.getQuantity())
            .flatMap(inventoryResponse -> {
                if (!inventoryResponse.isAvailable()) {
                    return Mono.error(new InventoryException("Товар недоступен"));
                }
                return processPaymentReactive(request.getPaymentDetails(), request.getAmount());
            })
            .flatMap(paymentResponse -> {
                if (!paymentResponse.isSuccess()) {
                    return Mono.error(new PaymentException(paymentResponse.getMessage()));
                }
                return saveOrderReactive(request);
            })
            .map(order -> new OrderResponse(true, "Заказ создан", order.getId()))
            .doOnNext(response -> 
                sendNotificationReactive(response.getOrderId()).subscribe()) // Fire and forget
            .timeout(Duration.ofSeconds(30)) // Таймаут всей операции
            .onErrorResume(throwable -> {
                log.error("Ошибка при создании заказа: {}", throwable.getMessage());
                return Mono.just(new OrderResponse(false, "Ошибка: " + throwable.getMessage()));
            });
    }
    
    private Mono<InventoryResponse> checkInventoryReactive(String productId, int quantity) {
        return inventoryWebClient.post()
            .uri("/api/inventory/check")
            .bodyValue(new InventoryCheckRequest(productId, quantity))
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .doOnError(error -> log.error("Ошибка вызова inventory service: {}", error.getMessage()));
    }
    
    private Mono<PaymentResponse> processPaymentReactive(PaymentDetails paymentDetails, BigDecimal amount) {
        return paymentWebClient.post()
            .uri("/api/payments/process")
            .bodyValue(new PaymentRequest(paymentDetails, amount))
            .retrieve()
            .bodyToMono(PaymentResponse.class);
    }
    
    private Mono<Order> saveOrderReactive(OrderRequest request) {
        return Mono.fromCallable(() -> {
            Order order = new Order(request);
            return orderRepository.save(order);
        }).subscribeOn(Schedulers.boundedElastic()); // Выполняем блокирующую операцию в отдельном потоке
    }
    
    private Mono<Void> sendNotificationReactive(Long orderId) {
        return notificationWebClient.post()
            .uri("/api/notifications/order-confirmation")
            .bodyValue(new NotificationRequest(orderId))
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(error -> log.warn("Не удалось отправить уведомление: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty()); // Игнорируем ошибки уведомлений
    }
}
```
4. Практические рекомендации
**Когда использовать синхронные вызовы:**
``` java 
// ✅ Идеальные случаи для синхронных вызовов:

// 1. Простые CRUD операции
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) {
    return userService.findById(id); // Быстрая локальная операция
}

// 2. Когда нужна немедленная обратная связь
@PostMapping("/validate")
public ValidationResult validateForm(@RequestBody FormData form) {
    return validationService.validate(form); // Быстрая проверка
}

// 3. Последовательные операции, зависящие друг от друга
public Order processOrder(OrderRequest request) {
    // Каждый шаг зависит от предыдущего
    validate(request);
    checkInventory(request);
    processPayment(request);
    return createOrder(request);
}
```
**Когда использовать асинхронные вызовы:**

``` java
// ✅ Идеальные случаи для асинхронных вызовов:

// 1. Параллельные независимые операции
public CompletableFuture<AggregatedData> getDashboardData(String userId) {
    CompletableFuture<User> userFuture = userService.getUserAsync(userId);
    CompletableFuture<List<Order>> ordersFuture = orderService.getOrdersAsync(userId);
    CompletableFuture<Preferences> prefsFuture = prefService.getPreferencesAsync(userId);
    
    return CompletableFuture.allOf(userFuture, ordersFuture, prefsFuture)
        .thenApply(ignored -> new AggregatedData(
            userFuture.join(), ordersFuture.join(), prefsFuture.join()
        ));
}

// 2. Длительные операции
@Async
public CompletableFuture<Report> generateReport(ReportRequest request) {
    // Операция может занимать минуты
    Report report = reportService.generateComplexReport(request);
    return CompletableFuture.completedFuture(report);
}

// 3. Event-driven архитектура
@EventListener
public void handleOrderCreatedEvent(OrderCreatedEvent event) {
    // Асинхронная обработка события
    notificationService.sendNotificationAsync(event.getOrder());
    analyticsService.trackOrderAsync(event.getOrder());
    recommendationService.updateRecommendationsAsync(event.getOrder().getUserId());
}
```

**Гибридный подход:**
``` java 
@Service
public class HybridOrderService {
    
    public OrderProcessingResult processOrderHybrid(OrderRequest request) {
        // Синхронно: критически важные проверки
        User user = userService.validateUser(request.getUserId());
        InventoryResponse inventory = inventoryService.checkAvailability(
            request.getProductId(), request.getQuantity());
        
        if (!inventory.isAvailable()) {
            throw new InventoryException("Товар недоступен");
        }
        
        // Асинхронно: не-критичные операции
        CompletableFuture<Void> notificationFuture = notificationService
            .sendWelcomeOfferAsync(user.getId());
        CompletableFuture<Void> analyticsFuture = analyticsService
            .trackUserActivityAsync(user.getId(), "order_started");
        
        // Синхронно: основная бизнес-логика
        PaymentResponse payment = paymentService.processPayment(request);
        Order order = orderService.createOrder(request, user, payment);
        
        // Асинхронно: пост-обработка
        CompletableFuture.runAsync(() -> {
            notificationService.sendOrderConfirmation(order.getId());
            recommendationService.updateBasedOnOrder(order);
        });
        
        return new OrderProcessingResult(order, true, "Order processed");
    }
}
```
Publish-Subscribe на примере Guava EventBus
1. Теоретический материал
**Что такое Publish-Subscribe?**
**Publish-Subscribe (Pub/Sub)** — это паттерн messaging, где отправители сообщений (**publishers**) не отправляют сообщения напрямую получателям (**subscribers**). Вместо этого сообщения отправляются в каналы (**channels/topics**), и подписчики получают только те сообщения, которые их интересуют.

Ключевые компоненты:
- **Publisher** — публикует события
- **Subscriber** — подписывается и обрабатывает события
- **EventBus** — центральный коммуникационный хаб
- **Event** — объект-сообщение, передаваемый между компонентами

Преимущества **Guava EventBus**:
✅ Простая настройка

✅ Легковесный

✅ Синхронная и асинхронная обработка

✅ Аннотационный подход

✅ Нет зависимостей от внешних брокеров

2. Настройка и базовое использование
**Зависимости Maven/Gradle:**
``` xml
<!-- Maven -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>
```

**Базовая конфигурация:**
``` java
@Configuration
public class EventBusConfig {
    
    // Синхронный EventBus - обработка в том же потоке
    @Bean
    public EventBus syncEventBus() {
        return new EventBus(); // По умолчанию синхронный
    }
    
    // Асинхронный EventBus - обработка в отдельном потоке
    @Bean
    public AsyncEventBus asyncEventBus() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("async-eventbus-%d")
            .setDaemon(true)
            .build();
        
        ExecutorService executor = Executors.newFixedThreadPool(10, threadFactory);
        return new AsyncEventBus(executor);
    }
    
    // EventBus с кастомной обработкой исключений
    @Bean
    public EventBus eventBusWithExceptionHandler() {
        return new EventBus(new SubscriberExceptionHandler() {
            @Override
            public void handleException(Throwable exception, SubscriberExceptionContext context) {
                log.error("Ошибка в подписчике: {}. Событие: {}", 
                    context.getSubscriberMethod(), context.getEvent(), exception);
            }
        });
    }
}
```

**3. Создание событий (Events)**
``` java
// Базовый класс для всех событий
public abstract class BaseEvent {
    private final String eventId;
    private final Instant timestamp;
    private final String source;
    
    public BaseEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.source = source;
    }
    
    // геттеры
}

// События доменной области
public class UserRegisteredEvent extends BaseEvent {
    private final Long userId;
    private final String username;
    private final String email;
    private final Instant registrationDate;
    
    public UserRegisteredEvent(Long userId, String username, String email) {
        super("user-service");
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.registrationDate = Instant.now();
    }
    
    // геттеры
}

public class OrderCreatedEvent extends BaseEvent {
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final List<OrderItem> items;
    
    public OrderCreatedEvent(Long orderId, Long userId, BigDecimal amount, List<OrderItem> items) {
        super("order-service");
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.items = items;
    }
    
    // геттеры
}

public class PaymentProcessedEvent extends BaseEvent {
    private final Long paymentId;
    private final Long orderId;
    private final PaymentStatus status;
    private final BigDecimal amount;
    private final String transactionId;
    
    public PaymentProcessedEvent(Long paymentId, Long orderId, PaymentStatus status, 
                               BigDecimal amount, String transactionId) {
        super("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.transactionId = transactionId;
    }
    
    // геттеры
}

public class InventoryUpdatedEvent extends BaseEvent {
    private final String productId;
    private final int quantityChange;
    private final InventoryUpdateType updateType;
    private final String reason;
    
    public InventoryUpdatedEvent(String productId, int quantityChange, 
                               InventoryUpdateType updateType, String reason) {
        super("inventory-service");
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.updateType = updateType;
        this.reason = reason;
    }
    
    // геттеры
}

// Enum для типов событий
public enum InventoryUpdateType {
    STOCK_DECREASE,
    STOCK_INCREASE,
    STOCK_CORRECTION
}

public enum PaymentStatus {
    SUCCESS, FAILED, PENDING
}
```

4. Подписчики (Subscribers)
**Базовый подписчик с логированием:**
``` java
@Component
@Slf4j
public class EventLoggingSubscriber {
    
    // Подписка на все события
    @Subscribe
    public void handleAllEvents(BaseEvent event) {
        log.info("📢 Получено событие: [{}] {} от {}", 
            event.getEventId(), 
            event.getClass().getSimpleName(),
            event.getSource());
    }
    
    // Подписка только на UserRegisteredEvent
    @Subscribe
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("👤 Новый пользователь: {} (ID: {})", 
            event.getUsername(), event.getUserId());
    }
}
```
**Подписчики для бизнес-логики:**
``` java
@Component
@Slf4j
public class NotificationSubscriber {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    // Отправка приветственного email при регистрации
    @Subscribe
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        try {
            emailService.sendWelcomeEmail(event.getEmail(), event.getUsername());
            log.info("📧 Приветственное письмо отправлено для: {}", event.getEmail());
        } catch (Exception e) {
            log.error("❌ Ошибка отправки приветственного письма для: {}", event.getEmail(), e);
        }
    }
    
    // Уведомление о создании заказа
    @Subscribe
    public void notifyOrderCreation(OrderCreatedEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                // Имитация отправки уведомления
                Thread.sleep(1000);
                log.info("📦 Уведомление о заказе {} отправлено пользователю {}", 
                    event.getOrderId(), event.getUserId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    // Уведомление о статусе платежа
    @Subscribe
    public void handlePaymentNotification(PaymentProcessedEvent event) {
        if (event.getStatus() == PaymentStatus.SUCCESS) {
            log.info("✅ Платеж успешен для заказа {}. Transaction: {}", 
                event.getOrderId(), event.getTransactionId());
        } else {
            log.warn("❌ Платеж не прошел для заказа {}", event.getOrderId());
        }
    }
}

@Component
@Slf4j
public class AnalyticsSubscriber {
    
    @Autowired
    private AnalyticsService analyticsService;
    
    // Сбор аналитики по регистрациям
    @Subscribe
    public void trackUserRegistration(UserRegisteredEvent event) {
        analyticsService.trackEvent("user_registered", Map.of(
            "user_id", event.getUserId().toString(),
            "username", event.getUsername(),
            "timestamp", event.getTimestamp().toString()
        ));
        log.info("📊 Аналитика: зарегистрирован пользователь {}", event.getUsername());
    }
    
    // Сбор аналитики по заказам
    @Subscribe
    public void trackOrderCreation(OrderCreatedEvent event) {
        analyticsService.trackEvent("order_created", Map.of(
            "order_id", event.getOrderId().toString(),
            "user_id", event.getUserId().toString(),
            "amount", event.getAmount().toString(),
            "items_count", String.valueOf(event.getItems().size())
        ));
        log.info("📊 Аналитика: создан заказ {} на сумму {}", 
            event.getOrderId(), event.getAmount());
    }
    
    // Обновление метрик инвентаря
    @Subscribe
    public void updateInventoryMetrics(InventoryUpdatedEvent event) {
        analyticsService.updateInventoryMetric(
            event.getProductId(), 
            event.getQuantityChange(),
            event.getUpdateType()
        );
        log.info("📊 Аналитика: обновлен инвентарь продукта {} на {}", 
            event.getProductId(), event.getQuantityChange());
    }
}

@Component
@Slf4j
public class InventorySubscriber {
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private RestockService restockService;
    
    // Обновление остатков при создании заказа
    @Subscribe
    public void updateStockForOrder(OrderCreatedEvent event) {
        log.info("📦 Обновление остатков для заказа {}", event.getOrderId());
        
        event.getItems().forEach(item -> {
            try {
                int updatedRows = inventoryRepository.decreaseStock(
                    item.getProductId(), 
                    item.getQuantity()
                );
                
                if (updatedRows == 0) {
                    log.warn("⚠️ Недостаточно остатков для продукта: {}", item.getProductId());
                    // Можно опубликовать событие недостатка остатков
                } else {
                    log.info("✅ Остатки обновлены для продукта: {}", item.getProductId());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка обновления остатков для продукта: {}", item.getProductId(), e);
            }
        });
    }
    
    // Автоматический заказ при низких остатках
    @Subscribe
    public void handleLowStock(InventoryUpdatedEvent event) {
        if (event.getUpdateType() == InventoryUpdateType.STOCK_DECREASE) {
            int currentStock = inventoryRepository.getCurrentStock(event.getProductId());
            
            if (currentStock < 10) { // Порог для автоматического заказа
                log.info("🔄 Низкие остатки продукта {}. Текущий запас: {}", 
                    event.getProductId(), currentStock);
                
                restockService.requestRestock(event.getProductId(), 100);
            }
        }
    }
}
```
5. Публикация событий (Publishers)
**Сервисы-публикаторы:**
``` java
@Service
@Slf4j
public class UserRegistrationService {
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private UserRepository userRepository;
    
    public User registerUser(String username, String email, String password) {
        log.info("Начало регистрации пользователя: {}", username);
        
        // Создание пользователя
        User user = new User(username, email, password);
        user = userRepository.save(user);
        
        // Публикация события
        UserRegisteredEvent event = new UserRegisteredEvent(
            user.getId(), user.getUsername(), user.getEmail());
        
        eventBus.post(event);
        log.info("✅ Опубликовано событие регистрации пользователя: {}", user.getId());
        
        return user;
    }
}

@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    public Order createOrder(Long userId, List<OrderItem> items) {
        log.info("Создание заказа для пользователя: {}", userId);
        
        // Расчет суммы
        BigDecimal totalAmount = calculateTotalAmount(items);
        
        // Создание заказа
        Order order = new Order(userId, items, totalAmount);
        order = orderRepository.save(order);
        
        // Публикация события создания заказа
        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
            order.getId(), userId, totalAmount, items);
        eventBus.post(orderEvent);
        
        // Обработка платежа
        PaymentResult paymentResult = paymentService.processPayment(userId, totalAmount);
        
        // Публикация события платежа
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
            paymentResult.getPaymentId(),
            order.getId(),
            paymentResult.getStatus(),
            totalAmount,
            paymentResult.getTransactionId()
        );
        eventBus.post(paymentEvent);
        
        log.info("✅ Опубликованы события для заказа: {}", order.getId());
        
        return order;
    }
    
    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

@Service
@Slf4j
public class InventoryService {
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    public void updateStock(String productId, int quantityChange, String reason) {
        log.info("Обновление остатков продукта {} на {}", productId, quantityChange);
        
        InventoryUpdateType updateType = quantityChange > 0 ? 
            InventoryUpdateType.STOCK_INCREASE : InventoryUpdateType.STOCK_DECREASE;
        
        // Обновление в базе
        inventoryRepository.updateStock(productId, quantityChange);
        
        // Публикация события
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
            productId, quantityChange, updateType, reason);
        
        eventBus.post(event);
        log.info("✅ Опубликовано событие обновления инвентаря: {}", productId);
    }
}
```
6. Регистрация подписчиков
**Конфигурация регистрации:**
``` java
@Component
@Slf4j
public class EventBusRegistrar {
    
    private final EventBus eventBus;
    private final List<Object> subscribers;
    
    @Autowired
    public EventBusRegistrar(EventBus eventBus,
                           EventLoggingSubscriber loggingSubscriber,
                           NotificationSubscriber notificationSubscriber,
                           AnalyticsSubscriber analyticsSubscriber,
                           InventorySubscriber inventorySubscriber) {
        this.eventBus = eventBus;
        this.subscribers = Arrays.asList(
            loggingSubscriber,
            notificationSubscriber,
            analyticsSubscriber,
            inventorySubscriber
        );
        
        registerAllSubscribers();
    }
    
    private void registerAllSubscribers() {
        subscribers.forEach(subscriber -> {
            eventBus.register(subscriber);
            log.info("✅ Зарегистрирован подписчик: {}", subscriber.getClass().getSimpleName());
        });
        
        log.info("Всего зарегистрировано подписчиков: {}", subscribers.size());
    }
    
    // Метод для динамической регистрации
    public void registerSubscriber(Object subscriber) {
        eventBus.register(subscriber);
        log.info("✅ Динамически зарегистрирован подписчик: {}", 
            subscriber.getClass().getSimpleName());
    }
    
    // Метод для удаления подписчика
    public void unregisterSubscriber(Object subscriber) {
        eventBus.unregister(subscriber);
        log.info("❌ Удален подписчик: {}", subscriber.getClass().getSimpleName());
    }
}
```
7. Расширенные возможности
**Фильтрация событий:**
``` java
@Component
@Slf4j
public class FilteredSubscriber {
    
    // Подписка с фильтрацией по условию
    @Subscribe
    public void handleLargeOrders(OrderCreatedEvent event) {
        if (event.getAmount().compareTo(new BigDecimal("1000")) > 0) {
            log.info("💰 Крупный заказ! ID: {}, Сумма: {}", 
                event.getOrderId(), event.getAmount());
            
            // Дополнительная обработка для крупных заказов
            handleLargeOrder(event);
        }
    }
    
    // Подписка на определенные статусы платежей
    @Subscribe
    public void handleFailedPayments(PaymentProcessedEvent event) {
        if (event.getStatus() == PaymentStatus.FAILED) {
            log.warn("💸 Неудачный платеж для заказа: {}", event.getOrderId());
            
            // Уведомление поддержки
            notifySupportTeam(event);
        }
    }
    
    private void handleLargeOrder(OrderCreatedEvent event) {
        // Специальная логика для крупных заказов
    }
    
    private void notifySupportTeam(PaymentProcessedEvent event) {
        // Уведомление команды поддержки
    }
}
```
**Цепочка обработки событий:**
``` java
@Component
@Slf4j
public class OrderProcessingPipeline {
    
    @Autowired
    private EventBus eventBus;
    
    // Первый шаг - валидация
    @Subscribe
    public void validateOrder(OrderCreatedEvent event) {
        log.info("1️⃣ Валидация заказа: {}", event.getOrderId());
        
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("❌ Невалидная сумма заказа: {}", event.getOrderId());
            return;
        }
        
        // Публикуем событие для следующего шага
        eventBus.post(new OrderValidatedEvent(event.getOrderId()));
    }
    
    // Второй шаг - резервирование товаров
    @Subscribe
    public void reserveInventory(OrderValidatedEvent event) {
        log.info("2️⃣ Резервирование товаров для заказа: {}", event.getOrderId());
        
        // Логика резервирования...
        eventBus.post(new InventoryReservedEvent(event.getOrderId()));
    }
    
    // Третий шаг - подтверждение заказа
    @Subscribe
    public void confirmOrder(InventoryReservedEvent event) {
        log.info("3️⃣ Подтверждение заказа: {}", event.getOrderId());
        
        // Финальная обработка...
        eventBus.post(new OrderCompletedEvent(event.getOrderId()));
    }
}

// События для цепочки
public class OrderValidatedEvent extends BaseEvent {
    private final Long orderId;
    public OrderValidatedEvent(Long orderId) {
        super("order-service");
        this.orderId = orderId;
    }
    // геттер
}

public class InventoryReservedEvent extends BaseEvent {
    private final Long orderId;
    public InventoryReservedEvent(Long orderId) {
        super("inventory-service");
        this.orderId = orderId;
    }
    // геттер
}

public class OrderCompletedEvent extends BaseEvent {
    private final Long orderId;
    public OrderCompletedEvent(Long orderId) {
        super("order-service");
        this.orderId = orderId;
    }
    // геттер
}
```
**Обработка ошибок:**
``` java
@Component
@Slf4j
public class ErrorHandlingSubscriber {
    
    // Глобальная обработка ошибок
    @Subscribe
    public void handleSubscriberErrors(DeadEvent deadEvent) {
        log.error("⚠️ Dead event: {} от источника {}", 
            deadEvent.getEvent().getClass().getSimpleName(),
            deadEvent.getSource().getClass().getSimpleName());
    }
    
    // Повторная попытка обработки
    @Subscribe
    public void handleWithRetry(OrderCreatedEvent event) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                processOrder(event);
                return; // Успешно обработано
            } catch (Exception e) {
                attempt++;
                log.warn("Попытка {}/{} не удалась для заказа {}", 
                    attempt, maxRetries, event.getOrderId(), e);
                
                if (attempt == maxRetries) {
                    log.error("❌ Все попытки обработки заказа {} не удались", event.getOrderId());
                    // Можно отправить в очередь мертвых писем
                }
            }
        }
    }
    
    private void processOrder(OrderCreatedEvent event) throws Exception {
        // Логика обработки, которая может выбросить исключение
        if (Math.random() < 0.3) { // 30% вероятность ошибки для демо
            throw new RuntimeException("Случайная ошибка обработки");
        }
        log.info("✅ Заказ {} успешно обработан", event.getOrderId());
    }
}
```
**Пример использования в контроллере:**
``` java
@RestController
@RequestMapping("/api/events")
@Slf4j
public class EventDemoController {
    
    @Autowired
    private UserRegistrationService userRegistrationService;
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/register-user")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest request) {
        User user = userRegistrationService.registerUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword()
        );
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/create-order")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
            request.getUserId(),
            request.getItems()
        );
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/demo")
    public ResponseEntity<String> demoEventFlow() {
        // Демонстрация полного цикла событий
        User user = userRegistrationService.registerUser("demo_user", "demo@example.com", "password");
        
        List<OrderItem> items = List.of(
            new OrderItem("demo_product", 1, new BigDecimal("99.99"))
        );
        
        Order order = orderService.createOrder(user.getId(), items);
        
        return ResponseEntity.ok("Demo completed. User ID: " + user.getId() + ", Order ID: " + order.getId());
    }
}
```
Guava EventBus предоставляет простой и эффективный способ реализации паттерна Publish-Subscribe в Java-приложениях, позволяя создавать слабосвязанные, расширяемые системы с четким разделением ответственности.

