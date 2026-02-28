"use client";

import { useEffect } from "react";
import { Layout } from "antd";
import { usePathname, useRouter } from "next/navigation";
import Sidebar from "@/components/Sidebar";
import TopBar from "@/components/TopBar";
import { useAuth } from "@/components/AuthProvider";

const { Content } = Layout;

/**
 * 应用布局组件
 * 
 * 提供应用程序的主要布局结构，包括侧边栏、顶部栏和内容区域。
 * 处理路由保护，确保未认证用户被重定向到登录页面。
 */
export default function AppLayout({
  title,
  children,
}: {
  title: string; // 页面标题
  children: React.ReactNode; // 页面内容
}) {
  // Next.js路由钩子，用于页面导航
  const router = useRouter();
  // 当前路径名，用于路由保护
  const pathname = usePathname();
  // 从认证上下文获取认证状态
  const { ready, isAuthenticated } = useAuth();

  // 路由保护：当认证状态就绪且用户未认证时，重定向到登录页面
  useEffect(() => {
    if (!ready) return;
    if (!isAuthenticated && pathname !== "/login") {
      router.replace("/login");
    }
  }, [ready, isAuthenticated, pathname, router]);

  // 如果认证状态未就绪或用户未认证，不渲染内容
  if (!ready || !isAuthenticated) {
    return null;
  }

  // 渲染应用布局
  return (
    <Layout className="shell-layout" style={{ minHeight: "100vh" }}>
      {/* 侧边栏导航 */}
      <Sidebar />
      <Layout>
        {/* 顶部栏 */}
        <TopBar title={title} />
        {/* 内容区域 */}
        <Content className="shell-content">{children}</Content>
      </Layout>
    </Layout>
  );
}
