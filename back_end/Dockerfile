# 使用Python 3.11精简版作为基础镜像
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置Python环境变量
# PYTHONDONTWRITEBYTECODE=1: 不生成.pyc文件
# PYTHONUNBUFFERED=1: 禁用Python输出缓冲
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# 复制依赖文件到容器
COPY requirements.txt ./

# 安装Python依赖包
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码到容器
COPY app ./app

# 暴露应用端口
EXPOSE 8000

# 启动应用服务器
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

