# 宿舍电源管理系统 - Spring Security 使用文档

## 目录
- [1. 项目为什么使用 Spring Security](#1-项目为什么使用-spring-security)
- [2. 项目如何使用 Spring Security](#2-项目如何使用-spring-security)
- [3. 核心功能实现](#3-核心功能实现)
- [4. 安全架构设计](#4-安全架构设计)
- [5. 面试要点总结](#5-面试要点总结)

---

## 1. 项目为什么使用 Spring Security

### 1.1 业务需求分析

宿舍电源管理系统是一个典型的企业级 IoT 应用，具有以下安全需求：

| 安全需求 | 具体场景 | Spring Security 解决方案 |
|----------|----------|--------------------------|
| **用户认证** | 管理员、普通用户登录 | JWT 无状态认证 |
| **权限控制** | 不同角色访问不同功能 | RBAC 权限模型 |
| **接口保护** | 防止未授权访问 | URL 级别授权 |
| **密码安全** | 用户密码加密存储 | PBKDF2 加密 |
| **跨域访问** | 前后端分离架构 | CORS 配置 |
| **限流保护** | 防止暴力破解 | 限流注解 + AOP |

### 1.2 Spring Security 在项目中的核心价值

#### 1.2.1 完善的认证机制

**问题**：需要支持多种认证方式，保证认证安全

**解决方案**：基于 JWT 的无状态认证

```
┌─────────────────────────────────────────────────────────────┐
│                      JWT 认证流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 用户登录                                                │
│     POST /api/auth/login                                    │
│     {username, password}                                    │
│                                                             │
│  2. 验证用户                                                │
│     ┌────────────────────────────────────┐                 │
│     │  - 查询用户信息                     │                 │
│     │  - PBKDF2 验证密码                  │                 │
│     │  - 生成 JWT Token                   │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  3. 返回 Token                                             │
│     {token: "eyJhbG...", user: {...}}                      │
│                                                             │
│  4. 后续请求                                                │
│     GET /api/devices                                        │
│     Header: Authorization: Bearer eyJhbG...                 │
│                                                             │
│  5. Token 验证                                             │
│     ┌────────────────────────────────────┐                 │
│     │  - 解析 Token                       │                 │
│     │  - 验证签名和过期时间               │                 │
│     │  - 加载用户权限                     │                 │
│     │  - 设置安全上下文                   │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  6. 授权检查并返回数据                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**效果**：
- 无状态认证，支持分布式部署
- Token 自包含用户信息，减少数据库查询
- 支持跨域访问，适合前后端分离

#### 1.2.2 灵活的权限控制

**问题**：不同角色用户需要访问不同功能

**解决方案**：RBAC 权限模型 + 细粒度授权

```
┌─────────────────────────────────────────────────────────────┐
│                    RBAC 权限模型                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户(User)                                                 │
│     │                                                       │
│     │ 多对多                                                │
│     ▼                                                       │
│  角色(Role)                                                 │
│     │                                                       │
│     │ 多对多                                                │
│     ▼                                                       │
│  权限(Permission)                                           │
│                                                             │
│  项目中的角色：                                             │
│  ┌──────────────────────────────────────────────┐          │
│  │  ADMIN: 系统管理员                            │          │
│  │  - 所有权限                                   │          │
│  │  - RBAC 管理                                  │          │
│  │  - 系统配置                                   │          │
│  └──────────────────────────────────────────────┘          │
│  ┌──────────────────────────────────────────────┐          │
│  │  USER: 普通用户                               │          │
│  │  - 设备查看                                   │          │
│  │  - 数据查询                                   │          │
│  │  - 个人信息管理                               │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 1.2.3 多层次安全防护

```
┌─────────────────────────────────────────────────────────────┐
│                    安全防护层次                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  第1层：CORS 跨域控制                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  - 允许指定来源访问                           │          │
│  │  - 限制请求方法                               │          │
│  │  - 控制请求头                                 │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│  第2层：认证验证                                            │
│  ┌──────────────────────────────────────────────┐          │
│  │  - JWT Token 验证                            │          │
│  │  - Token 黑名单检查                          │          │
│  │  - 过期时间验证                               │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│  第3层：权限检查                                            │
│  ┌──────────────────────────────────────────────┐          │
│  │  - URL 级别授权                               │          │
│  │  - 方法级别授权                               │          │
│  │  - 动态权限加载                               │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│  第4层：限流保护                                            │
│  ┌──────────────────────────────────────────────┐          │
│  │  - 登录接口限流                               │          │
│  │  - API 访问限流                               │          │
│  │  - 设备控制限流                               │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│  第5层：审计日志                                            │
│  ┌──────────────────────────────────────────────┐          │
│  │  - 登录日志                                   │          │
│  │  - 操作日志                                   │          │
│  │  - 异常日志                                   │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 技术选型对比

| 对比维度 | Spring Security | 自研安全框架 | Apache Shiro |
|----------|----------------|--------------|--------------|
| **功能完整性** | 非常完善 | 需自己实现 | 完善 |
| **Spring 集成** | 原生支持 | 需自己集成 | 需配置 |
| **JWT 支持** | 需自定义 | 需自己实现 | 需自定义 |
| **RBAC 支持** | 完善 | 需自己实现 | 完善 |
| **社区支持** | 非常活跃 | 无 | 活跃 |
| **学习曲线** | 陡峭 | - | 平缓 |
| **维护成本** | 低 | 高 | 中 |

**结论**：Spring Security 是企业级 Spring 应用的最佳选择

---

## 2. 项目如何使用 Spring Security

### 2.1 核心配置类

#### 2.1.1 SecurityConfig - 安全配置

**文件**：[SecurityConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/SecurityConfig.java)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // 禁用 CSRF（JWT 无状态应用不需要）
            .csrf(AbstractHttpConfigurer::disable)
            
            // URL 权限配置
            .authorizeHttpRequests(authorize -> authorize
                // OPTIONS 预检请求放行
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 公开访问路径
                .requestMatchers(
                    "/health",
                    "/actuator/**",
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // 管理员路径
                .requestMatchers("/api/rbac/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 认证路径
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/devices/**").authenticated()
                .requestMatchers("/api/telemetry/**").authenticated()
                .requestMatchers("/api/commands/**").authenticated()
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**设计要点**：
1. **无状态认证**：禁用 CSRF，使用 JWT
2. **细粒度授权**：URL 级别的权限控制
3. **预检请求支持**：OPTIONS 请求放行
4. **方法级安全**：`@EnableMethodSecurity` 支持注解授权

#### 2.1.2 JwtAuthenticationFilter - JWT 认证过滤器

**文件**：[JwtAuthenticationFilter.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/JwtAuthenticationFilter.java)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RbacService rbacService;

    @Value("${security.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 从请求头获取 Token
        String token = getTokenFromRequest(request);
        
        // 2. 验证 Token
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                // 3. 解析 Token 获取用户信息
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                
                // 4. 获取用户权限（角色 + 具体权限）
                List<SimpleGrantedAuthority> authorities = getUserAuthorities(username, role);
                
                // 5. 创建认证对象
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 6. 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token 无效，继续过滤链
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * 获取用户的所有权限（角色 + 具体权限）
     */
    private List<SimpleGrantedAuthority> getUserAuthorities(String username, String defaultRole) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        try {
            // 从 RBAC 系统获取用户的角色
            List<Role> roles = rbacService.getUserRoles(username);
            
            if (roles.isEmpty() && defaultRole != null) {
                // 如果没有分配角色，使用默认角色
                authorities.add(new SimpleGrantedAuthority("ROLE_" + defaultRole.toUpperCase()));
            } else {
                // 添加角色和权限
                for (Role role : roles) {
                    if (role.isEnabled()) {
                        // 添加角色
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));
                        
                        // 添加角色的具体权限
                        Set<Permission> permissions = role.getPermissions();
                        if (permissions != null) {
                            for (Permission permission : permissions) {
                                if (permission.isEnabled()) {
                                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取权限失败，使用默认角色
            if (defaultRole != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + defaultRole.toUpperCase()));
            }
        }
        
        return authorities;
    }
}
```

**设计要点**：
1. **OncePerRequestFilter**：确保每个请求只过滤一次
2. **Token 黑名单检查**：支持用户登出
3. **动态权限加载**：从 RBAC 系统加载权限
4. **降级策略**：权限加载失败时使用默认角色

#### 2.1.3 JwtUtil - JWT 工具类

**文件**：[JwtUtil.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/util/JwtUtil.java)

```java
@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration}")
    private long expiration;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * 生成 JWT Token
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            // 检查 token 是否在黑名单中
            if (tokenBlacklist.isBlacklisted(token)) {
                return false;
            }
            
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 token 添加到黑名单
     */
    public void blacklistToken(String token) {
        tokenBlacklist.addToBlacklist(token);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
```

**设计要点**：
1. **自包含信息**：Token 包含用户名和角色
2. **黑名单机制**：支持用户登出
3. **异常处理**：Token 无效时返回 false

### 2.2 认证服务实现

#### 2.2.1 AuthService - 认证服务

**文件**：[AuthService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/AuthService.java)

```java
@Service
public class AuthService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 用户登录
     */
    public Map<String, Object> login(String account, String password) {
        // 1. 查找用户
        UserAccount user = userAccountRepository.findById(account)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. 验证密码（PBKDF2）
        if (!EncryptionUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        // 3. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        
        // 4. 返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
            "username", user.getUsername(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
        return response;
    }

    /**
     * 用户注册
     */
    public Map<String, Object> register(String username, String email, String password) {
        // 1. 检查用户名和邮箱是否存在
        if (userAccountRepository.existsById(username)) {
            throw new BusinessException("Username already exists");
        }
        if (userAccountRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email already exists");
        }

        // 2. 创建新用户
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(EncryptionUtil.hashPassword(password));  // PBKDF2 加密
        user.setRole("user");  // 默认角色
        userAccountRepository.save(user);

        // 3. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // 4. 返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
            "username", user.getUsername(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
        return response;
    }

    /**
     * 忘记密码
     */
    public Map<String, Object> forgotPassword(String email) {
        // 1. 查找用户
        UserAccount user = userAccountRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. 生成重置码
        String resetCode = generateResetCode();
        String resetCodeHash = EncryptionUtil.hashPassword(resetCode);

        // 3. 设置重置码和过期时间（1小时）
        user.setResetCodeHash(resetCodeHash);
        user.setResetExpiresAt(System.currentTimeMillis() + 3600000);
        userAccountRepository.save(user);

        // 4. 发送重置邮件
        sendResetEmail(user.getEmail(), resetCode);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset email sent");
        return response;
    }

    /**
     * 重置密码
     */
    public Map<String, Object> resetPassword(String email, String resetCode, String newPassword) {
        // 1. 查找用户
        UserAccount user = userAccountRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. 检查重置码是否过期
        if (user.getResetExpiresAt() < System.currentTimeMillis()) {
            throw new BusinessException("Reset code expired");
        }

        // 3. 验证重置码
        if (!EncryptionUtil.verifyPassword(resetCode, user.getResetCodeHash())) {
            throw new BusinessException("Invalid reset code");
        }

        // 4. 更新密码
        user.setPasswordHash(EncryptionUtil.hashPassword(newPassword));
        user.setResetCodeHash("");
        user.setResetExpiresAt(0);
        userAccountRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        return response;
    }
}
```

**设计要点**：
1. **PBKDF2 加密**：密码安全存储
2. **重置码机制**：支持密码找回
3. **邮件通知**：重置码通过邮件发送

#### 2.2.2 CustomUserDetailsService - 用户详情服务

**文件**：[CustomUserDetailsService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/CustomUserDetailsService.java)

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private RbacService rbacService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查找用户
        com.dormpower.model.UserAccount userAccount = userAccountRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // 2. 检查用户是否启用
        if (!userAccount.isEnabled()) {
            throw new UsernameNotFoundException("User is disabled: " + username);
        }
        
        // 3. 获取用户权限
        List<GrantedAuthority> authorities = getUserAuthorities(username);
        
        // 4. 构建 UserDetails
        return User.builder()
                .username(userAccount.getUsername())
                .password(userAccount.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!userAccount.isEnabled())
                .build();
    }
    
    /**
     * 获取用户的所有权限
     */
    private List<GrantedAuthority> getUserAuthorities(String username) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 获取用户的角色
        List<Role> roles = rbacService.getUserRoles(username);
        
        // 添加角色权限
        for (Role role : roles) {
            if (role.isEnabled()) {
                // 添加角色
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));
                
                // 添加角色的具体权限
                Set<Permission> permissions = role.getPermissions();
                if (permissions != null) {
                    for (Permission permission : permissions) {
                        if (permission.isEnabled()) {
                            authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                        }
                    }
                }
            }
        }
        
        return authorities;
    }
}
```

**设计要点**：
1. **实现 UserDetailsService**：Spring Security 标准接口
2. **动态权限加载**：从 RBAC 系统加载
3. **用户状态检查**：启用/禁用状态

### 2.3 密码加密实现

**文件**：[EncryptionUtil.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/util/EncryptionUtil.java)

```java
public class EncryptionUtil {

