"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
import { SafetyOutlined, UserOutlined, LockOutlined, ArrowLeftOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

type ChangePasswordFormValues = {
  account: string;
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

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

export default function ChangePasswordPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm<ChangePasswordFormValues>();

  const handleChangePassword = async (values: ChangePasswordFormValues) => {
    setLoading(true);
    try {
      await postJSON<{ ok: boolean; message: string }>("/api/auth/change-password", {
        account: values.account,
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success("密码修改成功，请重新登录");
      router.push("/login");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "密码修改失败");
    } finally {
      setLoading(false);
    }
  };

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
              <SafetyOutlined style={{ fontSize: 28, color: "#00d4ff" }} />
            </div>
            <Title level={3} style={{ 
              margin: 0,
              color: "#e8f4ff",
              textShadow: "0 0 10px rgba(0, 212, 255, 0.5)",
            }}>
              修改密码
            </Title>
            <Text style={{ color: "#8ba3c7", fontSize: 14 }}>
              更新您的 Dorm Power 账户密码
            </Text>
          </div>
          
          <div
            style={{
              padding: "12px 16px",
              background: "rgba(255, 184, 0, 0.05)",
              border: "1px solid rgba(255, 184, 0, 0.15)",
              borderRadius: 8,
              display: "flex",
              alignItems: "center",
              gap: 8,
            }}
          >
            <SafetyOutlined style={{ color: "#ffb800" }} />
            <Text style={{ color: "#8ba3c7", fontSize: 13 }}>
              请输入您的账户信息和旧密码以修改密码
            </Text>
          </div>
          
          <Form
            form={form}
            layout="vertical"
            onFinish={handleChangePassword}
          >
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>账户</span>} 
              name="account" 
              rules={[{ required: true, message: "请输入用户名或邮箱" }]}
            >
              <Input 
                prefix={<UserOutlined style={{ color: "#8ba3c7" }} />}
                placeholder="用户名或邮箱"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>旧密码</span>} 
              name="oldPassword" 
              rules={[{ required: true, message: "请输入旧密码" }]}
            >
              <Input.Password 
                prefix={<LockOutlined style={{ color: "#8ba3c7" }} />}
                placeholder="请输入旧密码"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>新密码</span>} 
              name="newPassword" 
              rules={[
                { required: true, message: "请输入新密码" },
                { min: 6, message: "密码至少6个字符" },
              ]}
            >
              <Input.Password 
                prefix={<LockOutlined style={{ color: "#8ba3c7" }} />}
                placeholder="请输入新密码"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            <Form.Item 
              label={<span style={{ color: "#e8f4ff" }}>确认新密码</span>} 
              name="confirmNewPassword" 
              dependencies={["newPassword"]}
              rules={[
                { required: true, message: "请确认新密码" },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue("newPassword") === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error("两次输入的密码不一致"));
                  },
                }),
              ]}
            >
              <Input.Password 
                prefix={<LockOutlined style={{ color: "#8ba3c7" }} />}
                placeholder="请再次输入新密码"
                style={{
                  background: "rgba(16, 24, 40, 0.6)",
                  border: "1px solid rgba(0, 212, 255, 0.2)",
                  color: "#e8f4ff",
                  height: 44,
                }}
              />
            </Form.Item>
            
            <Button 
              htmlType="submit" 
              type="primary" 
              block
              loading={loading}
              style={{
                height: 44,
                background: "linear-gradient(135deg, #00d4ff 0%, #0099ff 100%)",
                border: "none",
                fontSize: 16,
                fontWeight: 600,
                boxShadow: "0 0 20px rgba(0, 212, 255, 0.4)",
              }}
            >
              确认修改
            </Button>
          </Form>
          
          <div style={{ textAlign: "center", marginTop: 16 }}>
            <Button 
              type="link" 
              onClick={() => router.push("/login")}
              style={{ color: "#00d4ff" }}
              icon={<ArrowLeftOutlined />}
            >
              返回登录
            </Button>
          </div>
          
          <div style={{ textAlign: "center", marginTop: 8 }}>
            <Text style={{ color: "#5a6a7a", fontSize: 12 }}>
              Dorm Power Console v1.0.3
            </Text>
          </div>
        </Space>
      </Card>
    </div>
  );
}
