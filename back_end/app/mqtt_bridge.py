"""MQTT桥接模块

本模块提供MQTT协议桥接功能，用于与物联网设备进行通信。
通过MQTT协议接收设备上报的状态和遥测数据，并向设备发送控制命令。
支持设备状态实时更新和命令执行结果通知。

使用方式:
    from app.mqtt_bridge import mqtt_bridge
    
    # 启动MQTT桥接
    mqtt_bridge.set_loop(asyncio.get_event_loop())
    mqtt_bridge.start()
    
    # 发布命令到设备
    mqtt_bridge.publish_cmd("room101 device1", {"action": "toggle", "socket": 1})
"""

from __future__ import annotations

import asyncio
import json
import logging
import time
from typing import Any

import paho.mqtt.client as mqtt

from .config import settings
from .db import get_session
from .services import (
    apply_command_effect_to_status,
    save_telemetry_point,
    sync_status_metrics_from_telemetry,
    update_cmd_state,
    update_status_from_payload,
)
from .ws import ws_manager

# 创建MQTT桥接模块的日志记录器
logger = logging.getLogger("mqtt-bridge")


class MQTTBridge:
    """MQTT桥接类
    
    管理与MQTT代理的连接，处理设备消息的接收和命令的发送。
    支持设备状态更新、遥测数据接收和命令执行结果处理。
    """
    
    def __init__(self) -> None:
        """初始化MQTT桥接
        
        创建MQTT客户端并配置回调函数，设置认证信息。
        """
        # 从配置中读取MQTT启用状态
        self._enabled = settings.mqtt_enabled
        # MQTT连接状态标志
        self._connected = False
        # 事件循环引用，用于异步广播
        self._loop: asyncio.AbstractEventLoop | None = None
        # 创建MQTT客户端，指定客户端ID
        self._client = mqtt.Client(client_id="dorm-power-backend")
        # 如果配置了用户名和密码，则设置认证
        if settings.mqtt_username:
            self._client.username_pw_set(settings.mqtt_username, settings.mqtt_password)
        # 设置MQTT事件回调函数
        self._client.on_connect = self._on_connect
        self._client.on_message = self._on_message
        self._client.on_disconnect = self._on_disconnect

    @property
    def enabled(self) -> bool:
        """获取MQTT是否启用
        
        Returns:
            bool: MQTT功能是否启用
        """
        return self._enabled

    @property
    def connected(self) -> bool:
        """获取MQTT连接状态
        
        Returns:
            bool: MQTT是否已连接
        """
        return self._connected

    def set_loop(self, loop: asyncio.AbstractEventLoop) -> None:
        """设置事件循环
        
        设置用于异步广播的事件循环，确保WebSocket广播在正确的线程中执行。
        
        Args:
            loop: asyncio事件循环对象
        """
        self._loop = loop

    def start(self) -> None:
        """启动MQTT桥接
        
        连接到MQTT代理并开始监听消息。
        如果MQTT未启用，则直接返回。
        """
        # 如果MQTT未启用，记录日志并返回
        if not self._enabled:
            logger.info("MQTT disabled via MQTT_ENABLED=0")
            return
        
        try:
            # 连接到MQTT代理
            self._client.connect(settings.mqtt_host, settings.mqtt_port, keepalive=60)
            # 启动MQTT客户端网络循环
            self._client.loop_start()
            logger.info("MQTT connecting to %s:%s", settings.mqtt_host, settings.mqtt_port)
        except Exception as exc:
            # 连接失败，记录异常
            logger.exception("MQTT connect failed: %s", exc)

    def stop(self) -> None:
        """停止MQTT桥接
        
        断开与MQTT代理的连接并停止监听消息。
        """
        # 如果MQTT未启用，直接返回
        if not self._enabled:
            return
        
        try:
            # 停止MQTT客户端网络循环
            self._client.loop_stop()
            # 断开与MQTT代理的连接
            self._client.disconnect()
        except Exception:
            # 停止失败，记录异常
            logger.exception("MQTT stop failed")

    def publish_cmd(self, device_id: str, payload: dict[str, Any]) -> bool:
        """发布命令到设备
        
        将控制命令发布到MQTT主题，供设备接收和执行。
        支持多种主题格式，确保设备能够正确接收命令。
        
        Args:
            device_id: 设备ID，格式可能为"device_id"或"room_id device_id"
            payload: 命令负载，包含命令类型和参数
            
        Returns:
            bool: 命令是否成功发布
        """
        # 检查MQTT是否启用且已连接
        if not (self._enabled and self._connected):
            return False
        
        # 构建命令主题列表
        topics: list[str] = [f"{settings.mqtt_topic_prefix}/{device_id}/cmd"]
        # 如果设备ID包含房间和设备两部分，则构建额外的主题
        chunks = [x for x in device_id.split(" ", 1) if x]
        if len(chunks) == 2:
            topics.append(f"{settings.mqtt_topic_prefix}/{chunks[0]}/{chunks[1]}/cmd")

        # 将负载序列化为JSON字符串
        payload_text = json.dumps(payload, ensure_ascii=False)
        ok = False
        # 使用dict.fromkeys去重，确保每个主题只发布一次
        for topic in dict.fromkeys(topics):
            # 发布消息到主题，QoS为1（至少送达一次）
            result = self._client.publish(topic, payload_text, qos=1)
            # 检查发布结果
            ok = ok or result.rc == mqtt.MQTT_ERR_SUCCESS
        return ok

    def _on_connect(self, client: mqtt.Client, userdata: Any, flags: Any, reason_code: Any, properties: Any = None) -> None:
        """MQTT连接成功回调
        
        当与MQTT代理成功连接时调用，订阅设备主题以接收设备消息。
        
        Args:
            client: MQTT客户端实例
            userdata: 用户数据
            flags: 连接标志
            reason_code: 连接结果代码，0表示成功
            properties: 连接属性（MQTT 5.0）
        """
        # 更新连接状态
        self._connected = reason_code == 0
        logger.info("MQTT connected rc=%s", reason_code)
        
        # 如果连接失败，直接返回
        if not self._connected:
            return
        
        # 订阅设备主题
        base = settings.mqtt_topic_prefix
        # 订阅单级设备主题（格式：prefix/device_id/type）
        for topic in (f"{base}/+/status", f"{base}/+/telemetry", f"{base}/+/ack", f"{base}/+/event"):
            client.subscribe(topic, qos=1)
        # 订阅两级设备主题（格式：prefix/room_id/device_id/type）
        for topic in (f"{base}/+/+/status", f"{base}/+/+/telemetry", f"{base}/+/+/ack", f"{base}/+/+/event"):
            client.subscribe(topic, qos=1)

    def _on_disconnect(self, client: mqtt.Client, userdata: Any, disconnect_flags: Any, reason_code: Any, properties: Any = None) -> None:
        """MQTT断开连接回调
        
        当与MQTT代理断开连接时调用，更新连接状态。
        
        Args:
            client: MQTT客户端实例
            userdata: 用户数据
            disconnect_flags: 断开连接标志
            reason_code: 断开连接原因代码
            properties: 断开连接属性（MQTT 5.0）
        """
        # 更新连接状态
        self._connected = False
        logger.warning("MQTT disconnected rc=%s", reason_code)

    def _on_message(self, client: mqtt.Client, userdata: Any, msg: mqtt.MQTTMessage) -> None:
        """MQTT消息接收回调
        
        当接收到MQTT消息时调用，解析消息并更新设备状态或处理命令确认。
        
        Args:
            client: MQTT客户端实例
            userdata: 用户数据
            msg: 接收到的MQTT消息
        """
        try:
            # 解析消息负载为JSON
            payload = json.loads(msg.payload.decode("utf-8", errors="ignore"))
        except Exception:
            # 解析失败，记录警告并返回
            logger.warning("Invalid JSON payload on topic=%s", msg.topic)
            return

        # 解析主题，获取设备ID和消息类型
        parsed = self._parse_topic(msg.topic)
        if parsed is None:
            return
        device_id, msg_type = parsed

        # 使用数据库会话处理消息
        with get_session() as session:
            # 处理设备状态消息
            if msg_type == "status":
                # 更新设备状态
                update_status_from_payload(session, device_id, payload)
                # 即使设备只上传状态，也保存遥测数据点，以便历史图表使用
                save_telemetry_point(session, device_id, payload)
                # 广播设备状态更新
                self._broadcast_safe({"type": "DEVICE_STATUS", "deviceId": device_id, "payload": payload})
            # 处理遥测数据消息
            elif msg_type == "telemetry":
                # 保存遥测数据点
                save_telemetry_point(session, device_id, payload)
                # 从遥测数据同步状态指标
                sync_status_metrics_from_telemetry(session, device_id, payload)
                # 广播遥测数据
                self._broadcast_safe({"type": "TELEMETRY", "deviceId": device_id, "payload": payload})
            # 处理命令确认消息
            elif msg_type == "ack":
                # 提取命令ID和状态
                cmd_id = str(payload.get("cmdId", ""))
                status = str(payload.get("status", "success"))
                cost_ms = payload.get("costMs")
                # 更新命令状态
                cmd = update_cmd_state(
                    session,
                    cmd_id,
                    "success" if status == "success" else "failed",
                    message=str(payload.get("errorMsg", "")),
                    duration_ms=int(cost_ms) if isinstance(cost_ms, (int, float)) else None,
                )
                # 如果命令存在，处理命令结果
                if cmd:
                    # 如果命令成功执行，将命令效果应用到设备状态
                    if cmd.state == "success":
                        apply_command_effect_to_status(session, cmd)
                    # 构建命令确认事件
                    event = {
                        "type": "CMD_ACK",
                        "cmdId": cmd.cmd_id,
                        "state": cmd.state,
                        "ts": int(time.time()),
                        "updatedAt": cmd.updated_at,
                        "message": cmd.message,
                        "durationMs": cmd.duration_ms,
                    }
                    # 广播命令确认事件
                    self._broadcast_safe(event)

    def _broadcast_safe(self, payload: dict[str, Any]) -> None:
        """安全广播消息
        
        在事件循环中安全地广播消息到所有WebSocket连接。
        如果事件循环未设置，则不执行广播。
        
        Args:
            payload: 要广播的消息字典
        """
        # 检查事件循环是否已设置
        if self._loop is None:
            return
        # 在事件循环中安全地执行WebSocket广播
        asyncio.run_coroutine_threadsafe(ws_manager.broadcast(payload), self._loop)

    def _parse_topic(self, topic: str) -> tuple[str, str] | None:
        """解析MQTT主题
        
        从MQTT主题中提取设备ID和消息类型。
        支持单级和两级设备ID格式。
        
        Args:
            topic: MQTT主题字符串
            
        Returns:
            tuple[str, str] | None: 设备ID和消息类型的元组，如果解析失败则返回None
        """
        # 分割主题为部分列表
        topic_parts = [p for p in topic.strip("/").split("/") if p]
        # 分割主题前缀为部分列表
        prefix_parts = [p for p in settings.mqtt_topic_prefix.strip("/").split("/") if p]
        
        # 检查主题部分数量是否足够
        if len(topic_parts) < len(prefix_parts) + 2:
            return None
        
        # 检查主题前缀是否匹配
        if topic_parts[: len(prefix_parts)] != prefix_parts:
            return None

        # 提取主题尾部（前缀之后的部分）
        tail = topic_parts[len(prefix_parts) :]
        # 消息类型是尾部的最后一部分
        msg_type = tail[-1]
        # 检查消息类型是否有效
        if msg_type not in {"status", "telemetry", "ack", "event"}:
            return None

        # 提取设备部分（除消息类型外的部分）
        device_parts = [p.strip() for p in tail[:-1] if p.strip()]
        # 检查设备部分是否为空
        if not device_parts:
            return None

        # 处理单级设备ID（格式：device_id）
        if len(device_parts) == 1:
            # 合并设备ID中的空格
            token = " ".join(device_parts[0].split())
            return token, msg_type

        # 处理两级设备ID（格式：room_id device_id）
        if len(device_parts) == 2:
            # 合并房间ID和设备ID中的空格
            room = " ".join(device_parts[0].split())
            dev = " ".join(device_parts[1].split())
            # 如果房间和设备都不为空，返回组合的设备ID
            if room and dev:
                return f"{room} {dev}", msg_type
            # 否则返回非空部分
            return " ".join([x for x in (room, dev) if x]), msg_type

        # 处理多级设备ID，合并所有部分
        return " ".join(device_parts), msg_type


# 创建全局MQTT桥接实例
# 应用程序通过导入此对象管理所有MQTT通信
mqtt_bridge = MQTTBridge()