    private static final int ITERATIONS = 160000;  // 迭代次数
    private static final int KEY_LENGTH = 256;     // 密钥长度
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 生成随机盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * 使用 PBKDF2 算法哈希密码
     */
    public static String hashPassword(String password, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                HexFormat.of().parseHex(salt),
                iterations,
                KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * 哈希密码（使用默认迭代次数）
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        String digest = hashPassword(password, salt, ITERATIONS);
        return String.format("pbkdf2_sha256$%d$%s$%s", ITERATIONS, salt, digest);
    }

    /**
     * 验证密码
     */
    public static boolean verifyPassword(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            String algorithm = parts[0];
            if (!"pbkdf2_sha256".equals(algorithm)) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            String salt = parts[2];
            String expectedDigest = parts[3];
            String actualDigest = hashPassword(password, salt, iterations);
            return constantTimeEquals(expectedDigest, actualDigest);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 恒定时间比较，防止时序攻击
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
```

**设计要点**：
1. **PBKDF2 算法**：安全的密码哈希算法
2. **高迭代次数**：160000 次，增加破解难度
3. **随机盐值**：每次加密生成不同的盐
4. **恒定时间比较**：防止时序攻击

### 2.4 CORS 配置

**文件**：[CorsConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/CorsConfig.java)

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**设计要点**：
1. **允许所有来源**：开发环境方便调试
2. **支持所有方法**：GET、POST、PUT、DELETE、PATCH、OPTIONS
3. **允许凭证**：支持 Cookie 和 Authorization 头
4. **预检缓存**：1 小时

---

## 3. 核心功能实现

### 3.1 登录认证流程

```
┌─────────────────────────────────────────────────────────────┐
│                      登录认证流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 前端发起登录请求                                        │
│     POST /api/auth/login                                    │
│     {username: "admin", password: "admin123"}               │
│                                                             │
│  2. AuthController 接收请求                                 │
│     ┌────────────────────────────────────────┐             │
│     │  @RateLimit(value = 2.0, type = "login")│             │
│     │  @AuditLog(value = "用户登录")          │             │
│     │  public ResponseEntity<?> login()       │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  3. AuthService 处理登录                                    │
│     ┌────────────────────────────────────────┐             │
│     │  - 查询用户信息                         │             │
│     │  - PBKDF2 验证密码                      │             │
│     │  - 生成 JWT Token                       │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  4. 返回 Token 和用户信息                                   │
│     {                                                       │
│       "token": "eyJhbG...",                                 │
│       "user": {                                             │
│         "username": "admin",                                │
│         "email": "admin@dorm.local",                        │
│         "role": "admin"                                     │
│       }                                                     │
│     }                                                       │
│                                                             │
│  5. 前端存储 Token                                          │
│     localStorage.setItem('token', token)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 请求认证流程

```
┌─────────────────────────────────────────────────────────────┐
│                      请求认证流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 前端发起 API 请求                                       │
│     GET /api/devices                                        │
│     Header: Authorization: Bearer eyJhbG...                 │
│                                                             │
│  2. JwtAuthenticationFilter 拦截请求                        │
│     ┌────────────────────────────────────────┐             │
│     │  - 从请求头提取 Token                   │             │
│     │  - 验证 Token 有效性                    │             │
│     │  - 检查黑名单                           │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  3. 解析 Token 获取用户信息                                 │
│     ┌────────────────────────────────────────┐             │
│     │  - 解析 Claims                          │             │
│     │  - 获取 username 和 role                │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  4. 加载用户权限                                            │
│     ┌────────────────────────────────────────┐             │
│     │  - 从 RBAC 系统获取角色                 │             │
│     │  - 获取角色的具体权限                   │             │
│     │  - 构建 GrantedAuthority 列表           │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  5. 设置安全上下文                                          │
│     ┌────────────────────────────────────────┐             │
│     │  - 创建 Authentication 对象             │             │
│     │  - 设置到 SecurityContextHolder         │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  6. FilterSecurityInterceptor 授权检查                      │
│     ┌────────────────────────────────────────┐             │
│     │  - 检查 URL 权限                        │             │
│     │  - 调用 AccessDecisionManager           │             │
│     └────────────────┬───────────────────────┘             │
│                      │                                      │
│  7. 执行 Controller 并返回数据                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 权限检查流程

```
┌─────────────────────────────────────────────────────────────┐
│                      权限检查流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  URL 级别授权                                               │
│  ┌──────────────────────────────────────────────────┐      │
│  │  /api/rbac/** → hasRole('ADMIN')                 │      │
│  │  /api/devices/** → authenticated()               │      │
│  │  /api/auth/** → permitAll()                      │      │
│  └──────────────────────────────────────────────────┘      │
│                                                             │
│  方法级别授权                                               │
│  ┌──────────────────────────────────────────────────┐      │
│  │  @PreAuthorize("hasRole('ADMIN')")               │      │
│  │  public void deleteUser(Long userId) {           │      │
│  │      // 删除用户逻辑                              │      │
│  │  }                                               │      │
│  └──────────────────────────────────────────────────┘      │
│                                                             │
│  动态权限检查                                               │
│  ┌──────────────────────────────────────────────────┐      │
│  │  用户权限：ROLE_ADMIN, user:delete, user:update  │      │
│  │  资源权限：user:delete                           │      │
│  │  检查结果：允许访问                              │      │
│  └──────────────────────────────────────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 安全架构设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      安全架构设计                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   客户端层                            │  │
│  │   Web前端  │  移动端App  │  第三方系统                │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   安全层                              │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  CorsFilter (跨域控制)                      │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  JwtAuthenticationFilter (JWT认证)         │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  FilterSecurityInterceptor (授权检查)      │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   应用层                              │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  Controller (业务接口)                      │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  Service (业务逻辑)                         │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  │  ┌────────────────────────────────────────────┐      │  │
│  │  │  Repository (数据访问)                      │      │  │
│  │  └────────────────────────────────────────────┘      │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   数据层                              │  │
│  │  PostgreSQL  │  Redis  │  JWT Token                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 权限模型

```
┌─────────────────────────────────────────────────────────────┐
│                      RBAC 权限模型                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户(UserAccount)                                          │
│     │                                                       │
│     │ 多对多(UserRole)                                      │
│     ▼                                                       │
│  角色(Role)                                                 │
│     │                                                       │
│     │ 多对多(RolePermission)                                │
│     ▼                                                       │
│  权限(Permission)                                           │
│     │                                                       │
│     │ 多对多(PermissionResource)                            │
│     ▼                                                       │
│  资源(Resource)                                             │
│                                                             │
│  项目中的角色定义：                                         │
│  ┌──────────────────────────────────────────────┐          │
│  │  ADMIN (系统管理员)                           │          │
│  │  权限：                                       │          │
│  │  - rbac:manage (RBAC管理)                    │          │
│  │  - system:config (系统配置)                  │          │
│  │  - user:manage (用户管理)                    │          │
│  │  - device:manage (设备管理)                  │          │
│  │  - data:export (数据导出)                    │          │
│  └──────────────────────────────────────────────┘          │
│  ┌──────────────────────────────────────────────┐          │
│  │  USER (普通用户)                              │          │
│  │  权限：                                       │          │
│  │  - device:view (设备查看)                    │          │
│  │  - data:view (数据查看)                      │          │
│  │  - profile:edit (个人信息编辑)               │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 面试要点总结

### 5.1 项目实战问题

#### Q1: 你们项目为什么使用 Spring Security？

**答案**：

我们项目是一个企业级 IoT 宿舍电源管理系统，使用 Spring Security 主要有以下几个原因：

1. **完善的认证机制**：
   - 支持 JWT 无状态认证
   - 适合前后端分离架构
   - 支持分布式部署

2. **灵活的权限控制**：
   - 实现 RBAC 权限模型
   - 支持 URL 级别和方法级别授权
   - 动态权限加载

3. **多层次安全防护**：
   - CORS 跨域控制
   - 认证验证
   - 权限检查
   - 限流保护
   - 审计日志

4. **Spring 生态集成**：
   - 原生支持 Spring Boot
   - 与其他 Spring 组件无缝集成
   - 社区活跃，文档完善

#### Q2: 你们项目中 Spring Security 用在哪些场景？

**答案**：

1. **用户认证**：
   - 登录认证（JWT Token）
   - Token 验证和刷新
   - 用户登出（Token 黑名单）

2. **权限控制**：
   - URL 级别授权（管理员路径、用户路径）
   - 方法级别授权（敏感操作）
   - 动态权限加载（RBAC）

3. **安全防护**：
   - CORS 跨域配置
   - 密码加密存储（PBKDF2）
   - 限流保护（登录接口）

4. **审计日志**：
   - 登录日志
   - 操作日志
   - 异常日志

#### Q3: 你们如何实现 JWT 认证？

**答案**：

我们通过自定义过滤器实现 JWT 认证：

1. **自定义过滤器**：
   - 继承 `OncePerRequestFilter`
   - 从请求头提取 Token
   - 验证 Token 有效性

2. **Token 解析**：
   - 使用 JJWT 库解析
   - 提取用户名和角色
   - 检查黑名单

3. **权限加载**：
   - 从 RBAC 系统加载角色
   - 获取角色的具体权限
   - 构建 GrantedAuthority 列表

4. **安全上下文设置**：
   - 创建 Authentication 对象
   - 设置到 SecurityContextHolder

关键代码：
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String token = getTokenFromRequest(request);
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            List<SimpleGrantedAuthority> authorities = getUserAuthorities(username);
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

#### Q4: 你们如何实现 RBAC 权限模型？

**答案**：

我们实现了完整的 RBAC 权限模型：

1. **数据模型**：
   - 用户(UserAccount)
   - 角色(Role)
   - 权限(Permission)
   - 资源(Resource)
   - 多对多关联表

2. **权限加载**：
   - 实现 `UserDetailsService`
   - 根据用户名加载角色和权限
   - 构建 UserDetails 对象

3. **权限配置**：
   - URL 级别：SecurityConfig 中配置
   - 方法级别：使用 `@PreAuthorize` 注解

4. **动态权限**：
   - 权限变更时更新缓存
   - 支持运行时权限调整

关键代码：
```java
@Override
public UserDetails loadUserByUsername(String username) {
    UserAccount user = userRepository.findByUsername(username);
    List<GrantedAuthority> authorities = new ArrayList<>();
    
    for (Role role : user.getRoles()) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        for (Permission permission : role.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(permission.getCode()));
        }
    }
    
    return User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .build();
}
```

#### Q5: 你们如何保证密码安全？

**答案**：

我们使用 PBKDF2 算法加密密码：

1. **加密算法**：
   - PBKDF2WithHmacSHA256
   - 迭代次数：160000 次
   - 密钥长度：256 位

2. **随机盐值**：
   - 每次加密生成不同的盐
   - 16 字节随机盐

3. **存储格式**：
   - `pbkdf2_sha256$iterations$salt$digest`
   - 包含算法、迭代次数、盐值、摘要

4. **验证机制**：
   - 恒定时间比较
   - 防止时序攻击

关键代码：
```java
public static String hashPassword(String password) {
    String salt = generateSalt();
    String digest = hashPassword(password, salt, ITERATIONS);
    return String.format("pbkdf2_sha256$%d$%s$%s", ITERATIONS, salt, digest);
}

public static boolean verifyPassword(String password, String encoded) {
    String[] parts = encoded.split("\\$");
    int iterations = Integer.parseInt(parts[1]);
    String salt = parts[2];
    String expectedDigest = parts[3];
    String actualDigest = hashPassword(password, salt, iterations);
    return constantTimeEquals(expectedDigest, actualDigest);
}
```

#### Q6: 你们如何处理 Token 失效？

**答案**：

我们通过 Token 黑名单机制处理失效：

1. **黑名单机制**：
   - 用户登出时将 Token 加入黑名单
   - 验证 Token 时检查黑名单

2. **存储方式**：
   - 使用内存 Set 存储（单机）
   - 可扩展为 Redis 存储（分布式）

3. **过期处理**：
   - Token 自带过期时间
   - 过期后自动失效

关键代码：
```java
@Component
public class TokenBlacklist {
    private final Set<String> blacklist = new HashSet<>();

    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}

// 登出时添加到黑名单
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
    if (token != null && token.startsWith("Bearer ")) {
        String jwtToken = token.substring(7);
        jwtUtil.blacklistToken(jwtToken);
    }
    return ResponseEntity.ok("Logged out successfully");
}
```

#### Q7: 你们如何实现限流保护？

**答案**：

我们通过自定义注解 + AOP + Redis 实现限流：

1. **限流注解**：
   - `@RateLimit(value = 2.0, type = "login")`
   - 标记需要限流的接口

2. **AOP 切面**：
   - 拦截标记注解的方法
   - 调用限流器检查

3. **Redis 限流器**：
   - 滑动窗口算法
   - 分布式限流

关键代码：
```java
@RateLimit(value = 2.0, type = "login")
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request.account(), request.password()));
}

@Around("@annotation(com.dormpower.annotation.RateLimit)")
public Object handleApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
    RateLimit rateLimit = method.getAnnotation(RateLimit.class);
    String limitKey = RedisRateLimiter.apiKey(rateLimit.type());
    if (!redisRateLimiter.tryAcquire(limitKey, 1, (long) rateLimit.value(), 1000)) {
        throw new RuntimeException("请求过于频繁，请稍后再试");
    }
    return joinPoint.proceed();
}
```

### 5.2 技术深度问题

#### Q8: Spring Security 的过滤器链是如何工作的？

**答案**：

Spring Security 通过 `FilterChainProxy` 代理所有安全过滤器：

1. **过滤器链**：
   - SecurityContextPersistenceFilter
   - HeaderWriterFilter
   - CorsFilter
   - CsrfFilter
   - LogoutFilter
   - UsernamePasswordAuthenticationFilter
   - FilterSecurityInterceptor

2. **执行流程**：
   - HTTP 请求到达
   - FilterChainProxy 拦截
   - 匹配 SecurityFilterChain
   - 依次执行过滤器
   - 到达 Servlet/Controller

3. **自定义过滤器**：
   - 继承 OncePerRequestFilter
   - 在 SecurityConfig 中注册
   - 指定插入位置

#### Q9: JWT 和 Session 认证的区别？

**答案**：

| 对比维度 | JWT | Session |
|----------|-----|---------|
| **状态** | 无状态 | 有状态 |
| **存储** | 客户端 | 服务端 |
| **扩展性** | 易扩展 | 需要Session共享 |
| **安全性** | Token 泄露风险 | Session 劫持风险 |
| **适用场景** | 微服务、API | 传统Web应用 |

我们项目选择 JWT 的原因：
- 前后端分离架构
- 支持分布式部署
- 减少服务端存储压力

#### Q10: 如何防止 JWT 被盗用？

**答案**：

我们通过多种方式防止 JWT 被盗用：

1. **HTTPS 传输**：
   - 所有请求使用 HTTPS
   - 防止中间人攻击

2. **短期有效期**：
   - Token 有效期 24 小时
   - 定期刷新 Token

3. **黑名单机制**：
   - 用户登出时加入黑名单
   - 异常登录时强制登出

4. **IP 绑定**：
   - 可选：Token 绑定 IP
   - IP 变化时要求重新登录

5. **安全存储**：
   - 前端使用 HttpOnly Cookie
   - 避免存储在 localStorage

---

## 总结

本文档详细介绍了宿舍电源管理系统中 Spring Security 的使用：

1. **为什么用**：完善的认证机制、灵活的权限控制、多层次安全防护
2. **怎么用**：JWT 认证、RBAC 权限模型、密码加密、限流保护
3. **面试要点**：项目实战问题、技术深度问题

通过实际项目实践，深入理解 Spring Security 的应用场景和实现原理，为面试和实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
