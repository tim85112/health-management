# 健康管理系統 (Health Management System)

健康管理系統是一个基于 Spring Boot 3.4.3 的後端專案，提供會員管理、健康追踪、課程管理等功能的 RESTful API。

## 技術棧

- Java 17
- Spring Boot 3.4.3
- Spring Security + JWT
- Spring Data JPA
- SQL Server
- Swagger API Documentation

## 專案結構

MVC 架構：

- Controller：處理 HTTP 请求
- Service：實現业务逻辑
- DAO：資料訪問层
- Model：資料模型

## 如何啟動

### 前提條件

- Java 17+
- Maven 3.6+
- SQL Server 2019+

### 步驟

1.  git clone https://github.com/yourusername/health-management.git

2.  設定資料库

    - 创建資料库`HealthManagement`
    - 修改`application.properties`中的資料库設定

3.  建置並執行專案
    mvn clean install
    mvn spring-boot:run

4.  訪問 API 文件
    http://localhost:8080/swagger-ui/index.html

## API 端點

- 認證：/auth/
  - 註冊：POST /auth/register
  - 登入：POST /auth/login
- 用戶管理：/users/
  - 取得用戶：GET /users/{id}
  - 更新用戶：PUT /users/{id}
  - 刪除用戶：DELETE /users/{id}
- 商品管理：/products/
- 訂單管理：/orders/
- 課程管理：/courses/
- 健身追蹤：/tracking/
- 社交論壇：/forums/

## 開發規範

1. 命名規範

   - 使用 PascalCase 命名
   - 使用 camelCase 命名方法與變數

2. Git 工作流

   - `main`：正式環境
   - `dev`：開發環境
   - `feature/xxx`：新功能分支

3. API 回應格式

json
{
"status": "success",
"data": {

}
}

## 團隊分工

- 負責人：會員
- 組員 A：商城
- 組員 B：課程
- 組員 C：健身
- 組員 D：社交
