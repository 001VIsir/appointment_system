"""
测试会话 Cookie 问题
"""
from playwright.sync_api import sync_playwright
import json

def test_session_cookie():
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()

        # 监听所有请求和响应
        print("=== 开始测试 ===\n")

        # 1. 先尝试注册
        print("1. 注册新用户...")
        register_data = {
            "username": "testuser_debug",
            "password": "password123",
            "email": "test_debug@example.com",
            "role": "USER"
        }

        response = context.post(
            "http://localhost:8080/api/auth/register",
            data=json.dumps(register_data),
            headers={"Content-Type": "application/json"}
        )
        print(f"注册响应状态: {response.status}")
        print(f"注册响应内容: {response.text()}")

        # 2. 检查 cookie
        cookies = context.cookies()
        print(f"\n2. 当前 Cookies: {cookies}")

        # 3. 尝试登录
        print("\n3. 尝试登录...")
        login_data = {
            "username": "testuser_debug",
            "password": "password123"
        }

        response = context.post(
            "http://localhost:8080/api/auth/login",
            data=json.dumps(login_data),
            headers={"Content-Type": "application/json"}
        )
        print(f"登录响应状态: {response.status}")
        print(f"登录响应头: {dict(response.headers)}")
        login_text = response.text()
        print(f"登录响应内容: {login_text}")

        # 4. 再次检查 cookie
        cookies = context.cookies()
        print(f"\n4. 登录后 Cookies: {cookies}")

        # 5. 尝试访问受保护的 API
        print("\n5. 尝试访问受保护的API...")
        response = context.get(
            "http://localhost:8080/api/users/me",
            headers={"Content-Type": "application/json"}
        )
        print(f"API响应状态: {response.status}")
        print(f"API响应内容: {response.text()}")

        # 保持浏览器打开
        print("\n=== 测试完成，按回车结束 ===")
        input()

        browser.close()

if __name__ == "__main__":
    test_session_cookie()
