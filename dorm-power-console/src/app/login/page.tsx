"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
import { useAuth } from "@/components/AuthProvider";

const { Title, Text } = Typography;

/**
 * 登录页面组件
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
    <div style={{ minHeight: "100vh", display: "grid", placeItems: "center", background: "linear-gradient(135deg, #eaf6ff 0%, #f8fbff 100%)" }}>
      <Card style={{ width: 400, borderRadius: 14 }}>
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          {/* 登录表单标题 */}
          <Title level={3} style={{ margin: 0 }}>Dorm Power Login</Title>
          {/* 登录提示信息 */}
          <Text type="secondary">Only one administrator account is enabled.</Text>
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
            <Form.Item label="Account (username or email)" name="account" rules={[{ required: true }]}>
              <Input placeholder="admin or admin@dorm.local" />
            </Form.Item>
            {/* 密码输入框 */}
            <Form.Item label="Password" name="password" rules={[{ required: true }]}>
              <Input.Password placeholder="Enter password" />
            </Form.Item>
            {/* 登录按钮 */}
            <Button htmlType="submit" type="primary" block>Login</Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
}

