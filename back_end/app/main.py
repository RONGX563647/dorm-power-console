"""主程序模块

本模块是应用程序的入口点，负责初始化FastAPI应用、配置中间件和定义API路由。
提供设备管理、命令控制、遥测数据查询和WebSocket实时通信等功能。

使用方式:
    # 启动应用
    uvicorn app.main:app --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

import asyncio
import json
import logging
import secrets
import time
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI, Query, WebSocket, WebSocketDisconnect
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import select

from .config import settings
from .db import Base, engine, get_session
from .models import Device, StripStatus
from .mqtt_bridge import mqtt_bridge
from .schemas import AIReportOut, CmdRequest, CmdStateOut, CmdSubmitOut, DeviceOut, StripStatusOut
from .services import (
    ai_report,
    build_telemetry_series,
    create_cmd_record,
    ensure_default_admin,
    ensure_seed_data,
    get_cmd_state,
    has_pending_conflict,
    login_user,
    mark_timeouts,
    refresh_online_state,
    update_cmd_state,
    utc_iso,
)
from .schemas import (
    AuthLoginOut,
    AuthLoginRequest,
    AuthUserOut,
)
from .ws import ws_manager

# 配置日志记录
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("dorm-backend")


def error_response(status: int, code: str, message: str, details: dict[str, Any] | None = None) -> JSONResponse:
    """创建错误响应
    
    创建一个标准化的错误响应，包含状态码、错误代码、消息和详细信息。
    
    Args:
        status: HTTP状态码
        code: 错误代码
        message: 错误消息
        details: 错误详细信息
        
    Returns:
        JSONResponse: 包含错误信息的JSON响应
    """
    return JSONResponse(
        status_code=status,
        content={
            "ok": False,
            "code": code,
            "message": message,
            "details": details or {},
        },
    )


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理
    
    管理应用程序的启动和关闭过程，包括数据库初始化、
    种子数据创建、默认管理员账户创建和MQTT桥接启动。
    
    Args:
        app: FastAPI应用实例
    """
    # 创建所有数据库表
    Base.metadata.create_all(bind=engine)
    # 初始化数据库数据
    with get_session() as session:
        # 确保种子数据存在
        ensure_seed_data(session)
        # 确保默认管理员账户存在
        ensure_default_admin(session)
        # 标记超时的命令
        mark_timeouts(session)

    # 设置MQTT桥接的事件循环
    mqtt_bridge.set_loop(asyncio.get_running_loop())
    # 启动MQTT桥接
    mqtt_bridge.start()
    try:
        # 应用程序运行中
        yield
    finally:
        # 应用程序关闭时停止MQTT桥接
        mqtt_bridge.stop()


# 创建FastAPI应用实例
app = FastAPI(title="Dorm Power Backend", version="1.0.0", lifespan=lifespan)

# 添加CORS中间件，允许跨域请求
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源
    allow_methods=["*"],  # 允许所有HTTP方法
    allow_headers=["*"],  # 允许所有请求头
)


# 注册请求验证异常处理器
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_request, exc: RequestValidationError):
    """处理请求验证异常
    
    当请求数据验证失败时，返回标准化的错误响应。
    
    Args:
        _request: 请求对象
        exc: 验证异常
        
    Returns:
        JSONResponse: 包含验证错误信息的JSON响应
    """
    return error_response(400, "BAD_REQUEST", "request validation failed", {"errors": exc.errors()})


# 健康检查端点
@app.get("/health")
def health() -> dict[str, Any]:
    """健康检查接口
    
    返回应用程序的健康状态，包括MQTT连接状态和数据库配置。
    
    Returns:
        dict[str, Any]: 包含健康状态信息的字典
    """
    return {
        "ok": True,
        "mqtt_enabled": mqtt_bridge.enabled,
        "mqtt_connected": mqtt_bridge.connected,
        "database_url": settings.database_url,
    }


# 用户登录端点
@app.post("/api/auth/login", response_model=AuthLoginOut)
def auth_login(req: AuthLoginRequest) -> Any:
    """用户登录接口
    
    验证用户凭据并返回认证令牌和用户信息。
    
    Args:
        req: 登录请求，包含用户名和密码
        
    Returns:
        AuthLoginOut: 登录响应，包含认证令牌和用户信息
    """
    with get_session() as session:
        # 验证用户凭据
        user = login_user(session, req.account, req.password)
        if user is None:
            # 用户名或密码错误
            return error_response(401, "UNAUTHORIZED", "invalid account or password")
        # 生成认证令牌
        token = secrets.token_urlsafe(24)
        return AuthLoginOut(
            ok=True,
            token=token,
            user=AuthUserOut(username=user.username, email=user.email, role="admin"),
        )


# 获取所有设备列表
@app.get("/api/devices", response_model=list[DeviceOut])
def get_devices() -> list[DeviceOut]:
    """获取所有设备接口
    
    返回系统中所有设备的列表，包括设备的基本信息和在线状态。
    
    Returns:
        list[DeviceOut]: 设备列表
    """
    with get_session() as session:
        # 查询所有设备，按ID升序排列
        items = session.scalars(select(Device).order_by(Device.id.asc())).all()
        output: list[DeviceOut] = []
        for d in items:
            # 刷新设备的在线状态
            refresh_online_state(session, d)
            output.append(
                DeviceOut(
                    id=d.id,
                    name=d.name,
                    room=d.room,
                    online=d.online,
                    lastSeen=utc_iso(d.last_seen_ts),
                )
            )
        return output


