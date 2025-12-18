"""
簡單截圖測試 - 用於驗證 UI 狀態
"""
from playwright.sync_api import sync_playwright
import os

# 確保截圖目錄存在
os.makedirs('/tmp/screenshots', exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()

    # 設定 viewport
    page.set_viewport_size({"width": 1280, "height": 720})

    # 先訪問首頁以設定 localStorage
    page.goto('http://localhost:4200')
    page.wait_for_load_state('networkidle')
    
    # 截圖初始狀態
    page.screenshot(path='/tmp/screenshots/01_initial.png', full_page=True)
    print(f"Initial URL: {page.url}")
    
    # 輸出頁面 HTML 內容
    content = page.content()
    print(f"Page content length: {len(content)}")
    
    # 檢查是否有錯誤訊息
    body_text = page.locator('body').inner_text()
    print(f"Body text: {body_text[:500] if body_text else 'Empty'}")

    browser.close()
    print("Done!")
