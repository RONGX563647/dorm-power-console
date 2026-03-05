#!/usr/bin/env python3
import serial
import time
import sys

def test_k210_connection():
    port = '/dev/tty.usbserial-310'
    baudrate = 115200
    
    print(f"正在尝试连接 K210 开发板...")
    print(f"串口: {port}")
    print(f"波特率: {baudrate}")
    print("-" * 50)
    
    try:
        ser = serial.Serial(
            port=port,
            baudrate=baudrate,
            timeout=2,
            write_timeout=2
        )
        
        print(f"✅ 串口连接成功!")
        print(f"串口信息: {ser}")
        print("-" * 50)
        
        print("\n正在尝试读取数据 (按 Ctrl+C 退出)...")
        print("=" * 50)
        
        start_time = time.time()
        while time.time() - start_time < 10:
            if ser.in_waiting > 0:
                try:
                    data = ser.read(ser.in_waiting)
                    try:
                        text = data.decode('utf-8', errors='replace')
                        print(f"{text}", end='', flush=True)
                    except:
                        print(f"[二进制数据: {data.hex()}]", end='', flush=True)
                except Exception as e:
                    print(f"\n读取错误: {e}")
            time.sleep(0.1)
        
        print("\n" + "=" * 50)
        print("\n尝试发送 AT 命令...")
        ser.write(b'AT\r\n')
        time.sleep(1)
        
        if ser.in_waiting > 0:
            response = ser.read(ser.in_waiting)
            print(f"响应: {response}")
        
        ser.close()
        print("\n✅ 测试完成!")
        
    except serial.SerialException as e:
        print(f"❌ 串口连接失败: {e}")
        print("\n请检查:")
        print("1. K210 开发板是否已连接")
        print("2. 串口设备是否正确")
        print("3. 波特率是否匹配")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n\n用户中断")
        if 'ser' in locals() and ser.is_open:
            ser.close()
        sys.exit(0)

if __name__ == "__main__":
    test_k210_connection()
