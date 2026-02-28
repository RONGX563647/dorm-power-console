"""数据库连接和会话管理模块

本模块负责设置数据库连接引擎、创建会话工厂和提供数据库会话管理功能。
使用SQLAlchemy ORM框架进行数据库操作，支持多种数据库后端。

使用方式:
    from app.db import get_session, Base, engine
    
    # 创建数据库表
    Base.metadata.create_all(bind=engine)
    
    # 使用上下文管理器获取会话
    with get_session() as session:
        # 执行数据库操作
        ...
"""

from __future__ import annotations

from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, declarative_base, sessionmaker

from .config import settings

# SQLite特定的连接参数
# 当使用SQLite数据库时，需要设置check_same_thread为False，
# 允许在不同线程间共享同一个连接
connect_args = {"check_same_thread": False} if settings.database_url.startswith("sqlite") else {}

# 创建数据库引擎
# future=True表示使用SQLAlchemy 2.0风格的API
engine = create_engine(settings.database_url, connect_args=connect_args, future=True)

# 创建会话工厂
# autoflush=False: 不在查询前自动刷新会话
# autocommit=False: 不自动提交事务
# expire_on_commit=False: 提交后不过期对象，允许在事务外访问对象属性
# future=True: 使用SQLAlchemy 2.0风格的API
SessionLocal = sessionmaker(
    bind=engine,
    autoflush=False,
    autocommit=False,
    expire_on_commit=False,
    future=True,
)

# 创建声明式基类
# 所有ORM模型都应继承自此基类
Base = declarative_base()


@contextmanager
def get_session() -> Generator[Session, None, None]:
    """获取数据库会话的上下文管理器
    
    此函数提供了一个安全的数据库会话管理方式，自动处理会话的提交、回滚和关闭。
    使用上下文管理器可以确保数据库会话在使用后被正确关闭，避免资源泄漏。
    
    Yields:
        Session: SQLAlchemy数据库会话对象
        
    使用示例:
        with get_session() as session:
            user = session.query(User).first()
            user.name = "New Name"
            # 退出上下文时会自动提交事务
    """
    session = SessionLocal()
    try:
        yield session
        # 如果没有异常发生，提交事务
        session.commit()
    except Exception:
        # 发生异常时回滚事务
        session.rollback()
        raise
    finally:
        # 无论成功与否，最终都关闭会话
        session.close()
