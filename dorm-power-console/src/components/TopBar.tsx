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
 * 顶部栏组件 - 科技风深蓝配色
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
      <div style={{ minWidth: 280, background: "rgba(16, 24, 40, 0.95)", padding: "12px" }}>
        {notices.map((n, index) => (
          <div 
            key={n.id} 
            style={{ 
              padding: "10px 0", 
              borderBottom: index < notices.length - 1 ? "1px solid rgba(0, 212, 255, 0.1)" : "none",
              color: "#e8f4ff",
            }}
          >
            <Text style={{ color: "#e8f4ff" }}>{n.text}</Text>
          </div>
        ))}
      </div>
    ),
    [],
  );

  return (
    <Header
      style={{
        background: "rgba(22, 32, 52, 0.9)",
        padding: "0 20px",
        margin: "16px 16px 0",
        borderRadius: 12,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        border: "1px solid rgba(0, 212, 255, 0.2)",
        boxShadow: "0 0 20px rgba(0, 212, 255, 0.1), 0 10px 24px rgba(0, 0, 0, 0.3)",
        backdropFilter: "blur(10px)",
        position: "relative",
        overflow: "hidden",
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
      
      {/* 左侧：页面标题区域 */}
      <Space>
        <ThunderboltOutlined style={{ 
          color: "#00d4ff", 
          fontSize: 18,
          filter: "drop-shadow(0 0 5px rgba(0, 212, 255, 0.5))",
        }} />
        <Text strong style={{ 
          fontSize: 18, 
          letterSpacing: 0.5,
          color: "#e8f4ff",
          textShadow: "0 0 10px rgba(0, 212, 255, 0.3)",
        }}>
          {title}
        </Text>
      </Space>
      
      {/* 右侧：用户信息和系统状态区域 */}
      <Space size={12} wrap>
        {/* 管理员用户名 */}
        <Tag style={{ 
          borderRadius: 999,
          background: "rgba(0, 212, 255, 0.1)",
          border: "1px solid rgba(0, 212, 255, 0.3)",
          color: "#00d4ff",
        }}>
          管理员 {user?.username ?? ""}
        </Tag>
        
        {/* MQTT连接状态 */}
        <Tag 
          icon={<CheckCircleOutlined />} 
          style={{ 
            borderRadius: 999,
            background: "rgba(0, 230, 118, 0.1)",
            border: "1px solid rgba(0, 230, 118, 0.3)",
            color: "#00e676",
          }}
        >
          MQTT 已连接
        </Tag>
        
        {/* 系统延迟 */}
        <Tag 
          icon={<WarningOutlined />} 
          style={{ 
            borderRadius: 999,
            background: "rgba(0, 153, 255, 0.1)",
            border: "1px solid rgba(0, 153, 255, 0.3)",
            color: "#0099ff",
          }}
        >
          延迟 120ms
        </Tag>
        
        {/* 丢包率 */}
        <Tag style={{ 
          borderRadius: 999,
          background: "rgba(139, 163, 199, 0.1)",
          border: "1px solid rgba(139, 163, 199, 0.3)",
          color: "#8ba3c7",
        }}>
          丢包率 0%
        </Tag>
        
        {/* 通知中心 */}
        <Popover 
          placement="bottomRight" 
          title={<span style={{ color: "#e8f4ff" }}>通知中心</span>} 
          content={noticeContent}
          overlayStyle={{ 
            background: "rgba(16, 24, 40, 0.95)",
            border: "1px solid rgba(0, 212, 255, 0.2)",
            borderRadius: 8,
          }}
        >
          <Badge count={2} size="small" style={{ 
            "--antd-badge-color": "#ff4757",
          } as React.CSSProperties}>
            <BellOutlined style={{ 
              fontSize: 18, 
              cursor: "pointer", 
              color: "#00d4ff",
              filter: "drop-shadow(0 0 3px rgba(0, 212, 255, 0.5))",
            }} />
          </Badge>
        </Popover>
        
        {/* 版本标签 */}
        <Tag style={{ 
          borderRadius: 999,
          background: "rgba(0, 212, 255, 0.15)",
          border: "1px solid rgba(0, 212, 255, 0.4)",
          color: "#00d4ff",
          fontWeight: 600,
        }}>
          Design Rev.B
        </Tag>
        
        {/* 当前时间 */}
        <Text style={{ color: "#8ba3c7" }}>{dayjs().format("YYYY-MM-DD HH:mm")}</Text>
        
        {/* 退出按钮 */}
        <Button 
          size="small" 
          onClick={logout}
          style={{
            background: "rgba(255, 71, 87, 0.1)",
            border: "1px solid rgba(255, 71, 87, 0.3)",
            color: "#ff4757",
          }}
        >
          退出
        </Button>
      </Space>
    </Header>
  );
}
