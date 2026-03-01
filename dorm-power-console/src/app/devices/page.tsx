"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { Device, StripStatus } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import { Card, Input, Select, Space, Statistic, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import Link from "next/link";
import { 
  DesktopOutlined, 
  WifiOutlined, 
  DisconnectOutlined,
  ThunderboltOutlined,
  SearchOutlined,
} from "@ant-design/icons";

/**
 * 设备页面组件 - 科技风深蓝配色
 * 
 * 显示所有设备的列表，包括设备状态、功率和在线情况。
 */
export default function DevicesPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [powerMap, setPowerMap] = useState<Record<string, number>>({});
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | "online" | "offline">("all");
  const [roomFilter, setRoomFilter] = useState<string>("all");

  const loadDevices = useCallback(async () => {
    const devs = await fetchJSON<Device[]>("/api/devices");
    const powerPairs = await Promise.all(
      devs.map(async (d) => {
        try {
          const s = await fetchJSON<StripStatus>(`/api/devices/${d.id}/status`);
          return [d.id, s.total_power_w] as const;
        } catch {
          return [d.id, 0] as const;
        }
      }),
    );
    setDevices(devs);
    setPowerMap(Object.fromEntries(powerPairs));
  }, []);

  useEffect(() => {
    loadDevices().catch(console.error);
    const timer = setInterval(() => {
      loadDevices().catch(console.error);
    }, 2000);
    return () => clearInterval(timer);
  }, [loadDevices]);

  const roomOptions = useMemo(() => {
    const rooms = Array.from(new Set(devices.map((d) => d.room)));
    return [{ value: "all", label: "全部房间" }, ...rooms.map((r) => ({ value: r, label: r }))];
  }, [devices]);

  const filtered = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    return devices
      .filter((d) => (k ? d.name.toLowerCase().includes(k) || d.room.toLowerCase().includes(k) || d.id.toLowerCase().includes(k) : true))
      .filter((d) => (statusFilter === "all" ? true : statusFilter === "online" ? d.online : !d.online))
      .filter((d) => (roomFilter === "all" ? true : d.room === roomFilter))
      .sort((a, b) => new Date(b.lastSeen).getTime() - new Date(a.lastSeen).getTime());
  }, [devices, keyword, statusFilter, roomFilter]);

  const columns: ColumnsType<Device> = [
    {
      title: <span style={{ color: "#8ba3c7" }}>设备名称</span>,
      dataIndex: "name",
      render: (_, d) => (
        <Link href={`/devices/${d.id}`} style={{ color: "#00d4ff", fontWeight: 500 }}>
          <DesktopOutlined style={{ marginRight: 8, color: "#8ba3c7" }} />
          {d.name}
        </Link>
      ),
    },
    { 
      title: <span style={{ color: "#8ba3c7" }}>设备ID</span>, 
      dataIndex: "id",
      render: (v) => <span style={{ color: "#e8f4ff" }}>{v}</span>,
    },
    { 
      title: <span style={{ color: "#8ba3c7" }}>房间</span>, 
      dataIndex: "room",
      render: (v) => <span style={{ color: "#e8f4ff" }}>{v}</span>,
    },
    {
      title: <span style={{ color: "#8ba3c7" }}>在线状态</span>,
      dataIndex: "online",
      render: (v) => (
        <Tag style={{
          borderRadius: 999,
          background: v ? "rgba(0, 230, 118, 0.15)" : "rgba(90, 106, 122, 0.15)",
          border: `1px solid ${v ? "rgba(0, 230, 118, 0.4)" : "rgba(90, 106, 122, 0.4)"}`,
          color: v ? "#00e676" : "#8ba3c7",
        }}>
          {v ? <WifiOutlined style={{ marginRight: 4 }} /> : <DisconnectOutlined style={{ marginRight: 4 }} />}
          {v ? "Online" : "Offline"}
        </Tag>
      ),
    },
    {
      title: <span style={{ color: "#8ba3c7" }}>当前功率</span>,
      render: (_, d) => (
        <span style={{ color: d.online ? "#00d4ff" : "#5a6a7a", fontWeight: 600 }}>
          {d.online ? (
            <><ThunderboltOutlined style={{ marginRight: 4 }} />{(powerMap[d.id] ?? 0).toFixed(1)} W</>
          ) : "--"}
        </span>
      ),
    },
    { 
      title: <span style={{ color: "#8ba3c7" }}>固件版本</span>, 
      render: () => <span style={{ color: "#8ba3c7" }}>v1.0.3</span>,
    },
    {
      title: <span style={{ color: "#8ba3c7" }}>最近上报</span>,
      dataIndex: "lastSeen",
      sorter: (a, b) => new Date(a.lastSeen).getTime() - new Date(b.lastSeen).getTime(),
      defaultSortOrder: "descend",
      render: (v) => <span style={{ color: "#8ba3c7" }}>{v}</span>,
    },
  ];

  const onlineCount = devices.filter((d) => d.online).length;

  return (
    <AppLayout title="设备 Devices">
      <Card 
        styles={{ body: { padding: 16 } }}
        style={{
          background: "rgba(16, 24, 40, 0.6)",
          border: "1px solid rgba(0, 212, 255, 0.15)",
        }}
      >
        <Space wrap style={{ width: "100%", justifyContent: "space-between" }}>
          <Space size={24}>
            <Statistic 
              title={<span style={{ color: "#8ba3c7" }}>总设备</span>} 
              value={devices.length}
              valueStyle={{ color: "#e8f4ff", fontWeight: 700, fontSize: 24 }}
            />
            <Statistic 
              title={<span style={{ color: "#8ba3c7" }}>在线</span>} 
              value={onlineCount}
              valueStyle={{ color: "#00e676", fontWeight: 700, fontSize: 24 }}
            />
            <Statistic 
              title={<span style={{ color: "#8ba3c7" }}>离线</span>} 
              value={devices.length - onlineCount}
              valueStyle={{ color: "#ff4757", fontWeight: 700, fontSize: 24 }}
            />
            <Statistic 
              title={<span style={{ color: "#8ba3c7" }}>在线率</span>} 
              value={devices.length ? ((onlineCount / devices.length) * 100).toFixed(1) : 0} 
              suffix="%"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
            />
          </Space>
          <Space wrap>
            <Select
              value={statusFilter}
              onChange={(v) => setStatusFilter(v)}
              options={[
                { value: "all", label: "全部状态" },
                { value: "online", label: "仅在线" },
                { value: "offline", label: "仅离线" },
              ]}
              style={{ 
                width: 130,
                background: "rgba(16, 24, 40, 0.6)",
              }}
            />
            <Select 
              value={roomFilter} 
              onChange={setRoomFilter} 
              options={roomOptions} 
              style={{ width: 140 }}
            />
            <Input.Search
              allowClear
              placeholder="搜索设备名称 / 房间 / ID"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={{ width: 280 }}
              prefix={<SearchOutlined style={{ color: "#8ba3c7" }} />}
            />
          </Space>
        </Space>
      </Card>

      <div style={{ marginTop: 16 }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filtered}
          rowClassName={(row) => (!row.online ? "offline-row" : "")}
          pagination={{ pageSize: 10 }}
        />
      </div>
    </AppLayout>
  );
}
