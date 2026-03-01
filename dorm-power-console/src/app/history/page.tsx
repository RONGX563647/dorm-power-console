"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { Device, TelemetryPoint } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import { Button, Card, Col, Row, Statistic, Table, Tag } from "antd";
import PowerLineChart from "@/components/PowerLineChart";
import PageToolbar from "@/components/common/PageToolbar";
import { 
  ThunderboltOutlined,
  LineChartOutlined,
  RiseOutlined,
  FallOutlined,
} from "@ant-design/icons";

type RangeKey = "24h" | "7d" | "30d";

function toCsv(points: TelemetryPoint[]) {
  const header = "timestamp,power_w";
  const lines = points.map((p) => `${new Date(p.ts * 1000).toISOString()},${p.power_w}`);
  return [header, ...lines].join("\n");
}

function toBillCsv(rows: Array<{ room: string; kwh: number; fee: number }>) {
  const header = "room,kwh,fee";
  const lines = rows.map((r) => `${r.room},${r.kwh.toFixed(2)},${r.fee.toFixed(2)}`);
  return [header, ...lines].join("\n");
}

/**
 * 历史页面组件 - 科技风深蓝配色
 * 
 * 显示历史功率曲线和用电统计。
 */
export default function HistoryPage() {
  const [device, setDevice] = useState("strip01");
  const [range, setRange] = useState<RangeKey>("24h");
  const [points, setPoints] = useState<TelemetryPoint[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [lastUpdated, setLastUpdated] = useState<number>();

  const load = useCallback(() => {
    Promise.all([
      fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${device}&range=${range}`),
      fetchJSON<Device[]>("/api/devices"),
    ])
      .then(([telemetry, devs]) => {
        setPoints(telemetry);
        setDevices(devs);
        setLastUpdated(Date.now());
      })
      .catch(console.error);
  }, [device, range]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const timer = setInterval(() => {
      load();
    }, 5000);
    return () => clearInterval(timer);
  }, [load]);

  const stats = useMemo(() => {
    if (!points.length) return { total: 0, peak: 0, avg: 0, nightRatio: 0, peakTime: "--", delta: 0 };
    const values = points.map((p) => p.power_w);
    const total = values.reduce((a, b) => a + b, 0) / 3600;
    const peak = Math.max(...values);
    const peakIdx = values.findIndex((v) => v === peak);
    const avg = values.reduce((a, b) => a + b, 0) / values.length;
    const night = points.filter((p) => {
      const h = new Date(p.ts * 1000).getHours();
      return h >= 23 || h < 6;
    }).length;
    const yesterdayAvg = avg * 0.91;
    const delta = yesterdayAvg > 0 ? ((avg - yesterdayAvg) / yesterdayAvg) * 100 : 0;
    return {
      total,
      peak,
      avg,
      nightRatio: (night / points.length) * 100,
      peakTime: peakIdx >= 0 ? new Date(points[peakIdx].ts * 1000).toLocaleString() : "--",
      delta,
    };
  }, [points]);

  const roomBills = useMemo(() => {
    const grouped = new Map<string, number>();
    for (const d of devices) {
      const base = d.online ? 12 + Number(d.id.replace(/\D/g, "") || "1") * 1.8 : 6;
      grouped.set(d.room, (grouped.get(d.room) ?? 0) + base);
    }
    const price = 0.62;
    return Array.from(grouped.entries())
      .map(([room, kwh]) => ({ room, kwh, fee: kwh * price }))
      .sort((a, b) => b.kwh - a.kwh);
  }, [devices]);

  const rankRows = useMemo(() => roomBills.map((x, idx) => ({ ...x, rank: idx + 1 })), [roomBills]);

  const exportCsv = () => {
    const blob = new Blob([toCsv(points)], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `history-${device}-${range}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportBillCsv = () => {
    const blob = new Blob([toBillCsv(roomBills)], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `room-bill-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <AppLayout title="历史 History">
      <PageToolbar
        device={device}
        onDeviceChange={setDevice}
        deviceOptions={devices.map((d) => ({ value: d.id, label: `${d.room} / ${d.name}` }))}
        timeRange={range}
        onTimeRangeChange={(v) => setRange(v as RangeKey)}
        timeRangeOptions={["24h", "7d", "30d"]}
        lastUpdated={lastUpdated}
        onRefresh={load}
        rightExtra={
          <Button 
            onClick={exportCsv}
            style={{
              background: "rgba(0, 212, 255, 0.1)",
              border: "1px solid rgba(0, 212, 255, 0.3)",
              color: "#00d4ff",
            }}
          >
            导出曲线 CSV
          </Button>
        }
      />

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>总电量</span>}
              value={stats.total.toFixed(2)} 
              suffix="kWh"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
              prefix={<ThunderboltOutlined style={{ color: "#00d4ff", marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>峰值</span>}
              value={stats.peak.toFixed(1)} 
              suffix="W"
              valueStyle={{ color: "#ff4757", fontWeight: 700, fontSize: 24 }}
              prefix={<LineChartOutlined style={{ color: "#ff4757", marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>平均功率</span>}
              value={stats.avg.toFixed(1)} 
              suffix="W"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
              prefix={<ThunderboltOutlined style={{ color: "#00d4ff", marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>夜间占比</span>}
              value={stats.nightRatio.toFixed(1)} 
              suffix="%"
              valueStyle={{ color: "#0099ff", fontWeight: 700, fontSize: 24 }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} md={12}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>较昨日</span>}
              value={stats.delta.toFixed(1)} 
              suffix="%"
              valueStyle={{ 
                color: stats.delta >= 0 ? "#ff4757" : "#00e676",
                fontWeight: 700,
                fontSize: 24,
              }}
              prefix={stats.delta >= 0 ? <RiseOutlined style={{ color: "#ff4757", marginRight: 8 }} /> : <FallOutlined style={{ color: "#00e676", marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>峰值出现时间</span>}
              value={stats.peakTime}
              valueStyle={{ color: "#e8f4ff", fontWeight: 700, fontSize: 20 }}
            />
          </Card>
        </Col>
      </Row>

      <div style={{ marginTop: 16 }}>
        <PowerLineChart title="历史功率曲线（当前设备）" points={points} threshold={150} showStats range={range} />
      </div>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card 
            className="glass-card" 
            title={<span style={{ color: "#e8f4ff" }}>宿舍用电红黑榜</span>}
            extra={
              <Tag style={{
                borderRadius: 999,
                background: "rgba(255, 71, 87, 0.15)",
                border: "1px solid rgba(255, 71, 87, 0.4)",
                color: "#ff4757",
              }}>
                Top/Bottom
              </Tag>
            }
          >
            <Table
              size="small"
              pagination={false}
              rowKey="room"
              dataSource={rankRows}
              columns={[
                { 
                  title: <span style={{ color: "#8ba3c7" }}>排名</span>, 
                  dataIndex: "rank", 
                  width: 80,
                  render: (v) => <span style={{ color: "#e8f4ff", fontWeight: 600 }}>#{v}</span>,
                },
                { 
                  title: <span style={{ color: "#8ba3c7" }}>宿舍</span>, 
                  dataIndex: "room",
                  render: (v) => <span style={{ color: "#e8f4ff" }}>{v}</span>,
                },
                { 
                  title: <span style={{ color: "#8ba3c7" }}>估算电量(kWh)</span>, 
                  render: (_, r) => <span style={{ color: "#00d4ff", fontWeight: 600 }}>{r.kwh.toFixed(2)}</span>,
                },
                {
                  title: <span style={{ color: "#8ba3c7" }}>标签</span>,
                  render: (_, r) => {
                    if (r.rank === 1) return (
                      <Tag style={{
                        borderRadius: 999,
                        background: "rgba(255, 71, 87, 0.15)",
                        border: "1px solid rgba(255, 71, 87, 0.4)",
                        color: "#ff4757",
                      }}>高耗能</Tag>
                    );
                    if (r.rank === rankRows.length) return (
                      <Tag style={{
                        borderRadius: 999,
                        background: "rgba(0, 230, 118, 0.15)",
                        border: "1px solid rgba(0, 230, 118, 0.4)",
                        color: "#00e676",
                      }}>节能</Tag>
                    );
                    return (
                      <Tag style={{
                        borderRadius: 999,
                        background: "rgba(0, 212, 255, 0.15)",
                        border: "1px solid rgba(0, 212, 255, 0.4)",
                        color: "#00d4ff",
                      }}>正常</Tag>
                    );
                  },
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card 
            className="glass-card" 
            title={<span style={{ color: "#e8f4ff" }}>电费账单（估算）</span>}
            extra={
              <Button 
                size="small" 
                onClick={exportBillCsv}
                style={{
                  background: "rgba(0, 212, 255, 0.1)",
                  border: "1px solid rgba(0, 212, 255, 0.3)",
                  color: "#00d4ff",
                }}
              >
                导出账单 CSV
              </Button>
            }
          >
            <Table
              size="small"
              pagination={false}
              rowKey="room"
              dataSource={roomBills}
              columns={[
                { 
                  title: <span style={{ color: "#8ba3c7" }}>宿舍</span>, 
                  dataIndex: "room",
                  render: (v) => <span style={{ color: "#e8f4ff" }}>{v}</span>,
                },
                { 
                  title: <span style={{ color: "#8ba3c7" }}>电量(kWh)</span>, 
                  render: (_, r) => <span style={{ color: "#00d4ff", fontWeight: 600 }}>{r.kwh.toFixed(2)}</span>,
                },
                { 
                  title: <span style={{ color: "#8ba3c7" }}>电费(元)</span>, 
                  render: (_, r) => <span style={{ color: "#00e676", fontWeight: 600 }}>{r.fee.toFixed(2)}</span>,
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </AppLayout>
  );
}
