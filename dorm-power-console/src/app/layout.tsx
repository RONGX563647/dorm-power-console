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
                colorPrimary: "#00d4ff",
                colorSuccess: "#00e676",
                colorWarning: "#ffb800",
                colorError: "#ff4757",
                colorInfo: "#0099ff",
                colorTextBase: "#e8f4ff",
                colorBgBase: "#0a0f1a",
                colorBorder: "rgba(0, 212, 255, 0.2)",
                borderRadius: 8,
                boxShadowTertiary: "0 0 20px rgba(0, 212, 255, 0.1)",
                fontSize: 14,
              },
              components: {
                Menu: {
                  darkItemBg: "transparent",
                  darkItemSelectedBg: "rgba(0, 212, 255, 0.15)",
                  darkItemSelectedColor: "#ffffff",
                  darkSubMenuItemBg: "transparent",
                },
                Card: {
                  colorBgContainer: "rgba(16, 24, 40, 0.85)",
                  colorBorderSecondary: "rgba(0, 212, 255, 0.1)",
                },
                Table: {
                  colorBgContainer: "transparent",
                  headerBg: "rgba(0, 212, 255, 0.05)",
                  headerColor: "#8ba3c7",
                  rowSelectedBg: "rgba(0, 212, 255, 0.15)",
                  rowSelectedColor: "#ffffff",
                },
                Input: {
                  colorBgContainer: "rgba(16, 24, 40, 0.6)",
                  colorBorder: "rgba(0, 212, 255, 0.2)",
                  activeBorderColor: "#00d4ff",
                },
                Select: {
                  colorBgContainer: "rgba(16, 24, 40, 0.6)",
                  colorBorder: "rgba(0, 212, 255, 0.2)",
                  optionSelectedBg: "rgba(0, 212, 255, 0.15)",
                  optionSelectedColor: "#ffffff",
                },
                Segmented: {
                  colorText: "#8ba3c7",
                  colorTextHover: "#e8f4ff",
                  colorTextSelected: "#ffffff",
                  colorBgSelected: "rgba(0, 212, 255, 0.15)",
                  colorBorder: "rgba(0, 212, 255, 0.2)",
                  colorBorderSelected: "#00d4ff",
                },
                Button: {
                  defaultBg: "rgba(0, 212, 255, 0.1)",
                  defaultBorderColor: "rgba(0, 212, 255, 0.3)",
                  defaultColor: "#00d4ff",
                  primaryColor: "#00d4ff",
                  primaryHoverColor: "#0099ff",
                },
                Tag: {
                  colorText: "#00d4ff",
                },
                Statistic: {
                  colorTextDescription: "#8ba3c7",
                },
                Cascader: {
                  colorBgContainer: "rgba(16, 24, 40, 0.6)",
                  colorBorder: "rgba(0, 212, 255, 0.2)",
                  optionSelectedBg: "rgba(0, 212, 255, 0.15)",
                  optionSelectedColor: "#ffffff",
                },
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
