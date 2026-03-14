# Spring Security 学习文档

## 目录
- [1. Spring Security 基础概念](#1-spring-security-基础概念)
- [2. Spring Security 核心组件](#2-spring-security-核心组件)
- [3. Spring Security 认证机制](#3-spring-security-认证机制)
- [4. Spring Security 授权机制](#4-spring-security-授权机制)
- [5. Spring Security 常见应用场景](#5-spring-security-常见应用场景)
- [6. Spring Security 原理分析](#6-spring-security-原理分析)
- [7. Spring Security 面试要点](#7-spring-security-面试要点)

---

## 1. Spring Security 基础概念

### 1.1 什么是 Spring Security

Spring Security 是一个功能强大且高度可定制的身份验证和访问控制框架，它是保护基于 Spring 应用程序的事实标准。

### 1.2 Spring Security 核心功能

| 功能 | 说明 |
|------|------|
| **认证（Authentication）** | 验证用户身份（你是谁） |
| **授权（Authorization）** | 验证用户权限（你能做什么） |
| **CSRF 防护** | 防止跨站请求伪造攻击 |
| **Session 管理** | 会话固定保护、并发控制 |
| **CORS 支持** | 跨域资源共享配置 |
| **安全 Headers** | HTTP 安全头配置 |
| **密码加密** | 多种加密算法支持 |
| **Remember Me** | 记住我功能 |

### 1.3 Spring Security 与其他安全框架对比

| 对比维度 | Spring Security | Apache Shiro | JWT |
|----------|----------------|--------------|-----|
| **集成难度** | 中等 | 简单 | 需自己实现 |
| **功能完整性** | 非常完善 | 完善 | 基础 |
| **Spring 集成** | 原生支持 | 需配置 | 需自己集成 |
| **社区支持** | 非常活跃 | 活跃 | 广泛 |
| **学习曲线** | 陡峭 | 平缓 | 平缓 |
| **适用场景** | 企业级应用 | 中小型应用 | 微服务/API |

---

## 2. Spring Security 核心组件

### 2.1 SecurityFilterChain

安全过滤器链是 Spring Security 的核心，由一系列过滤器组成：

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Security 过滤器链                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HTTP 请求                                                  │
│     │                                                       │
│     ▼                                                       │
│  ┌──────────────────────┐                                  │
│  │ SecurityContextPersistenceFilter                        │
│  │ 安全上下文持久化过滤器                                    │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ HeaderWriterFilter   │                                  │
│  │ HTTP头写入过滤器       │                                  │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ CorsFilter           │                                  │
│  │ 跨域过滤器            │                                  │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ CsrfFilter           │                                  │
│  │ CSRF防护过滤器        │                                  │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ LogoutFilter         │                                  │
│  │ 登出过滤器            │                                  │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ UsernamePasswordAuthenticationFilter                   │
│  │ 用户名密码认证过滤器                                     │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│  ┌──────────────────────┐                                  │
│  │ FilterSecurityInterceptor                              │
│  │ 安全拦截器             │                                  │
│  └──────────┬───────────┘                                  │
│             │                                               │
│             ▼                                               │
│        Controller                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Authentication（认证对象）

```java
public interface Authentication extends Principal, Serializable {
    // 获取用户权限集合
    Collection<? extends GrantedAuthority> getAuthorities();
    
    // 获取凭证（通常是密码）
    Object getCredentials();
    
    // 获取详细信息
    Object getDetails();
    
    // 获取主体（通常是用户名）
    Object getPrincipal();
    
    // 是否已认证
    boolean isAuthenticated();
    
    // 设置认证状态
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
}
```

### 2.3 SecurityContext（安全上下文）

```java
// 获取当前安全上下文
SecurityContext context = SecurityContextHolder.getContext();

// 获取当前认证信息
Authentication authentication = context.getAuthentication();

// 获取当前用户名
String username = authentication.getName();

// 获取当前用户权限
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
```

### 2.4 UserDetailsService（用户详情服务）

```java
public interface UserDetailsService {
    // 根据用户名加载用户信息
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

### 2.5 PasswordEncoder（密码编码器）

```java
// BCrypt密码编码器
PasswordEncoder encoder = new BCryptPasswordEncoder();

// 加密密码
String encodedPassword = encoder.encode("password123");

// 验证密码
boolean matches = encoder.matches("password123", encodedPassword);
```

---

## 3. Spring Security 认证机制

### 3.1 认证流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Security 认证流程                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 用户提交认证信息                                         │
│     POST /login {username, password}                        │
│                                                             │
│  2. AuthenticationFilter 拦截请求                           │
│     ┌────────────────────────────────────┐                 │
│     │  创建 UsernamePasswordAuthenticationToken│            │
│     │  (未认证状态)                       │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  3. AuthenticationManager 认证                              │
│     ┌────────────────────────────────────┐                 │
│     │  委托给 AuthenticationProvider      │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  4. AuthenticationProvider 验证                             │
│     ┌────────────────────────────────────┐                 │
│     │  调用 UserDetailsService 加载用户   │                 │
│     │  使用 PasswordEncoder 验证密码      │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  5. 认证成功                                                │
│     ┌────────────────────────────────────┐                 │
│     │  创建已认证的 Authentication        │                 │
│     │  存储到 SecurityContext            │                 │
│     │  返回成功响应                       │                 │
│     └────────────────────────────────────┘                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 表单登录认证

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .formLogin(form -> form
                .loginPage("/login")           // 登录页面
                .loginProcessingUrl("/login")  // 登录处理URL
                .defaultSuccessUrl("/home")    // 登录成功跳转
                .failureUrl("/login?error")    // 登录失败跳转
                .permitAll()
            );
        return http.build();
    }
}
```

### 3.3 HTTP Basic 认证

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic(basic -> basic
                .realmName("My App")
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 3.4 JWT 认证

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 3.5 OAuth2 认证

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/home")
            );
        return http.build();
    }
}
```

---

## 4. Spring Security 授权机制

### 4.1 授权流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Security 授权流程                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 用户访问受保护资源                                       │
│     GET /admin/users                                        │
│                                                             │
│  2. FilterSecurityInterceptor 拦截                          │
│     ┌────────────────────────────────────┐                 │
│     │  检查用户是否已认证                 │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  3. 获取用户权限信息                                         │
│     ┌────────────────────────────────────┐                 │
│     │  从 SecurityContext 获取           │                 │
│     │  Authentication.getAuthorities()   │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  4. AccessDecisionManager 决策                               │
│     ┌────────────────────────────────────┐                 │
│     │  AffirmativeBased: 一票通过        │                 │
│     │  ConsensusBased: 多数通过          │                 │
│     │  UnanimousBased: 全票通过          │                 │
│     └────────────────┬───────────────────┘                 │
│                      │                                      │
│  5. 授权结果                                                │
│     ┌────────────────────────────────────┐                 │
│     │  允许：继续访问资源                 │                 │
│     │  拒绝：抛出 AccessDeniedException  │                 │
│     └────────────────────────────────────┘                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 URL 级别授权

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 公开访问
                .requestMatchers("/", "/home", "/register").permitAll()
                
                // 静态资源
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // 管理员路径
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 用户路径
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 4.3 方法级别授权

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // 配置内容
}

@Service
public class UserService {

    // 要求用户有 ADMIN 角色
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        // 删除用户逻辑
    }

    // 要求用户有 USER 或 ADMIN 角色
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public User getUser(Long userId) {
        return userRepository.findById(userId);
    }

    // 要求用户名匹配
    @PreAuthorize("#username == authentication.name")
    public User getProfile(String username) {
        return userRepository.findByUsername(username);
    }

    // 后置授权：返回值验证
    @PostAuthorize("returnObject.username == authentication.name")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    // 过滤返回结果
    @PostFilter("filterObject.owner == authentication.name")
    public List<Document> getDocuments() {
        return documentRepository.findAll();
    }
}
```

### 4.4 表达式授权

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // hasRole: 有指定角色
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // hasAnyRole: 有任一角色
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                
                // hasAuthority: 有指定权限
                .requestMatchers("/api/write/**").hasAuthority("WRITE")
                
                // hasAnyAuthority: 有任一权限
                .requestMatchers("/api/**").hasAnyAuthority("READ", "WRITE")
                
                // access: 自定义表达式
                .requestMatchers("/special/**")
                    .access("hasRole('ADMIN') and hasIpAddress('192.168.1.0/24')")
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

## 5. Spring Security 常见应用场景

### 5.1 RBAC 权限模型

```
┌─────────────────────────────────────────────────────────────┐
│                      RBAC 权限模型                           │
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
│     │                                                       │
│     │ 多对多                                                │
│     ▼                                                       │
│  资源(Resource)                                             │
│                                                             │
│  示例：                                                     │
│  用户 admin → 角色 ADMIN → 权限 user:delete → 资源 /users   │
│  用户 user1 → 角色 USER → 权限 user:read → 资源 /users      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**实现代码**：

```java
@Entity
public class User {
    @Id
    private String username;
    private String password;
    
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;
}

@Entity
public class Role {
    @Id
    private String code;
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Permission> permissions;
}

@Entity
public class Permission {
    @Id
    private String code;
    private String name;
    private String resource;
    private String action;
}

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 添加角色
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            
            // 添加权限
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
}
```

### 5.2 JWT 无状态认证

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 从请求头获取token
        String token = getTokenFromRequest(request);
        
        // 2. 验证token
        if (token != null && jwtUtil.validateToken(token)) {
            // 3. 解析token获取用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            
            // 4. 加载用户权限
            List<GrantedAuthority> authorities = getAuthorities(username);
            
            // 5. 创建认证对象
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            
            // 6. 设置到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
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
}
```

### 5.3 密码加密存储

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt加密，强度10
        return new BCryptPasswordEncoder(10);
    }
}

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        // 加密密码
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    public boolean login(String username, String password) {
        User user = userRepository.findByUsername(username);
        // 验证密码
        return passwordEncoder.matches(password, user.getPassword());
    }
}
```

### 5.4 自定义认证失败处理

```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request, 
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "未授权：" + authException.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            );
        return http.build();
    }
}
```

### 5.5 自定义权限不足处理

```java
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request, 
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", "权限不足：" + accessDeniedException.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling(exception -> exception
                .accessDeniedHandler(accessDeniedHandler)
            );
        return http.build();
    }
}
```

### 5.6 Session 管理

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                // Session创建策略
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                
                // Session固定保护
                .sessionFixation().migrateSession()
                
                // 最大并发Session数
                .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)  // 踢出之前的登录
                    .expiredUrl("/login?expired")
                
                // Session过期处理
                .and()
                .invalidSessionUrl("/login?invalid")
            );
        return http.build();
    }
}
```

