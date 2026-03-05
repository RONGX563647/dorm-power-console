import requests
import json

# API Base URL
BASE_URL = "http://localhost:8000"

# Test credentials
TEST_ACCOUNT = "admin"
TEST_PASSWORD = "123456"

class APITest:
    def __init__(self):
        self.token = None
    
    def login(self):
        """Test login API"""
        print("=== Testing Login API ===")
        url = f"{BASE_URL}/api/auth/login"
        data = {
            "account": TEST_ACCOUNT,
            "password": TEST_PASSWORD
        }
        
        try:
            response = requests.post(url, json=data)
            if response.status_code == 200:
                result = response.json()
                self.token = result.get("token")
                print(f"Login successful! Token: {self.token[:20]}...")
                return True
            else:
                print(f"Login failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"Error during login: {e}")
            return False
    
    def get_headers(self):
        """Get headers with authorization token"""
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
    
    def test_devices(self):
        """Test device API endpoints"""
        print("\n=== Testing Device API ===")
        
        # Get all devices
        url = f"{BASE_URL}/api/devices"
        response = requests.get(url, headers=self.get_headers())
        print(f"Get all devices: {response.status_code}")
        if response.status_code == 200:
            devices = response.json()
            print(f"Found {len(devices)} devices")
            if devices:
                device_id = devices[0].get("id")
                print(f"Testing device {device_id}")
                
                # Get device detail
                detail_url = f"{BASE_URL}/api/devices/{device_id}"
                detail_response = requests.get(detail_url, headers=self.get_headers())
                print(f"Get device detail: {detail_response.status_code}")
    
    def test_telemetry(self):
        """Test telemetry API endpoints"""
        print("\n=== Testing Telemetry API ===")
        
        # Get telemetry data
        url = f"{BASE_URL}/api/telemetry"
        params = {
            "device": "1",
            "range": "24h"
        }
        response = requests.get(url, params=params, headers=self.get_headers())
        print(f"Get telemetry data: {response.status_code}")
        
        # Get statistics
        stats_url = f"{BASE_URL}/api/telemetry/statistics"
        stats_response = requests.get(stats_url, params=params, headers=self.get_headers())
        print(f"Get telemetry statistics: {stats_response.status_code}")
    
    def test_power_control(self):
        """Test power control API endpoints"""
        print("\n=== Testing Power Control API ===")
        
        # Get power history
        url = f"{BASE_URL}/api/power-control/history/1"
        response = requests.get(url, headers=self.get_headers())
        print(f"Get power history: {response.status_code}")
    
    def test_billing(self):
        """Test billing API endpoints"""
        print("\n=== Testing Billing API ===")
        
        # Get room balance
        url = f"{BASE_URL}/api/billing/balance/1"
        response = requests.get(url, headers=self.get_headers())
        print(f"Get room balance: {response.status_code}")
        
        # Get consumption
        consumption_url = f"{BASE_URL}/api/billing/consumption/1"
        consumption_response = requests.get(consumption_url, headers=self.get_headers())
        print(f"Get consumption: {consumption_response.status_code}")
        
        # Get bills
        bills_url = f"{BASE_URL}/api/billing/bills/1"
        bills_response = requests.get(bills_url, headers=self.get_headers())
        print(f"Get bills: {bills_response.status_code}")
    
    def test_monitor(self):
        """Test monitoring API endpoints"""
        print("\n=== Testing Monitoring API ===")
        
        # Get system status
        url = f"{BASE_URL}/api/admin/monitor/system"
        response = requests.get(url, headers=self.get_headers())
        print(f"Get system status: {response.status_code}")
        
        # Get device status
        device_status_url = f"{BASE_URL}/api/admin/monitor/devices"
        device_status_response = requests.get(device_status_url, headers=self.get_headers())
        print(f"Get device status: {device_status_response.status_code}")
        
        # Get active alerts
        alerts_url = f"{BASE_URL}/api/admin/monitor/alerts"
        alerts_response = requests.get(alerts_url, headers=self.get_headers())
        print(f"Get active alerts: {alerts_response.status_code}")
        
        # Get system logs
        logs_url = f"{BASE_URL}/api/admin/monitor/logs"
        logs_response = requests.get(logs_url, headers=self.get_headers())
        print(f"Get system logs: {logs_response.status_code}")
    
    def register(self):
        """Test register API"""
        print("=== Testing Register API ===")
        url = f"{BASE_URL}/api/auth/register"
        data = {
            "username": "testuser123",
            "email": "test123@example.com",
            "password": "123456"
        }
        
        try:
            response = requests.post(url, json=data)
            if response.status_code == 200:
                result = response.json()
                self.token = result.get("token")
                print(f"Register successful! Token: {self.token[:20]}...")
                return True
            else:
                print(f"Register failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"Error during register: {e}")
            return False
    
    def run_all_tests(self):
        """Run all API tests"""
        print("Starting API tests...")
        
        if self.login():
            self.test_devices()
            self.test_telemetry()
            self.test_power_control()
            self.test_billing()
            self.test_monitor()
            print("\n=== All tests completed ===")
        else:
            print("Login failed, trying to register...")
            if self.register():
                self.test_devices()
                self.test_telemetry()
                self.test_power_control()
                self.test_billing()
                self.test_monitor()
                print("\n=== All tests completed ===")
            else:
                print("Register failed, cannot run tests")

if __name__ == "__main__":
    test = APITest()
    test.run_all_tests()
