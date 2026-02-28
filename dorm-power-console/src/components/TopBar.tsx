"use client";

import { useMemo } from "react";
import { Badge, Button, Layout, Popover, Space, Tag, Typography } from "antd";
import { BellOutlined, CheckCircleOutlined, ThunderboltOutlined, WarningOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useAuth } from "@/components/AuthProvider";

const { Header } = Layout;
const { Text } = Typography;

// 通知列表数据
const notices = [
  { id: "n1", level: "warn", text: "插孔 3 功率接近阈值" },
  { id: "n2", level: "info", text: "设备 strip01 刷新成功" },
  { id: "n3", level: "ok", text: "系统服务运行正常" },
];

/**
 * 顶部栏组件
 * 
 * 显示当前页面标题、用户信息、系统状态和通知。
 * 提供退出登录功能。
 */
export default function TopBar({ title }: { title: string }) {
  // 从认证上下文获取用户信息和登出函数
  const { user, logout } = useAuth();

  // 通知内容组件，使用useMemo优化性能
  const noticeContent = useMemo(
    () => (
      <div style={{ minWidth: 280 }}>
        {notices.map((n) => (
          <div key={n.id} style={{ padding: "8px 0", borderBottom: "1px solid #f0f4f8" }}>
            <Text>{n.text}</Text>
          </div>
        ))}
      </div>
    ),
    [],
  );

  return (
    <Header
      style={{
        background: "var(--panel-strong)",
        padding: "0 16px",
        margin: "16px 16px 0",
        borderRadius: 14,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        border: "1px solid rgba(13, 108, 145, 0.18)",
        boxShadow: "var(--shadow-md)",
      }}
    >
      {/* 左侧：页面标题区域 */}
      <Space>
        <ThunderboltOutlined style={{ color: "var(--brand-1)", fontSize: 16 }} />
        <Text strong style={{ fontSize: 18, letterSpacing: 0.2 }}>{title}</Text>
      </Space>
      {/* 右侧：用户信息和系统状态区域 */}
      <Space size={10} wrap>
        {/* 管理员用户名 */}
        <Tag color="blue" style={{ borderRadius: 999 }}>
          管理员 {user?.username ?? ""}
        </Tag>
        {/* MQTT连接状态 */}
        <Tag icon={<CheckCircleOutlined />} color="success" style={{ borderRadius: 999 }}>MQTT 已连接</Tag>
        {/* 系统延迟 */}
        <Tag icon={<WarningOutlined />} color="processing" style={{ borderRadius: 999 }}>延迟 120ms</Tag>
        {/* 丢包率 */}
        <Tag color="default" style={{ borderRadius: 999 }}>丢包率 0%</Tag>
        {/* 通知中心 */}
        <Popover placement="bottomRight" title="通知中心" content={noticeContent}>
          <Badge count={2} size="small">
            <BellOutlined style={{ fontSize: 18, cursor: "pointer", color: "#2e5874" }} />
          </Badge>
        </Popover>
        {/* 版本标签 */}
        <Tag color="cyan" style={{ borderRadius: 999 }}>Design Rev.B</Tag>
        {/* 当前时间 */}
        <Text type="secondary">{dayjs().format("YYYY-MM-DD HH:mm")}</Text>
        {/* 退出按钮 */}
        <Button size="small" onClick={logout}>退出</Button>
      </Space>
    </Header>
  );
}