---

## 6. Spring Security 原理分析

### 6.1 过滤器链原理

Spring Security 通过一系列过滤器实现安全控制：

```
┌─────────────────────────────────────────────────────────────┐
│                  过滤器链执行流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HTTP 请求                                                  │
│     │                                                       │
│     ▼                                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  FilterChainProxy                             │          │
│  │  (代理所有安全过滤器)                          │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  SecurityFilterChain                          │          │
│  │  (匹配请求路径的安全过滤器链)                   │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│         ┌───────────┼───────────┐                          │
│         │           │           │                          │
│         ▼           ▼           ▼                          │
│    Filter 1     Filter 2     Filter 3                      │
│         │           │           │                          │
│         └───────────┼───────────┘                          │
│                     │                                       │
│                     ▼                                       │
│              Servlet/Controller                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 认证架构

```
┌─────────────────────────────────────────────────────────────┐
│                    认证架构组件                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────┐          │
│  │  AuthenticationFilter                         │          │
│  │  (认证过滤器)                                 │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  AuthenticationManager                        │          │
│  │  (认证管理器接口)                              │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  ProviderManager                              │          │
│  │  (认证管理器实现)                              │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│         ┌───────────┼───────────┐                          │
│         │           │           │                          │
│         ▼           ▼           ▼                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │Provider 1│ │Provider 2│ │Provider 3│                   │
│  │(表单)    │ │(Basic)   │ │(JWT)     │                   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘                   │
│       │            │            │                          │
│       └────────────┼────────────┘                          │
│                    │                                        │
│                    ▼                                        │
│  ┌──────────────────────────────────────────────┐          │
│  │  UserDetailsService                           │          │
│  │  (用户详情服务)                               │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  PasswordEncoder                              │          │
│  │  (密码编码器)                                 │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 授权架构

