"""配置模块

本模块负责加载和管理应用程序的所有配置项，包括数据库连接、MQTT设置、
管理员账户等。配置项从环境变量中读取，如果未设置则使用默认值。

使用方式:
    from app.config import settings
    print(settings.host)  # 获取后端服务监听地址
"""

from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

# 从.env文件中加载环境变量
load_dotenv()


def _to_bool(value: str | None, default: bool = False) -> bool:
    """将字符串值转换为布尔值
    
    Args:
        value: 要转换的字符串值
        default: 如果value为None时返回的默认值
        
    Returns:
        转换后的布尔值。如果value为"1"、"true"、"yes"或"on"（不区分大小写），则返回True，否则返回False
    """
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    """应用程序设置类
    
    使用dataclass定义所有配置项，frozen=True确保配置项在运行时不可变。
    所有配置项都可以通过环境变量覆盖，每个配置项都有合理的默认值。
    """
    # 后端服务配置
    host: str = os.getenv("BACKEND_HOST", "0.0.0.0")  # 后端服务监听地址，默认监听所有网络接口
    port: int = int(os.getenv("BACKEND_PORT", "8000"))  # 后端服务监听端口，默认为8000
    
    # 数据库配置
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./iot_backend.db")  # 数据库连接URL，默认使用SQLite
    
    # MQTT配置
    mqtt_enabled: bool = _to_bool(os.getenv("MQTT_ENABLED"), False)  # 是否启用MQTT功能，默认禁用
    mqtt_host: str = os.getenv("MQTT_HOST", "127.0.0.1")  # MQTT代理服务器地址，默认为本地
    mqtt_port: int = int(os.getenv("MQTT_PORT", "1883"))  # MQTT代理服务器端口，默认为1883
    mqtt_username: str = os.getenv("MQTT_USERNAME", "")  # MQTT连接用户名，默认为空
    mqtt_password: str = os.getenv("MQTT_PASSWORD", "")  # MQTT连接密码，默认为空
    mqtt_topic_prefix: str = os.getenv("MQTT_TOPIC_PREFIX", "dorm").strip("/") or "dorm"  # MQTT主题前缀，默认为"dorm"
    
    # 管理员账户配置
    admin_username: str = os.getenv("ADMIN_USERNAME", "admin")  # 管理员用户名，默认为"admin"
    admin_email: str = os.getenv("ADMIN_EMAIL", "admin@dorm.local")  # 管理员邮箱，默认为"admin@dorm.local"
    admin_password: str = os.getenv("ADMIN_PASSWORD", "admin123")  # 管理员密码，默认为"admin123"
    
    # 超时配置
    cmd_timeout_seconds: int = int(os.getenv("CMD_TIMEOUT_SECONDS", "30"))  # 命令执行超时时间（秒），默认为30秒
    online_timeout_seconds: int = int(os.getenv("ONLINE_TIMEOUT_SECONDS", "60"))  # 设备在线状态超时时间（秒），默认为60秒


# 创建全局设置实例，应用程序通过导入此对象访问所有配置
settings = Settings()
