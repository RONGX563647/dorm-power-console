/**
 * 用户数据库工具类
 * 
 * 提供用户注册、登录、修改密码等功能。
 * 使用内存数据库模拟后端服务，仅用于演示目的。
 */

export type User = {
  id: string;
  username: string;
  email: string;
  password: string;
  role: "admin" | "user";
  createdAt: number;
  updatedAt: number;
};

type UserStore = {
  users: Map<string, User>;
  usernameIndex: Map<string, string>;
  emailIndex: Map<string, string>;
};

let store: UserStore | null = null;

function getStore(): UserStore {
  if (!store) {
    store = {
      users: new Map(),
      usernameIndex: new Map(),
      emailIndex: new Map(),
    };
    initDefaultUsers();
  }
  return store;
}

function initDefaultUsers() {
  const now = Date.now();
  const defaultAdmin: User = {
    id: "user_admin",
    username: "admin",
    email: "admin@dorm.local",
    password: "admin123",
    role: "admin",
    createdAt: now,
    updatedAt: now,
  };
  
  if (store) {
    store.users.set(defaultAdmin.id, defaultAdmin);
    store.usernameIndex.set(defaultAdmin.username, defaultAdmin.id);
    store.emailIndex.set(defaultAdmin.email, defaultAdmin.id);
  }
}

function generateUserId(): string {
  return `user_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

function hashPassword(password: string): string {
  return password;
}

function verifyPassword(password: string, hashedPassword: string): boolean {
  return password === hashedPassword;
}

export function createUser(
  username: string,
  email: string,
  password: string,
  role: "admin" | "user" = "user"
): { success: boolean; user?: User; error?: string } {
  const db = getStore();
  
  if (db.usernameIndex.has(username)) {
    return { success: false, error: "Username already exists" };
  }
  
  if (db.emailIndex.has(email)) {
    return { success: false, error: "Email already exists" };
  }
  
  const now = Date.now();
  const user: User = {
    id: generateUserId(),
    username,
    email,
    password: hashPassword(password),
    role,
    createdAt: now,
    updatedAt: now,
  };
  
  db.users.set(user.id, user);
  db.usernameIndex.set(user.username, user.id);
  db.emailIndex.set(user.email, user.id);
  
  const { password: _, ...userWithoutPassword } = user;
  return { success: true, user: userWithoutPassword as User };
}

export function authenticateUser(
  account: string,
  password: string
): { success: boolean; user?: User; token?: string; error?: string } {
  const db = getStore();
  
  let user: User | undefined;
  
  const userIdByUsername = db.usernameIndex.get(account);
  if (userIdByUsername) {
    user = db.users.get(userIdByUsername);
  }
  
  if (!user) {
    const userIdByEmail = db.emailIndex.get(account);
    if (userIdByEmail) {
      user = db.users.get(userIdByEmail);
    }
  }
  
  if (!user) {
    return { success: false, error: "User not found" };
  }
  
  if (!verifyPassword(password, user.password)) {
    return { success: false, error: "Invalid password" };
  }
  
  const token = `token_${user.id}_${Date.now()}_${Math.random().toString(36).substring(2)}`;
  
  const { password: _, ...userWithoutPassword } = user;
  return { 
    success: true, 
    user: userWithoutPassword as User, 
    token 
  };
}

export function changePassword(
  userId: string,
  oldPassword: string,
  newPassword: string
): { success: boolean; error?: string } {
  const db = getStore();
  
  const user = db.users.get(userId);
  if (!user) {
    return { success: false, error: "User not found" };
  }
  
  if (!verifyPassword(oldPassword, user.password)) {
    return { success: false, error: "Invalid old password" };
  }
  
  if (oldPassword === newPassword) {
    return { success: false, error: "New password must be different from old password" };
  }
  
  user.password = hashPassword(newPassword);
  user.updatedAt = Date.now();
  
  return { success: true };
}

export function getUserById(userId: string): User | undefined {
  const db = getStore();
  const user = db.users.get(userId);
  if (user) {
    const { password: _, ...userWithoutPassword } = user;
    return userWithoutPassword as User;
  }
  return undefined;
}

export function getUserByUsername(username: string): User | undefined {
  const db = getStore();
  const userId = db.usernameIndex.get(username);
  if (userId) {
    const user = db.users.get(userId);
    if (user) {
      const { password: _, ...userWithoutPassword } = user;
      return userWithoutPassword as User;
    }
  }
  return undefined;
}

export function getUserByEmail(email: string): User | undefined {
  const db = getStore();
  const userId = db.emailIndex.get(email);
  if (userId) {
    const user = db.users.get(userId);
    if (user) {
      const { password: _, ...userWithoutPassword } = user;
      return userWithoutPassword as User;
    }
  }
  return undefined;
}

export function resetDatabase(): void {
  if (store) {
    store.users.clear();
    store.usernameIndex.clear();
    store.emailIndex.clear();
  }
  store = null;
}