```
┌─────────────────────────────────────────────────────────────┐
│                    授权架构组件                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────┐          │
│  │  FilterSecurityInterceptor                    │          │
│  │  (安全拦截器)                                 │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  AccessDecisionManager                        │          │
│  │  (访问决策管理器)                              │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│         ┌───────────┼───────────┐                          │
│         │           │           │                          │
│         ▼           ▼           ▼                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │Voter 1   │ │Voter 2   │ │Voter 3   │                   │
│  │(角色)    │ │(权限)    │ │(IP)      │                   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘                   │
│       │            │            │                          │
│       └────────────┼────────────┘                          │
│                    │                                        │
│                    ▼                                        │
│  ┌──────────────────────────────────────────────┐          │
│  │  ConfigAttribute                              │          │
│  │  (安全配置属性)                               │          │
│  │  如: ROLE_ADMIN, hasRole('USER')             │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.4 SecurityContext 存储

```
┌─────────────────────────────────────────────────────────────┐
│                SecurityContext 存储机制                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────┐          │
│  │  SecurityContextHolder                        │          │
│  │  (安全上下文持有者)                            │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  SecurityContext                              │          │
│  │  (安全上下文)                                 │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────────────┐          │
│  │  Authentication                               │          │
│  │  (认证对象)                                   │          │
│  └──────────────────┬───────────────────────────┘          │
│                     │                                       │
│         ┌───────────┼───────────┐                          │
│         │           │           │                          │
│         ▼           ▼           ▼                          │
│    Principal    Credentials  Authorities                   │
│    (主体)       (凭证)        (权限)                        │
│                                                             │
│  存储策略：                                                 │
│  - MODE_THREADLOCAL: 线程本地存储（默认）                   │
│  - MODE_INHERITABLETHREADLOCAL: 子线程继承                 │
│  - MODE_GLOBAL: 全局存储                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Spring Security 面试要点

