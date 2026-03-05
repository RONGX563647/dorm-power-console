#!/usr/bin/env python3
import serial
import time
import sys

def test_baudrate(port, baudrate, duration=3):
    print(f"\n测试波特率: {baudrate}")
    try:
        ser = serial.Serial(
            port=port,
            baudrate=baudrate,
            timeout=1,
            write_timeout=1
        )
        
        print(f"  ✅ 连接成功")
        
        received_data = False
        start_time = time.time()
        
        while time.time() - start_time < duration:
            if ser.in_waiting > 0:
                data = ser.read(ser.in_waiting)
                try:
                    text = data.decode('utf-8', errors='replace')
                    print(f"  📨 收到数据: {text.strip()}")
                except:
                    print(f"  📨 收到二进制: {data.hex()}")
                received_data = True
            
            time.sleep(0.1)
        
        ser.close()
        return received_data
        
    except Exception as e:
        print(f"  ❌ 连接失败: {e}")
        return False

def main():
    port = '/dev/tty.usbserial-310'
    
    print("=" * 60)
    print("K210 串口自动检测工具")
    print("=" * 60)
    print(f"串口设备: {port}")
    print()
    
    baudrates = [115200, 9600, 57600, 230400, 38400, 19200]
    
    for baud in baudrates:
        if test_baudrate(port, baud):
            print(f"\n✅ 找到正确波特率: {baud}")
            break
    else:
        print("\n⚠️  未检测到数据，请检查:")
        print("   1. K210 是否正常供电并运行")
        print("   2. 串口线连接是否正确 (TX-RX交叉连接)")
        print("   3. K210 是否有输出程序")

if __name__ == "__main__":
    main()
