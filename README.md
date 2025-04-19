![image](https://github.com/user-attachments/assets/3beb153a-6824-4878-b2ee-f8f7d52293e3)

# 享健你健康管理系統 (Health Management System)

健康管理系統是一個基於 Spring Boot 3.2.4 的後端專案，提供會員管理、健康追蹤、課程管理等功能的 RESTful API。

前端項目連結：https://github.com/EEIT94Team2/HealthManagement-vue

## 技術棧

- Java 17
- Spring Boot 3.2.4
- Spring Security + JWT (0.11.5)...
- Spring Data JPA
- SQL Server
- Swagger API 文檔 (Springdoc OpenAPI 2.5.0)
- Maven 構建工具

## 專案結構DD

標準 Spring MVC 架構：

- Controller：處理 HTTP 請求，位於 `com.healthmanagement.controller`
- Service：實現業務邏輯，位於 `com.healthmanagement.service`
- DAO：數據訪問層，位於 `com.healthmanagement.dao`
- Model：數據模型，位於 `com.healthmanagement.model`
- DTO：數據傳輸對象，位於 `com.healthmanagement.dto`
- Config：配置類，位於 `com.healthmanagement.config`
- Security：安全相關，位於 `com.healthmanagement.security`
- Exception：異常處理，位於 `com.healthmanagement.exception`
- Util：工具類，位於 `com.healthmanagement.util`

## 如何啟動

### 前提條件

- Java 17+
- Maven 3.6+
- SQL Server 2019+

### 步驟

1. 複製此專案

   ```
   git clone https://github.com/your-organization/health-management.git
   ```

2. 設定資料庫

   - 創建資料庫 `HealthManagement`
   - 修改 `src/main/resources/application.properties` 中的資料庫設定

3. 建置並執行專案

   ```
   mvn clean install
   mvn spring-boot:run
   ```

4. 訪問 API 文檔
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

## API 端點

- 認證：`/auth/`
  - 註冊：POST `/auth/register`
  - 登入：POST `/auth/login`
- 用戶管理：`/users/`
  - 取得用戶：GET `/users/{id}`
  - 更新用戶：PUT `/users/{id}`
  - 刪除用戶：DELETE `/users/{id}`
- 商品管理：`/product/`
- 訂單管理：`/order/`
- 購物車管理：`/cart/`
- 課程管理：`/courses/`
- 健身追蹤：`/tracking/`
- 社交論壇：`/forums/`

## 開發規範

1. 命名規範

   - 類使用 PascalCase 命名
   - 方法與變數使用 camelCase 命名
   - 常量使用 UPPER_SNAKE_CASE 命名

2. Git 工作流

   - `main`：正式環境
   - `dev`：開發環境
   - `feature/xxx`：新功能分支

3. API 回應格式

```json
{
  "status": "success",
  "data": {
    // 返回數據
  }
}
```

## 團隊分工

- 負責人：會員系統
- 組員 A：電商功能（商品、訂單、購物車）
- 組員 B：課程系統
- 組員 C：健身追蹤系統
- 組員 D：社交論壇系統