### 7.1 基础问题

#### Q1: Spring Security 的核心功能是什么？

**答案**：
1. **认证（Authentication）**：验证用户身份
2. **授权（Authorization）**：验证用户权限
3. **防护功能**：CSRF、Session固定、点击劫持等
4. **集成功能**：OAuth2、JWT、LDAP等

#### Q2: Spring Security 的工作原理？

**答案**：

Spring Security 基于过滤器链工作：

1. **过滤器链**：所有请求经过一系列过滤器
2. **认证过滤器**：验证用户身份
3. **授权过滤器**：验证用户权限
4. **安全上下文**：存储认证信息

核心组件：
- SecurityFilterChain：过滤器链
- AuthenticationManager：认证管理器
- AccessDecisionManager：访问决策管理器
- SecurityContext：安全上下文

#### Q3: Spring Security 的认证流程？

**答案**：

1. 用户提交认证信息
2. AuthenticationFilter 拦截请求
3. 创建未认证的 Authentication 对象
4. AuthenticationManager 委托给 AuthenticationProvider
5. AuthenticationProvider 调用 UserDetailsService 加载用户
6. 使用 PasswordEncoder 验证密码
7. 认证成功，创建已认证的 Authentication 对象
8. 存储到 SecurityContext

### 7.2 高级问题

#### Q4: 如何实现 JWT 认证？

**答案**：

1. **自定义过滤器**：继承 OncePerRequestFilter
2. **解析 Token**：从请求头获取并验证
3. **设置认证信息**：创建 Authentication 并设置到 SecurityContext
4. **配置过滤器链**：在 UsernamePasswordAuthenticationFilter 之前添加

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String token = getTokenFromRequest(request);
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

#### Q5: 如何实现 RBAC 权限模型？

**答案**：

1. **数据模型**：用户-角色-权限-资源
2. **UserDetailsService**：加载用户权限信息
3. **权限配置**：URL 级别和方法级别
4. **权限验证**：通过 AccessDecisionManager

关键代码：
```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username);
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

#### Q6: Spring Security 如何防止 CSRF 攻击？

**答案**：

1. **CSRF Token**：生成随机 Token，表单提交时验证
2. **SameSite Cookie**：设置 Cookie 的 SameSite 属性
3. **Referer 验证**：检查请求来源

Spring Security 默认启用 CSRF 防护：
```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
);
```

对于无状态应用（如 JWT），可以禁用：
```java
http.csrf(csrf -> csrf.disable());
```

#### Q7: 如何处理认证失败和权限不足？

**答案**：

1. **认证失败**：实现 AuthenticationEntryPoint
2. **权限不足**：实现 AccessDeniedHandler

```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(...) {
        response.setStatus(401);
        response.getWriter().write("{\"code\":401,\"message\":\"未授权\"}");
    }
}

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(...) {
        response.setStatus(403);
        response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
    }
}
```

#### Q8: Spring Security 的密码加密方式？

**答案**：

1. **BCrypt**：推荐，自动加盐，可配置强度
2. **PBKDF2**：基于密码的密钥派生函数
3. **SCrypt**：内存密集型哈希
4. **Argon2**：密码哈希竞赛冠军

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);  // 强度10
}
```

#### Q9: 如何实现记住我功能？

**答案**：

```java
http.rememberMe(remember -> remember
    .key("uniqueAndSecret")
    .tokenValiditySeconds(86400)  // 1天
    .rememberMeParameter("remember-me")
    .userDetailsService(userDetailsService)
);
```

原理：
1. 用户登录时勾选"记住我"
2. 服务器生成持久化 Token
3. 存储到 Cookie
4. 下次访问时自动登录

#### Q10: Spring Security 的 Session 管理策略？

**答案**：

1. **IF_REQUIRED**：需要时创建（默认）
2. **ALWAYS**：总是创建
3. **NEVER**：从不创建
4. **STATELESS**：无状态（JWT 推荐）

```java
http.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
);
```

---

## 总结

Spring Security 是一个功能强大的安全框架，掌握其核心原理和应用场景对于构建安全的应用系统至关重要。本文档涵盖了：

1. **基础概念**：认证、授权、核心组件
2. **核心组件**：过滤器链、Authentication、SecurityContext
3. **认证机制**：表单登录、HTTP Basic、JWT、OAuth2
4. **授权机制**：URL 级别、方法级别、表达式授权
5. **应用场景**：RBAC、JWT、密码加密、异常处理
6. **原理分析**：过滤器链、认证架构、授权架构
7. **面试要点**：常见问题和答案

建议结合实际项目实践，深入理解 Spring Security 的使用和原理。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
