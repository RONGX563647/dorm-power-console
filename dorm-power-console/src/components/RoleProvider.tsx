"use client";

import { createContext, useContext, useMemo, useState } from "react";

export type UserRole = "admin";

type RoleContextValue = {
  role: UserRole;
  setRole: (role: UserRole) => void;
  canControl: boolean;
  canGlobalPolicy: boolean;
};

const RoleContext = createContext<RoleContextValue | undefined>(undefined);

export function RoleProvider({ children }: { children: React.ReactNode }) {
  const [role] = useState<UserRole>("admin");

  const value = useMemo<RoleContextValue>(() => ({
    role,
    setRole: () => undefined,
    canControl: true,
    canGlobalPolicy: true,
  }), [role]);

  return <RoleContext.Provider value={value}>{children}</RoleContext.Provider>;
}

export function useRole() {
  const ctx = useContext(RoleContext);
  if (!ctx) {
    throw new Error("useRole must be used within RoleProvider");
  }
  return ctx;
}