# 获取设备状态
@app.get("/api/devices/{device_id}/status", response_model=StripStatusOut)
def get_device_status(device_id: str) -> Any:
    """获取设备状态接口
    
    返回指定设备的详细状态信息，包括电力参数和所有插孔的状态。
    
    Args:
        device_id: 设备ID
        
    Returns:
        StripStatusOut: 设备状态信息
    """
    with get_session() as session:
        # 查询设备和状态记录
        d = session.get(Device, device_id)
        s = session.get(StripStatus, device_id)
        if d is None or s is None:
            # 设备不存在
            return error_response(404, "NOT_FOUND", "device not found")

        # 刷新设备的在线状态
        refresh_online_state(session, d)
        # 解析插孔状态JSON
        try:
            sockets = json.loads(s.sockets_json)
        except Exception:
            sockets = []
        return StripStatusOut(
            ts=s.ts,
            online=d.online and s.online,
            total_power_w=s.total_power_w,
            voltage_v=s.voltage_v,
            current_a=s.current_a,
            sockets=sockets,
        )


# 获取遥测数据
@app.get("/api/telemetry")
def get_telemetry(
    device: str = Query(..., min_length=1),
    range: str = Query(..., pattern="^(60s|24h|7d|30d)$"),
) -> Any:
    """获取遥测数据接口
    
    返回指定设备在指定时间范围内的电力使用数据。
    
    Args:
        device: 设备ID
        range: 时间范围，可选值为"60s"、"24h"、"7d"或"30d"
        
    Returns:
        遥测数据点列表
    """
    with get_session() as session:
        # 检查设备是否存在
        if session.get(Device, device) is None:
            return error_response(404, "NOT_FOUND", "device not found")
        try:
            # 构建并返回遥测数据系列
            return build_telemetry_series(session, device, range)
        except ValueError:
            # 时间范围无效
            return error_response(400, "BAD_REQUEST", "range is invalid")


# 发送控制命令
@app.post("/api/strips/{device_id}/cmd", response_model=CmdSubmitOut)
async def post_cmd(device_id: str, req: CmdRequest) -> Any:
    """发送控制命令接口
    
    向指定设备发送控制命令，并返回命令提交结果。
    
    Args:
        device_id: 设备ID
        req: 命令请求，包含命令类型和参数
        
    Returns:
        CmdSubmitOut: 命令提交响应
    """
    with get_session() as session:
        # 检查设备是否存在
        if session.get(Device, device_id) is None:
            return error_response(404, "NOT_FOUND", "device not found")
        # 检查是否存在冲突的待处理命令
        if has_pending_conflict(session, device_id, req.socket):
            return error_response(409, "CMD_CONFLICT", "pending command exists for target")
        # 创建命令记录
        cmd = create_cmd_record(session, device_id, req)

    # 构建命令负载
    cmd_payload = {
        "cmdId": cmd.cmd_id,
        "ts": int(time.time()),
        "type": req.action.upper(),
        "socketId": req.socket,
        "payload": req.payload,
        "mode": req.mode,
        "duration": req.duration,
        "source": "web",
    }
    # 通过MQTT发布命令
    published = mqtt_bridge.publish_cmd(device_id, cmd_payload)
    if not published:
        # MQTT发布失败，更新命令状态并广播
        with get_session() as session:
            update_cmd_state(session, cmd.cmd_id, "failed", message="mqtt unavailable")
        await ws_manager.broadcast(
            {
                "type": "CMD_ACK",
                "cmdId": cmd.cmd_id,
                "state": "failed",
                "ts": int(time.time()),
                "updatedAt": int(time.time()),
                "message": "mqtt unavailable",
            }
        )

    return CmdSubmitOut(ok=True, cmdId=cmd.cmd_id, stripId=device_id, acceptedAt=int(time.time()))


# 获取命令状态
@app.get("/api/cmd/{cmd_id}", response_model=CmdStateOut)
def get_cmd(cmd_id: str) -> Any:
    """获取命令状态接口
    
    返回指定命令的执行状态和结果信息。
    
    Args:
        cmd_id: 命令ID
        
    Returns:
        CmdStateOut: 命令状态信息
    """
    with get_session() as session:
        # 查询命令状态
        state = get_cmd_state(session, cmd_id)
        if state is None:
            # 命令不存在
            return error_response(404, "NOT_FOUND", "cmd not found")
        return state


# 获取AI报告
@app.get("/api/rooms/{room_id}/ai_report", response_model=AIReportOut)
def get_ai_report(room_id: str, period: str = Query("7d")) -> Any:
    """获取AI分析报告接口
    
    返回指定房间在指定时间范围内的电力使用分析报告。
    
    Args:
        room_id: 房间ID
        period: 时间范围，可选值为"7d"或"30d"
        
    Returns:
        AIReportOut: AI分析报告
    """
    # 验证时间范围参数
    if period not in {"7d", "30d"}:
        return error_response(400, "BAD_REQUEST", "period is invalid")
    with get_session() as session:
        # 检查房间是否存在
        exists = session.scalar(select(Device.id).where(Device.room == room_id).limit(1))
        if exists is None:
            return error_response(404, "NOT_FOUND", "room not found")
        # 生成AI报告
        result = ai_report(session, room_id, period)
        return AIReportOut(**result)


# WebSocket端点
@app.websocket("/ws")
async def ws_endpoint(ws: WebSocket) -> None:
    """WebSocket端点
    
    处理WebSocket连接，用于实时推送设备状态更新和事件通知。
    
    Args:
        ws: WebSocket连接对象
    """
    # 接受WebSocket连接
    await ws_manager.connect(ws)
    try:
        # 持续接收消息，保持连接
        while True:
            await ws.receive_text()
    except WebSocketDisconnect:
        # 客户端主动断开连接
        ws_manager.disconnect(ws)
    except Exception:
        # 发生异常，断开连接
        ws_manager.disconnect(ws)
