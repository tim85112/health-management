
	/*
		注意：user表格的插入數據是程式自動生成，user表要有數據才能執行part2! 不然就會報錯!
		如果不想插入測試數據可以先不執行part2!
	*/


use health_db
-- 創建 user 表
CREATE TABLE [user]
(
    [user_id]       INT PRIMARY KEY IDENTITY (1, 1),-- 自動遞增主鍵
    [name]          NVARCHAR(50)  NOT NULL,-- 使用者名稱
    [email]         NVARCHAR(100) NOT NULL,-- 電子郵件
    [password_hash] VARCHAR(255)  NOT NULL,-- 密碼哈希
    [gender]        CHAR(1),-- 性別，例如 'M' 或 'F'
    [bio]           NVARCHAR(MAX),-- 個人簡介
    [role]          VARCHAR(10) CHECK (role IN ('user', 'admin')),-- 角色，只能是 user 或 admin 
    [user_points]   INT           NOT NULL DEFAULT 0,	 -- 使用者點數，預設為 0 
		[last_login]    DATETIME	DEFAULT CURRENT_TIMESTAMP	-- 最後登入時間
);
GO

-- 創建 user_point 表
CREATE TABLE [user_point]
(
    [user_id]      INT PRIMARY KEY,
    [points]       INT DEFAULT (0),
    [last_updated] DATETIME
)
GO

-- 創建 product 表
CREATE TABLE [product]
(
    [id]             INT PRIMARY KEY IDENTITY (1, 1),
    [name]           NVARCHAR(255)  NOT NULL,
    [description]    NVARCHAR(1000),
    [price]          DECIMAL(10, 2) NOT NULL,
    [stock_quantity] INT            NOT NULL DEFAULT (0),
    [image_url]      NVARCHAR(500),
    [created_at]     DATETIME                DEFAULT CURRENT_TIMESTAMP,
    [updated_at]     DATETIME                DEFAULT CURRENT_TIMESTAMP
)
GO

-- 創建 coach 表
CREATE TABLE [coach]
(
    [id]        INT PRIMARY KEY IDENTITY (1, 1),
    [name]      NVARCHAR(255) NOT NULL,
    [expertise] NVARCHAR(255)
)
GO

-- 創建 course 表
CREATE TABLE [course]
(
    [id]           INT PRIMARY KEY IDENTITY (1, 1),
    [name]         NVARCHAR(255)  NOT NULL,
    [description]  NVARCHAR(1000) NOT NULL,
    [date]         DATE           NOT NULL,
    [coach_id]     INT            NOT NULL,
    [duration]     INT            NOT NULL,
    [max_capacity] INT            NOT NULL
)
GO

-- 創建 order 表
CREATE TABLE [order]
(
    [id]           INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]      INT            NOT NULL,
    [total_amount] DECIMAL(10, 2) NOT NULL,
    [status]       NVARCHAR(50) DEFAULT 'pending',
    [created_at]   DATETIME     DEFAULT CURRENT_TIMESTAMP
)
GO

-- 創建 order_item 表
CREATE TABLE [order_item]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [order_id]   INT            NOT NULL,
    [product_id] INT,
    [course_id]  INT,
    [quantity]   INT            NOT NULL DEFAULT (1),
    [subtotal]   DECIMAL(10, 2) NOT NULL
)
GO

-- 創建 cart_item 表
CREATE TABLE [cart_item]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]    INT NOT NULL,
    [product_id] INT,
    [course_id]  INT,
    [quantity]   INT NOT NULL DEFAULT (1),
    [added_at]   DATETIME     DEFAULT CURRENT_TIMESTAMP
)
GO

-- 創建 body_metric 表
CREATE TABLE [body_metric]
(
    [id]                  INT PRIMARY KEY IDENTITY (1,1),
    [user_id]             INT           NOT NULL,
    [weight]              DECIMAL(5, 2) NOT NULL,
    [body_fat]            DECIMAL(5, 2),
    [muscle_mass]         DECIMAL(5, 2),
    [waist_circumference] DECIMAL(5, 2),
    [hip_circumference]   DECIMAL(5, 2),
    [height]              DECIMAL(5, 2),
    [bmi]                 AS (weight / (height / 100) / (height / 100)) PERSISTED,
    [date_recorded]       DATETIME DEFAULT GETDATE()
)
GO

-- 創建 exercise_record 表
CREATE TABLE [exercise_record]
(
    [id]              INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]         INT            NOT NULL,
    [exercise_type]   NVARCHAR(50)   NOT NULL,
    [duration]        INT            NOT NULL,
    [calories_burned] DECIMAL(10, 2) NOT NULL,
    [date]            DATE           NOT NULL
)
GO

-- 創建 nutrition_record 表
CREATE TABLE [nutrition_record]
(
    [id]            INT PRIMARY KEY IDENTITY (1,1),
    [user_id]       INT           NOT NULL,
    [food_name]     NVARCHAR(255) NOT NULL,
    [calories]      INT           NOT NULL,
    [protein]       FLOAT         NOT NULL,
    [carbs]         FLOAT         NOT NULL,
    [fats]          FLOAT         NOT NULL,
    [mealtime]      NVARCHAR(50) CHECK ([mealtime] IN ('breakfast', 'lunch', 'dinner', 'snack')),
    [date_recorded] DATETIME DEFAULT GETDATE()
)
GO

