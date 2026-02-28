"""数据模型定义模块

本模块定义了应用程序的所有数据库模型，包括设备、状态、遥测数据、
命令记录和用户账户等。每个模型对应数据库中的一张表。

使用方式:
    from app.models import Device, Telemetry, UserAccount
    
    # 创建新设备
    device = Device(id="dev001", name="主控板", room="101")
    
    # 查询设备
    devices = session.query(Device).all()
"""

from __future__ import annotations

from sqlalchemy import BigInteger, Boolean, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


class Device(Base):
    """设备模型
    
    表示宿舍中的物理设备，如智能插座或控制板。
    记录设备的基本信息和在线状态。
    """
    __tablename__ = "devices"

    # 设备唯一标识符，作为主键
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    # 设备名称，如"主控板"或"智能插座"
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    # 设备所在房间号
    room: Mapped[str] = mapped_column(String(64), nullable=False)
    # 设备在线状态，True表示在线，False表示离线
    online: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # 设备最后上线时间戳（Unix时间戳，秒）
    last_seen_ts: Mapped[int] = mapped_column(BigInteger, default=0, nullable=False)


class StripStatus(Base):
    """插座状态模型
    
    记录智能插座的实时状态，包括电力参数和各插孔的状态。
    每个设备对应一条记录，用于快速查询设备的最新状态。
    """
    __tablename__ = "strip_status"

    # 关联的设备ID，作为主键
    device_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    # 状态更新时间戳（Unix时间戳，秒）
    ts: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 设备在线状态
    online: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # 总功率（瓦特）
    total_power_w: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    # 电压（伏特）
    voltage_v: Mapped[float] = mapped_column(Float, default=220.0, nullable=False)
    # 电流（安培）
    current_a: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    # 各插孔状态的JSON字符串，包含每个插孔的开关状态和功率信息
    sockets_json: Mapped[str] = mapped_column(Text, default="[]", nullable=False)


class Telemetry(Base):
    """遥测数据模型
    
    记录设备的电力使用历史数据，用于监控和分析。
    每次设备上报数据时都会创建一条新记录。
    """
    __tablename__ = "telemetry"

    # 记录唯一ID，自增主键
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    # 关联的设备ID，建立索引以提高查询效率
    device_id: Mapped[str] = mapped_column(String(64), index=True, nullable=False)
    # 遥测数据时间戳（Unix时间戳，秒），建立索引以提高查询效率
    ts: Mapped[int] = mapped_column(BigInteger, index=True, nullable=False)
    # 功率（瓦特）
    power_w: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    # 电压（伏特）
    voltage_v: Mapped[float] = mapped_column(Float, default=220.0, nullable=False)
    # 电流（安培）
    current_a: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)


class CommandRecord(Base):
    """命令记录模型
    
    记录发送给设备的控制命令及其执行状态。
    每个命令都有一个生命周期，包括创建、发送、执行和完成。
    """
    __tablename__ = "cmd_records"

    # 命令唯一标识符，作为主键
    cmd_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    # 目标设备ID，建立索引以提高查询效率
    device_id: Mapped[str] = mapped_column(String(64), index=True, nullable=False)
    # 目标插孔编号，None表示整个设备
    socket: Mapped[int | None] = mapped_column(Integer, nullable=True)
    # 命令动作类型，如"toggle"、"on"、"off"等
    action: Mapped[str] = mapped_column(String(64), nullable=False)
    # 命令负载的JSON字符串，包含命令的详细参数
    payload_json: Mapped[str] = mapped_column(Text, default="{}", nullable=False)
    # 命令状态，如"pending"、"sent"、"completed"、"failed"等
    state: Mapped[str] = mapped_column(String(16), default="pending", nullable=False)
    # 命令状态消息，包含执行结果或错误信息
    message: Mapped[str] = mapped_column(String(255), default="", nullable=False)
    # 命令创建时间戳（Unix时间戳，秒）
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 命令最后更新时间戳（Unix时间戳，秒）
    updated_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 命令过期时间戳（Unix时间戳，秒），过期后命令将被取消
    expires_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 命令执行持续时间（毫秒），None表示未完成或无意义
    duration_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)


class UserAccount(Base):
    """用户账户模型
    
    记录系统用户的信息，包括管理员和普通用户。
    支持用户登录和密码重置功能。
    """
    __tablename__ = "user_accounts"

    # 用户名，作为主键
    username: Mapped[str] = mapped_column(String(64), primary_key=True)
    # 用户邮箱，唯一且建立索引
    email: Mapped[str] = mapped_column(String(128), unique=True, index=True, nullable=False)
    # 密码哈希值，不存储明文密码
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    # 用户角色，如"admin"或"user"
    role: Mapped[str] = mapped_column(String(16), default="admin", nullable=False)
    # 密码重置代码的哈希值
    reset_code_hash: Mapped[str] = mapped_column(String(255), default="", nullable=False)
    # 密码重置代码过期时间戳（Unix时间戳，秒）
    reset_expires_at: Mapped[int] = mapped_column(BigInteger, default=0, nullable=False)
    # 账户创建时间戳（Unix时间戳，秒）
    created_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 账户最后更新时间戳（Unix时间戳，秒）
    updated_at: Mapped[int] = mapped_column(BigInteger, nullable=False)
