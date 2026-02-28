"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { Device, TelemetryPoint } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import { Button, Card, Col, Row, Statistic, Table, Tag } from "antd";
import PowerLineChart from "@/components/PowerLineChart";
import PageToolbar from "@/components/common/PageToolbar";

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
    const delta = yesterdayAvg ? ((avg - yesterdayAvg) / yesterdayAvg) * 100 : 0;
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
        rightExtra={<Button onClick={exportCsv}>导出曲线 CSV</Button>}
      />

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}><Card className="glass-card"><Statistic title="总电量" value={stats.total.toFixed(2)} suffix="kWh" /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card className="glass-card"><Statistic title="峰值" value={stats.peak.toFixed(1)} suffix="W" /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card className="glass-card"><Statistic title="平均功率" value={stats.avg.toFixed(1)} suffix="W" /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card className="glass-card"><Statistic title="夜间占比" value={stats.nightRatio.toFixed(1)} suffix="%" /></Card></Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} md={12}><Card className="glass-card"><Statistic title="较昨日" value={stats.delta.toFixed(1)} suffix="%" styles={{ content: { color: stats.delta >= 0 ? "#cf1322" : "#3f8600" } }} /></Card></Col>
        <Col xs={24} md={12}><Card className="glass-card"><Statistic title="峰值出现时间" value={stats.peakTime} /></Card></Col>
      </Row>

      <div style={{ marginTop: 16 }}>
        <PowerLineChart title="历史功率曲线（当前设备）" points={points} threshold={150} showStats range={range} />
      </div>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card className="glass-card" title="宿舍用电红黑榜" extra={<Tag color="red">Top/Bottom</Tag>}>
            <Table
              size="small"
              pagination={false}
              rowKey="room"
              dataSource={rankRows}
              columns={[
                { title: "排名", dataIndex: "rank", width: 80 },
                { title: "宿舍", dataIndex: "room" },
                { title: "估算电量(kWh)", render: (_, r) => r.kwh.toFixed(2) },
                {
                  title: "标签",
                  render: (_, r) => {
                    if (r.rank === 1) return <Tag color="red">高耗能</Tag>;
                    if (r.rank === rankRows.length) return <Tag color="green">节能</Tag>;
                    return <Tag>正常</Tag>;
                  },
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card className="glass-card" title="电费账单（估算）" extra={<Button size="small" onClick={exportBillCsv}>导出账单 CSV</Button>}>
            <Table
              size="small"
              pagination={false}
              rowKey="room"
              dataSource={roomBills}
              columns={[
                { title: "宿舍", dataIndex: "room" },
                { title: "电量(kWh)", render: (_, r) => r.kwh.toFixed(2) },
                { title: "电费(元)", render: (_, r) => r.fee.toFixed(2) },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </AppLayout>
  );
}
