"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
import { ThunderboltOutlined, SafetyOutlined } from "@ant-design/icons";
import { useAuth } from "@/components/AuthProvider";

const { Title, Text } = Typography;

/**
 * 登录页面组件 - 科技风深蓝配色
 * 
 * 提供用户登录界面，验证用户凭据并跳转到仪表板。
 * 使用Ant Design的Form组件处理表单验证和提交。
 */
export default function LoginPage() {
  // Next.js路由钩子，用于页面导航
  const router = useRouter();
  // 从认证上下文获取登录函数和认证状态
  const { login, isAuthenticated, ready } = useAuth();
  // 创建表单实例，用于管理表单状态和验证
  const [form] = Form.useForm<{ account: string; password: string }>();

  // 当认证状态就绪且已认证时，自动跳转到仪表板
  useEffect(() => {
    if (ready && isAuthenticated) {
      router.replace("/dashboard");
    }
  }, [ready, isAuthenticated, router]);

  // 如果已认证，不渲染登录表单
  if (ready && isAuthenticated) return null;

  // 渲染登录表单
  return (
    <div 
      style={{ 
        minHeight: "100vh", 
        display: "grid", 
        placeItems: "center", 
        background: `
          radial-gradient(ellipse 80% 50% at 50% -20%, rgba(0, 153, 255, 0.15), transparent),
          radial-gradient(ellipse 60% 40% at 80% 80%, rgba(0, 212, 255, 0.08), transparent),
          linear-gradient(180deg, #0a0f1a 0%, #0d1525 50%, #0a0f1a 100%)
        `,
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* 背景装饰 */}
      <div
        style={{
          position: "absolute",
          top: "10%",
          left: "10%",
          width: "300px",
          height: "300px",
          background: "radial-gradient(circle, rgba(0, 212, 255, 0.1) 0%, transparent 70%)",
          borderRadius: "50%",
          filter: "blur(40px)",
        }}
      />
      <div
        style={{
          position: "absolute",
          bottom: "10%",
          right: "10%",
          width: "400px",
          height: "400px",
          background: "radial-gradient(circle, rgba(0, 102, 204, 0.1) 0%, transparent 70%)",
          borderRadius: "50%",
          filter: "blur(60px)",
        }}
      />
      
      <Card 
        style={{ 
          width: 420, 
          borderRadius: 16,
          background: "rgba(16, 24, 40, 0.9)",
          border: "1px solid rgba(0, 212, 255, 0.2)",
          boxShadow: "0 0 40px rgba(0, 212, 255, 0.15), 0 20px 50px rgba(0, 0, 0, 0.4)",
          backdropFilter: "blur(10px)",
          position: "relative",
          overflow: "hidden",
        }}
        styles={{ body: { padding: "32px" } }}
      >
        {/* 顶部发光线条 */}
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: "2px",
            background: "linear-gradient(90deg, transparent, #00d4ff, #0099ff, #00d4ff, transparent)",
          }}
        />
        
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          {/* Logo和标题区域 */}
          <div style={{ textAlign: "center", marginBottom: 8 }}>
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: "50%",
                background: "linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 153, 255, 0.2) 100%)",
                border: "1px solid rgba(0, 212, 255, 0.3)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                margin: "0 auto 16px",
                boxShadow: "0 0 20px rgba(0, 212, 255, 0.3)",
              }}
            >
              <ThunderboltOutlined style={{ fontSize: 28, color: "#00d4ff" }} />
            </div>
            <Title level={3} style={{ 
              margin: 0,
              color: "#e8f4ff",
              textShadow: "0 0 10px rgba(0, 212, 255, 0.5)",
            }}>
              Dorm Power
            </Title>
            <Text style={{ color: "#8ba3c7", fontSize: 14 }}>
              Smart Energy Management System
            </Text>
          </div>
          
          {/* 登录提示信息 */}
          <div
            style={{
              padding: "12px 16px",
              background: "rgba(0, 212, 255, 0.05)",
              border: "1px solid rgba(0, 212, 255, 0.15)",
              borderRadius: 8,
              display: "flex",
              alignItems: "center",
              gap: 8,
            }}
          >
            <SafetyOutlined style={{ color: "#00d4ff" }} />
            <Text style={{ color: "#8ba3c7", fontSize: 13 }}>
              Only one administrator account is enabled.
            </Text>
          </div>
          
          {/* 登录表单 */}
          <Form
            form={form}
            layout="vertical"
            onFinish={async (values) => {
              try {
                // 调用登录函数验证凭据
                await login(values.account, values.password);
                // 登录成功提示
                message.success("Login success");
                // 跳转到仪表板
                router.replace("/dashboard");
              } catch (error) {
                // 登录失败提示
                message.error(error instanceof Error ? error.message : "Login failed");
              }
            }}
          >
            {/* 账户输入框 */}
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>Account</span>} 
              name="account" 
              rules={[{ required: true, message: "Please enter account" }]}
            >
              <Input 
                placeholder="admin or admin@dorm.local"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            {/* 密码输入框 */}
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>Password</span>} 
              name="password" 
              rules={[{ required: true, message: "Please enter password" }]}
            >
              <Input.Password 
                placeholder="Enter password"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            {/* 登录按钮 */}
            <Button 
              htmlType="submit" 
              type="primary" 
              block
              style={{
                height: 44,
                background: "linear-gradient(135deg, #00d4ff 0%, #0099ff 100%)",
                border: "none",
                fontSize: 16,
                fontWeight: 600,
                boxShadow: "0 0 20px rgba(0, 212, 255, 0.4)",
              }}
            >
              Login
            </Button>
          </Form>
          
          {/* 底部版权信息 */}
          <div style={{ textAlign: "center", marginTop: 16 }}>
            <Text style={{ color: "#5a6a7a", fontSize: 12 }}>
              Dorm Power Console v1.0.3
            </Text>
          </div>
        </Space>
      </Card>
    </div>
  );
}
