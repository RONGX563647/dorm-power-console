"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Layout, Menu, Tag, Typography } from "antd";
import {
  DashboardOutlined,
  AppstoreOutlined,
  LineChartOutlined,
  RobotOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";

const { Sider } = Layout;
const { Text } = Typography;

/**
 * 侧边栏导航组件
 * 
 * 提供应用程序的主要导航菜单，包括总览、设备、实时、历史和AI分析等功能。
 * 根据当前路径高亮显示活动菜单项。
 */
export default function Sidebar() {
  // 获取当前路径名，用于确定活动菜单项
  const pathname = usePathname();

  // 根据当前路径确定选中的菜单项
  const selectedKey =
    pathname.startsWith("/devices") ? "/devices" :
    pathname.startsWith("/live") ? "/live" :
    pathname.startsWith("/history") ? "/history" :
    pathname.startsWith("/ai") ? "/ai" :
    "/dashboard";

  return (
    <Sider
      width={250}
      breakpoint="lg"
      collapsedWidth={78}
      theme="dark"
      style={{
        background: "#001529",
        borderRight: "1px solid rgba(255,255,255,0.08)",
        boxShadow: "0 16px 36px rgba(0,0,0,0.28)",
      }}
    >
      {/* 应用标题区域 */}
      <div
        style={{
          minHeight: 84,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          gap: 4,
          padding: "0 16px",
          borderBottom: "1px solid rgba(255,255,255,0.12)",
        }}
      >
        <Text strong style={{ color: "#fff", fontSize: 18, letterSpacing: 0.3 }}>Dorm Power</Text>
        <Text style={{ color: "rgba(255,255,255,0.72)", fontSize: 12 }}>Realtime Energy Command Deck</Text>
        <Tag color="blue" style={{ width: "fit-content", marginTop: 2, borderRadius: 999 }}>Campus Beta</Tag>
      </div>
      {/* 导航菜单 */}
      <Menu
        mode="inline"
        theme="dark"
        selectedKeys={[selectedKey]}
        style={{ borderInlineEnd: 0, marginTop: 8 }}
        items={[
          { key: "/dashboard", icon: <DashboardOutlined />, label: <Link href="/dashboard">总览</Link> },
          { key: "/devices", icon: <AppstoreOutlined />, label: <Link href="/devices">设备</Link> },
          { key: "/live", icon: <ThunderboltOutlined />, label: <Link href="/live">实时</Link> },
          { key: "/history", icon: <LineChartOutlined />, label: <Link href="/history">历史</Link> },
          { key: "/ai", icon: <RobotOutlined />, label: <Link href="/ai">AI 分析</Link> },
        ]}
      />
    </Sider>
  );
}
