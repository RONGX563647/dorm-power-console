"use client";

import { Card, Col, Popover, Row, Space, Switch, Tag, Typography } from "antd";
import type { SocketStatus } from "@/components/types";

const { Text } = Typography;

/**
 * 根据插孔状态返回状态类名
 * 
 * @param s 插孔状态对象
 * @returns 状态类名
 */
function statusClass(s: SocketStatus) {
  if (s.power_w >= 100) return "status-dot-bad"; // 高负载状态
  if (s.on) return "status-dot-ok"; // 正常运行状态
  return "status-dot-off"; // 关闭状态
}

/**
 * 根据插孔状态返回状态文本
 * 
 * @param s 插孔状态对象
 * @returns 状态文本
 */
function statusText(s: SocketStatus) {
  if (s.power_w >= 100) return "高负载"; // 高负载警告
  return s.on ? "运行中" : "已关闭"; // 正常运行或关闭状态
}

/**
 * 插孔矩阵组件
 * 
 * 显示智能插座的插孔状态，包括开关状态和功率消耗。
 * 支持通过开关控制插孔电源。
 */
export default function SocketMatrix({
  sockets, // 插孔状态数组
  onToggle, // 插孔开关切换回调函数
}: {
  sockets: SocketStatus[];
  onToggle?: (socketId: number, nextOn: boolean) => void;
}) {
  // 计算所有插孔的总功率
  const total = sockets.reduce((sum, s) => sum + s.power_w, 0);

  return (
    <Card className="glass-card" title="宿舍设备聚合面板" styles={{ body: { padding: 16 } }}>
      {/* 总功率显示区域 */}
      <div style={{ marginBottom: 12 }}>
        <Text type="secondary">当前总功率</Text>
        <div style={{ fontSize: 24, fontWeight: 700, color: "#0f7fb0" }}>{total.toFixed(1)}W</div>
      </div>
      {/* 插孔网格 */}
      <Row gutter={[10, 10]}>
        {sockets.map((s) => (
          <Col xs={24} sm={12} key={s.id}>
            <Card size="small" style={{ borderRadius: 12, borderColor: "rgba(13,108,145,0.18)" }}>
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                {/* 插孔标题和状态标签 */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Text strong>插孔 {s.id}</Text>
                  <Tag color={s.power_w >= 100 ? "error" : s.on ? "success" : "default"}>{statusText(s)}</Tag>
                </div>

                {/* 插孔详情弹出框 */}
                <Popover
                  trigger={["hover", "click"]}
                  content={
                    <Space direction="vertical" size="small" style={{ minWidth: 220 }}>
                      <Text strong>插孔 {s.id} / {s.device}</Text>
                      <Text type="secondary">实时功率：{s.power_w.toFixed(1)}W</Text>
                      <Text type="secondary">当前状态：{s.on ? "ON" : "OFF"}</Text>
                      {/* 插孔开关控制 */}
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <Text>电源控制</Text>
                        <Switch checked={s.on} onChange={(v) => onToggle?.(s.id, v)} />
                      </div>
                    </Space>
                  }
                >
                  {/* 插孔卡片内容 */}
                  <div style={{
                    border: "1px dashed rgba(15, 127, 176, 0.35)",
                    borderRadius: 10,
                    padding: 10,
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                  }}>
                    <span>
                      <span className={`status-dot ${statusClass(s)}`} />
                      <Text>{s.device}</Text>
                    </span>
                    <Text strong>{s.power_w.toFixed(1)}W</Text>
                  </div>
                </Popover>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </Card>
  );
}
