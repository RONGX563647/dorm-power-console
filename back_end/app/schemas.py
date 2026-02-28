"""数据模式定义模块

本模块定义了API的输入输出数据模式，使用Pydantic进行数据验证和序列化。
这些模式用于确保API请求和响应的数据格式正确，并提供自动的API文档生成。

使用方式:
    from app.schemas import DeviceOut, CmdRequest, AuthLoginOut
    
    # 创建设备输出对象
    device = DeviceOut(id="dev001", name="主控板", room="101", online=True, lastSeen="2023-01-01T00:00:00Z")
    
    # 验证命令请求
    cmd = CmdRequest(socket=1, action="toggle")
"""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class SocketStatus(BaseModel):
    """单个插孔状态模式
    
    表示智能插座上单个插孔的状态信息，包括开关状态和功率消耗。
    """
    # 插孔编号，从0开始
    id: int
    # 插孔开关状态，True表示开启，False表示关闭
    on: bool
    # 插孔当前功率消耗（瓦特）
    power_w: float = 0.0
    # 关联的设备名称，默认为"Unknown"
    device: str = "Unknown"


class DeviceOut(BaseModel):
    """设备输出模式
    
    表示设备的基本信息和在线状态，用于API响应。
    """
    # 设备唯一标识符
    id: str
    # 设备名称
    name: str
    # 设备所在房间号
    room: str
    # 设备在线状态
    online: bool
    # 设备最后上线时间，ISO 8601格式的字符串
    lastSeen: str


class StripStatusOut(BaseModel):
    """插座状态输出模式
    
    表示智能插座的完整状态信息，包括电力参数和所有插孔的状态。
    """
    # 状态更新时间戳（Unix时间戳，秒）
    ts: int
    # 设备在线状态
    online: bool
    # 总功率消耗（瓦特）
    total_power_w: float
    # 电压（伏特）
    voltage_v: float
    # 电流（安培）
    current_a: float
    # 所有插孔的状态列表
    sockets: list[SocketStatus]


class TelemetryPointOut(BaseModel):
    """遥测数据点输出模式
    
    表示单个时间点的电力使用数据，用于历史数据查询和图表展示。
    """
    # 遥测数据时间戳（Unix时间戳，秒）
    ts: int
    # 功率消耗（瓦特）
    power_w: float


class CmdRequest(BaseModel):
    """命令请求模式
    
    表示发送给设备的控制命令请求，包含命令类型和参数。
    """
    # 目标插孔编号，None表示整个设备
    socket: int | None = None
    # 命令动作类型，如"toggle"、"on"、"off"等
    action: str
    # 命令执行模式，如"auto"或"manual"
    mode: str | None = None
    # 命令持续时间，格式为"HH:MM:SS"
    duration: str | None = None
    # 命令负载，包含其他命令参数
    payload: dict[str, Any] = Field(default_factory=dict)


class CmdSubmitOut(BaseModel):
    """命令提交响应模式
    
    表示命令提交后的响应信息，确认命令已被系统接受。
    """
    # 命令提交是否成功
    ok: bool
    # 命令唯一标识符
    cmdId: str
    # 目标设备ID
    stripId: str
    # 命令被接受的时间戳（Unix时间戳，秒）
    acceptedAt: int


class CmdStateOut(BaseModel):
    """命令状态输出模式
    
    表示命令的执行状态和结果信息。
    """
    # 命令唯一标识符
    cmdId: str
    # 命令状态，可以是"pending"、"success"、"failed"、"timeout"或"cancelled"
    state: Literal["pending", "success", "failed", "timeout", "cancelled"]
    # 命令状态最后更新时间戳（Unix时间戳，秒）
    updatedAt: int
    # 命令状态消息，包含执行结果或错误信息
    message: str = ""
    # 命令执行持续时间（毫秒），None表示未完成或无意义
    durationMs: int | None = None


class AIReportOut(BaseModel):
    """AI报告输出模式
    
    表示AI分析生成的电力使用报告，包含摘要、异常和建议。
    """
    # 房间ID
    room_id: str
    # 报告时间范围，如"2023-01"或"2023-W01"
    period: str
    # 报告摘要
    summary: str
    # 发现的异常列表
    anomalies: list[str]
    # 改进建议列表
    suggestions: list[str]


class AuthLoginRequest(BaseModel):
    """认证登录请求模式
    
    表示用户登录请求，包含用户名和密码。
    """
    # 用户名或邮箱
    account: str = Field(min_length=3, max_length=128)
    # 用户密码
    password: str = Field(min_length=6, max_length=128)


class AuthUserOut(BaseModel):
    """认证用户输出模式
    
    表示登录成功后的用户信息。
    """
    # 用户名
    username: str
    # 用户邮箱
    email: str
    # 用户角色，目前仅支持"admin"
    role: Literal["admin"]


class AuthLoginOut(BaseModel):
    """认证登录响应模式
    
    表示登录成功后的响应，包含认证令牌和用户信息。
    """
    # 登录是否成功
    ok: bool
    # 认证令牌，用于后续API请求的身份验证
    token: str
    # 登录用户的信息
    user: AuthUserOut
