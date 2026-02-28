import "./globals.css";
import type { Metadata } from "next";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import { ConfigProvider } from "antd";
import { AuthProvider } from "@/components/AuthProvider";
import { RoleProvider } from "@/components/RoleProvider";

export const metadata: Metadata = {
  title: "Dorm Power Console",
  description: "Smart power strip console",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <AntdRegistry>
          <ConfigProvider
            theme={{
              token: {
                colorPrimary: "#1677FF",
                colorSuccess: "#52C41A",
                colorWarning: "#FAAD14",
                colorError: "#FF4D4F",
                borderRadius: 8,
                boxShadowTertiary: "0 1px 2px rgba(0,0,0,0.05)",
                fontSize: 14,
              },
            }}
          >
            <AuthProvider>
              <RoleProvider>{children}</RoleProvider>
            </AuthProvider>
          </ConfigProvider>
        </AntdRegistry>
      </body>
    </html>
  );
}
