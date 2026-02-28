"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";

/**
 * 认证用户类型
 * 定义了系统中用户的基本信息
 */
type AuthUser = {
  username: string; // 用户名
  email: string; // 邮箱
  role: "admin"; // 用户角色，目前仅支持admin
};

/**
 * 认证上下文值类型
 * 定义了认证上下文提供的所有属性和方法
 */
type AuthContextValue = {
  ready: boolean; // 认证状态是否已就绪
  isAuthenticated: boolean; // 用户是否已认证
  token: string | null; // 认证令牌
  user: AuthUser | null; // 当前用户信息
  login: (account: string, password: string) => Promise<void>; // 登录函数
  logout: () => void; // 登出函数
};

// 创建认证上下文
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// 本地存储键名
const TOKEN_KEY = "dorm_auth_token"; // 令牌存储键
const USER_KEY = "dorm_auth_user"; // 用户信息存储键

/**
 * 发送JSON POST请求
 * 
 * @param url 请求URL
 * @param body 请求体
 * @returns 响应数据
 * @throws 当请求失败时抛出错误
 */
async function postJSON<T>(url: string, body: Record<string, unknown>): Promise<T> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg = typeof data?.message === "string" ? data.message : "request failed";
    throw new Error(msg);
  }
  return data as T;
}

/**
 * 认证提供者组件
 * 
 * 管理应用程序的认证状态，提供登录和登出功能。
 * 使用React Context API在组件树中共享认证状态。
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  // 认证状态是否已就绪
  const [ready, setReady] = useState(false);
  // 认证令牌
  const [token, setToken] = useState<string | null>(null);
  // 当前用户信息
  const [user, setUser] = useState<AuthUser | null>(null);

  // 组件挂载时从本地存储恢复认证状态
  useEffect(() => {
    try {
      const cachedToken = localStorage.getItem(TOKEN_KEY);
      const cachedUser = localStorage.getItem(USER_KEY);
      if (cachedToken) setToken(cachedToken);
      if (cachedUser) setUser(JSON.parse(cachedUser) as AuthUser);
    } catch {
      // 恢复失败时清除本地存储
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } finally {
      // 标记认证状态为就绪
      setReady(true);
    }
  }, []);

  /**
 * 用户登录函数
 * 
 * @param account 用户名或邮箱
 * @param password 密码
 */
  const login = async (account: string, password: string) => {
    const data = await postJSON<{ token: string; user: AuthUser }>("/api/auth/login", { account, password });
    // 将令牌和用户信息保存到本地存储
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    // 更新状态
    setToken(data.token);
    setUser(data.user);
  };

  /**
 * 用户登出函数
 * 调用后端登出接口并清除本地存储和状态中的认证信息
 */
  const logout = async () => {
    try {
      // 调用后端登出接口
      await fetch("/api/auth/logout", { method: "POST" });
    } catch {
      // 忽略登出接口错误，继续清除本地状态
    } finally {
      // 清除本地存储和状态
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      setToken(null);
      setUser(null);
    }
  };

  // 创建上下文值，使用useMemo优化性能
  const value = useMemo<AuthContextValue>(() => ({
    ready,
    isAuthenticated: Boolean(token && user),
    token,
    user,
    login,
    logout,
  }), [ready, token, user, login, logout]);

  // 提供上下文给子组件
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * 使用认证上下文的钩子
 * 
 * @returns 认证上下文值
 * @throws 当在AuthProvider外部使用时抛出错误
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
