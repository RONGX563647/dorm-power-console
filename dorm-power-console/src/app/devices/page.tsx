"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { Device, StripStatus } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import { Card, Input, Select, Space, Statistic, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import Link from "next/link";

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
      title: "设备名称",
      dataIndex: "name",
      render: (_, d) => <Link href={`/devices/${d.id}`}>{d.name}</Link>,
    },
    { title: "设备ID", dataIndex: "id" },
    { title: "房间", dataIndex: "room" },
    {
      title: "在线",
      dataIndex: "online",
      render: (v) => <Tag color={v ? "green" : "default"}>{v ? "Online" : "Offline"}</Tag>,
    },
    {
      title: "当前功率",
      render: (_, d) => <span>{d.online ? `${(powerMap[d.id] ?? 0).toFixed(1)} W` : "--"}</span>,
    },
    { title: "固件版本", render: () => <span>v1.0.3</span> },
    {
      title: "最近上报",
      dataIndex: "lastSeen",
      sorter: (a, b) => new Date(a.lastSeen).getTime() - new Date(b.lastSeen).getTime(),
      defaultSortOrder: "descend",
    },
  ];

  const onlineCount = devices.filter((d) => d.online).length;

  return (
    <AppLayout title="设备 Devices">
      <Card styles={{ body: { padding: 16 } }}>
        <Space wrap style={{ width: "100%", justifyContent: "space-between" }}>
          <Space size={24}>
            <Statistic title="总设备" value={devices.length} />
            <Statistic title="在线" value={onlineCount} />
            <Statistic title="离线" value={devices.length - onlineCount} />
            <Statistic title="在线率" value={devices.length ? ((onlineCount / devices.length) * 100).toFixed(1) : 0} suffix="%" />
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
              style={{ width: 130 }}
            />
            <Select value={roomFilter} onChange={setRoomFilter} options={roomOptions} style={{ width: 140 }} />
            <Input.Search
              allowClear
              placeholder="搜索设备名称 / 房间 / ID"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={{ width: 280 }}
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

