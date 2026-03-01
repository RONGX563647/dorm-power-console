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
 * 侧边栏导航组件 - 科技风深蓝配色
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
        background: "linear-gradient(180deg, #0d1525 0%, #0a0f1a 100%)",
        borderRight: "1px solid rgba(0, 212, 255, 0.15)",
        boxShadow: "0 0 30px rgba(0, 0, 0, 0.5), inset -1px 0 0 rgba(0, 212, 255, 0.1)",
      }}
    >
      {/* 应用标题区域 - 科技风 */}
      <div
        style={{
          minHeight: 84,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          gap: 4,
          padding: "0 16px",
          borderBottom: "1px solid rgba(0, 212, 255, 0.15)",
          background: "linear-gradient(180deg, rgba(0, 212, 255, 0.05) 0%, transparent 100%)",
          position: "relative",
        }}
      >
        {/* 顶部发光线条 */}
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: "1px",
            background: "linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.5), transparent)",
          }}
        />
        <Text strong style={{ 
          color: "#00d4ff", 
          fontSize: 18, 
          letterSpacing: 0.5,
          textShadow: "0 0 10px rgba(0, 212, 255, 0.5)",
        }}>
          Dorm Power
        </Text>
        <Text style={{ color: "rgba(139, 163, 199, 0.8)", fontSize: 12 }}>
          Realtime Energy Command Deck
        </Text>
        <Tag 
          style={{ 
            width: "fit-content", 
            marginTop: 2, 
            borderRadius: 999,
            background: "rgba(0, 212, 255, 0.1)",
            border: "1px solid rgba(0, 212, 255, 0.3)",
            color: "#00d4ff",
          }}
        >
          Campus Beta
        </Tag>
      </div>
      
      {/* 导航菜单 - 科技风 */}
      <Menu
        mode="inline"
        theme="dark"
        selectedKeys={[selectedKey]}
        style={{ 
          borderInlineEnd: 0, 
          marginTop: 8,
          background: "transparent",
        }}
        items={[
          { 
            key: "/dashboard", 
            icon: <DashboardOutlined style={{ color: selectedKey === "/dashboard" ? "#00d4ff" : "#8ba3c7" }} />, 
            label: <Link href="/dashboard" style={{ color: selectedKey === "/dashboard" ? "#00d4ff" : "#e8f4ff" }}>总览</Link> 
          },
          { 
            key: "/devices", 
            icon: <AppstoreOutlined style={{ color: selectedKey === "/devices" ? "#00d4ff" : "#8ba3c7" }} />, 
            label: <Link href="/devices" style={{ color: selectedKey === "/devices" ? "#00d4ff" : "#e8f4ff" }}>设备</Link> 
          },
          { 
            key: "/live", 
            icon: <ThunderboltOutlined style={{ color: selectedKey === "/live" ? "#00d4ff" : "#8ba3c7" }} />, 
            label: <Link href="/live" style={{ color: selectedKey === "/live" ? "#00d4ff" : "#e8f4ff" }}>实时</Link> 
          },
          { 
            key: "/history", 
            icon: <LineChartOutlined style={{ color: selectedKey === "/history" ? "#00d4ff" : "#8ba3c7" }} />, 
            label: <Link href="/history" style={{ color: selectedKey === "/history" ? "#00d4ff" : "#e8f4ff" }}>历史</Link> 
          },
          { 
            key: "/ai", 
            icon: <RobotOutlined style={{ color: selectedKey === "/ai" ? "#00d4ff" : "#8ba3c7" }} />, 
            label: <Link href="/ai" style={{ color: selectedKey === "/ai" ? "#00d4ff" : "#e8f4ff" }}>AI 分析</Link> 
          },
        ]}
      />
      
      {/* 底部装饰 */}
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          height: "100px",
          background: "linear-gradient(180deg, transparent 0%, rgba(0, 212, 255, 0.02) 100%)",
          pointerEvents: "none",
        }}
      />
    </Sider>
  );
}
