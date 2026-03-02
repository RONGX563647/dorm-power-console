package com.dormpower.service;

import com.dormpower.model.Permission;
import com.dormpower.model.Role;
import com.dormpower.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户详情服务
 * 用于Spring Security认证
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private RbacService rbacService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.dormpower.model.UserAccount userAccount = userAccountRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        if (!userAccount.isEnabled()) {
            throw new UsernameNotFoundException("User is disabled: " + username);
        }
        
        List<GrantedAuthority> authorities = getUserAuthorities(username);
        
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