-- 創建 fitness_goal 表
CREATE TABLE [fitness_goal]
(
    [id]                 INT PRIMARY KEY IDENTITY (1,1),
    [user_id]            INT      NOT NULL,
    [type]               NVARCHAR(50) CHECK ([type] IN ('weight_loss', 'muscle_gain', 'cardio', 'other')),
    [target_value]       FLOAT    NOT NULL,
    [current_progress]   FLOAT                                                                   DEFAULT 0,
    [unit]               NVARCHAR(20) CHECK ([unit] IN ('kg', '%', 'min', 'cal')),
    [start_date]         DATETIME                                                                DEFAULT GETDATE(),
    [end_date]           DATETIME NULL,
    [status]             NVARCHAR(20) CHECK ([status] IN ('in_progress', 'completed', 'failed')) DEFAULT 'in_progress',
    [frequency_goal]     INT      NULL,
    [frequency_duration] INT      NULL,
    [reference_table]    NVARCHAR(50) CHECK ([reference_table] IN ('exercise_record', 'body_metric')),
    CONSTRAINT [CK_end_date] CHECK (end_date IS NULL OR end_date >= start_date)
)
GO

-- 創建 achievement 表
CREATE TABLE [achievement]
(
    [id]            INT PRIMARY KEY IDENTITY (1,1),
    [user_id]       INT                                                NOT NULL,
    [type]          NVARCHAR(50) CHECK ([type] IN ('goal_completed', 'general_reward')),
    [title]         NVARCHAR(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
    [description]   NVARCHAR(500),
    [date_achieved] DATETIME DEFAULT GETDATE(),
    CONSTRAINT [UC_unique_achievement] UNIQUE ([user_id], [title])
)
GO

-- 創建 exercise_type_coefficient 表
CREATE TABLE [exercise_type_coefficient]
(
    [id]   INT PRIMARY KEY IDENTITY (1,1),
    [name] NVARCHAR(50)  NOT NULL,
    [met]  DECIMAL(5, 2) NOT NULL
)
GO

-- 創建 social_post 表
CREATE TABLE [social_post]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [category]   NVARCHAR(10)   NOT NULL,
    [title]      NVARCHAR(255)  NOT NULL,
    [content]    NVARCHAR(2000) NOT NULL,
    [user_id]    INT            NOT NULL,
    [created_at] DATETIME DEFAULT CURRENT_TIMESTAMP,
    [updated_at] DATETIME DEFAULT CURRENT_TIMESTAMP
)
GO

-- 創建 comment 表
CREATE TABLE [comment]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [post_id]    INT            NOT NULL,
    [user_id]    INT            NOT NULL,
    [text]       NVARCHAR(1000) NOT NULL,
    [created_at] DATETIME DEFAULT CURRENT_TIMESTAMP,
    [updated_at] DATETIME DEFAULT CURRENT_TIMESTAMP
)
GO

-- 添加外鍵約束
ALTER TABLE [user_point]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [order]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [order_item]
    ADD FOREIGN KEY ([order_id]) REFERENCES [order] ([id])
GO

ALTER TABLE [order_item]
    ADD FOREIGN KEY ([product_id]) REFERENCES [product] ([id])
GO

ALTER TABLE [order_item]
    ADD FOREIGN KEY ([course_id]) REFERENCES [course] ([id])
GO

ALTER TABLE [course]
    ADD FOREIGN KEY ([coach_id]) REFERENCES [coach] ([id])
GO

ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([product_id]) REFERENCES [product] ([id])
GO

ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([course_id]) REFERENCES [course] ([id])
GO

ALTER TABLE [exercise_record]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [social_post]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [comment]
    ADD FOREIGN KEY ([post_id]) REFERENCES [social_post] ([id])
GO

ALTER TABLE [comment]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id])
GO

ALTER TABLE [body_metric]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id]) ON DELETE CASCADE
GO

ALTER TABLE [nutrition_record]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id]) ON DELETE CASCADE
GO

ALTER TABLE [fitness_goal]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id]) ON DELETE CASCADE
GO

ALTER TABLE [achievement]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([user_id]) ON DELETE CASCADE
GO



-- 創建視圖
CREATE VIEW dashboard_stat AS
SELECT (SELECT COUNT(*) FROM [user] WHERE role = 'user')                             AS total_users,
       (SELECT COUNT(*) FROM exercise_record)                                        AS total_workouts,
       (SELECT SUM(duration) FROM exercise_record)                                   AS total_workout_minutes,
       (SELECT SUM(calories_burned) FROM exercise_record)                            AS total_calories_burned,
       (SELECT COUNT(*) FROM [user] WHERE DATEDIFF(DAY, last_login, GETDATE()) <= 7) AS active_users_this_week;
GO
