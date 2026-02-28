"""WebSocket连接管理模块

本模块提供WebSocket连接管理功能，用于实时推送设备状态更新和事件通知。
通过广播机制，可以将消息同时发送给所有连接的客户端。

使用方式:
    from app.ws import ws_manager
    
    # 在路由中接受WebSocket连接
    @app.websocket("/ws")
    async def websocket_endpoint(websocket: WebSocket):
        await ws_manager.connect(websocket)
        try:
            while True:
                data = await websocket.receive_text()
                # 处理接收到的消息
                ...
        finally:
            ws_manager.disconnect(websocket)
    
    # 广播消息给所有连接的客户端
    await ws_manager.broadcast({"type": "status_update", "data": {...}})
"""

from __future__ import annotations

import json
from typing import Any

from fastapi import WebSocket


class WSManager:
    """WebSocket连接管理器
    
    管理所有WebSocket连接，提供连接、断开和广播功能。
    使用集合存储活跃连接，确保连接唯一性。
    """
    
    def __init__(self) -> None:
        """初始化WebSocket管理器
        
        创建一个空的连接集合，用于存储所有活跃的WebSocket连接。
        """
        # 使用集合存储所有活跃的WebSocket连接
        self._clients: set[WebSocket] = set()

    async def connect(self, ws: WebSocket) -> None:
        """接受并注册新的WebSocket连接
        
        接受WebSocket连接请求，并将其添加到活跃连接集合中。
        
        Args:
            ws: FastAPI WebSocket连接对象
        """
        # 接受WebSocket连接请求
        await ws.accept()
        # 将连接添加到活跃连接集合
        self._clients.add(ws)

    def disconnect(self, ws: WebSocket) -> None:
        """断开并移除WebSocket连接
        
        从活跃连接集合中移除指定的WebSocket连接。
        使用discard而不是remove，避免在连接不存在时抛出异常。
        
        Args:
            ws: 要断开的FastAPI WebSocket连接对象
        """
        # 从活跃连接集合中移除连接
        self._clients.discard(ws)

    async def broadcast(self, payload: dict[str, Any]) -> None:
        """向所有连接的客户端广播消息
        
        将指定的消息发送给所有活跃的WebSocket连接。
        如果发送失败（例如连接已断开），则自动清理失效的连接。
        
        Args:
            payload: 要广播的消息字典，将被序列化为JSON格式
        """
        # 如果没有活跃连接，直接返回
        if not self._clients:
            return
        
        # 将消息字典序列化为JSON字符串
        text = json.dumps(payload, ensure_ascii=False)
        
        # 存储失效的连接，用于后续清理
        stale: list[WebSocket] = []
        
        # 遍历所有活跃连接，发送消息
        for ws in self._clients:
            try:
                # 尝试发送消息
                await ws.send_text(text)
            except Exception:
                # 如果发送失败，将连接标记为失效
                stale.append(ws)
        
        # 清理所有失效的连接
        for ws in stale:
            self._clients.discard(ws)


# 创建全局WebSocket管理器实例
# 应用程序通过导入此对象管理所有WebSocket连接
ws_manager = WSManager()
