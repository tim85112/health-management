/*
    注意：user表格的插入數據是程式自動生成，user表要有數據才能執行part2! 不然就會報錯!
    如果不想插入測試數據可以先不執行part2!
*/
USE HealthManagement

-- 創建 users 表
CREATE TABLE [users]
(
    [user_id]       INT PRIMARY KEY IDENTITY (1, 1),								-- 自動遞增主鍵
    [name]          NVARCHAR(50)  NOT NULL,											-- 使用者名稱
    [email]         NVARCHAR(100) NOT NULL,											-- 電子郵件
    [password_hash] VARCHAR(255)  NOT NULL,											-- 密碼哈希
    [gender]        CHAR(1),														-- 性別，例如 'M' 或 'F'
    [bio]           NVARCHAR(MAX),													-- 個人簡介
    [role]          VARCHAR(10) CHECK (role IN ('user', 'admin', 'coach', 'guest')),-- 角色
    [user_points]   INT           NOT NULL DEFAULT 0,								-- 使用者點數
    [last_login]    DATETIME               DEFAULT CURRENT_TIMESTAMP,				-- 最後登入時間
    [consecutive_login_days] INT DEFAULT 0,								        -- 連續登入天數
);
GO

-- 創建 user_point 表
CREATE TABLE [user_point]
(
    [user_id]      INT PRIMARY KEY,
    [points]       INT DEFAULT (0),
    [last_updated] DATETIME
);
GO

CREATE TABLE reset_tokens (
    id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    CONSTRAINT FK_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);
GO

-- 創建 [course] 表
CREATE TABLE [course]
(
    [id] INT PRIMARY KEY IDENTITY (1, 1),
    [name] VARCHAR(255) NOT NULL,
    [description] TEXT,
    [day_of_week] INT NOT NULL,
    [start_time] TIME NOT NULL,
    [coach_id] INT NOT NULL,
    [duration] INT NOT NULL,
    [max_capacity] INT NOT NULL,
	[offers_trial_option] BIT DEFAULT 0,
    [max_trial_capacity] INT NULL,
    -- 資料表級別的條件約束
	CONSTRAINT [CK_DayOfWeek] CHECK ([day_of_week] BETWEEN 0 AND 6),
	CONSTRAINT [CK_DurationPositive] CHECK ([duration] > 0),
	CONSTRAINT [CK_MaxCapacityPositive] CHECK ([max_capacity] > 0),
);
GO

-- 創建 enrollment 表
CREATE TABLE [enrollment]
(
    [id] INT PRIMARY KEY IDENTITY (1, 1),
    [user_id] INT NOT NULL,
    [course_id] INT NOT NULL,
    [enrollment_time] DATETIME DEFAULT GETDATE(),
    [status] VARCHAR(50) NOT NULL,
	CONSTRAINT [CK_EnrollmentStatus] CHECK ([status] IN ('已報名', '已取消', '候補中', '已完成', '未到場'))
);
GO

-- 建立 trial_booking 資料表
CREATE TABLE [trial_booking]
(
    [id] INT PRIMARY KEY IDENTITY (1, 1),
    [user_id] INT,
    [course_id] INT NOT NULL,
    [booking_name] VARCHAR(255) NOT NULL,			-- 報名姓名
	[booking_email] VARCHAR(255) NOT NULL,
    [booking_phone] VARCHAR(20) NOT NULL,			-- 電話號碼
    [booking_date] DATE,							-- 預約日期
    [booking_status] VARCHAR(50) DEFAULT '已預約',	-- 預約狀態 (例如：已預約、已取消、已完成)
    [booked_at] DATETIME DEFAULT GETDATE(),			-- 預約建立時間
);
GO

-- 創建 product 表
CREATE TABLE [product]
(
    [id]             INT PRIMARY KEY IDENTITY (1, 1),
    [name]           NVARCHAR(255)  NOT NULL,
    [description]    NVARCHAR(1000),
    [price]          DECIMAL(10, 2) NOT NULL,
    [stock_quantity] INT            NOT NULL DEFAULT (0),
    [category]       NVARCHAR(100), -- 添加 category 欄位
    [image_url]      NVARCHAR(MAX), -- 修改為NVARCHAR(MAX)以支持更長的URL
    [created_at]     DATETIME                DEFAULT CURRENT_TIMESTAMP,
    [updated_at]     DATETIME                DEFAULT CURRENT_TIMESTAMP
);
GO


-- 創建 order 表
CREATE TABLE [order]
(
    [id]           INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]      INT            NOT NULL,
    [total_amount] DECIMAL(10, 2) NOT NULL,
    [status]       NVARCHAR(50) DEFAULT 'pending',
    [created_at]   DATETIME     DEFAULT CURRENT_TIMESTAMP
);
GO

-- 創建 order_item 表
CREATE TABLE [order_item]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [order_id]   INT            NOT NULL,
    [product_id] INT,
    [quantity]   INT            NOT NULL DEFAULT (1),
    [subtotal]   DECIMAL(10, 2) NOT NULL
);
GO

-- 創建 cart_item 表
CREATE TABLE [cart_item]
(
    [id]         INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]    INT NOT NULL,
    [product_id] INT,
    [quantity]   INT NOT NULL DEFAULT (1),
    [added_at]   DATETIME     DEFAULT CURRENT_TIMESTAMP
);
GO

-- 創建 payment 表
CREATE TABLE [payment]
(
    [id]            NVARCHAR(36) PRIMARY KEY, -- UUID格式
    [order_id]      INT            NOT NULL,
    [amount]        DECIMAL(10, 2) NOT NULL,
    [currency]      NVARCHAR(10) DEFAULT 'TWD',
    [method]        NVARCHAR(50)   NOT NULL,
    [status]        NVARCHAR(50) DEFAULT 'PENDING',
    [created_at]    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    [paid_at]       DATETIME       NULL,
    [error_message] NVARCHAR(500)  NULL
);
GO

-- 創建 exercise_type_coefficients 表
CREATE TABLE [exercise_type_coefficients]
(
    [exercise_type_id] INT PRIMARY KEY IDENTITY (1, 1),
    [exercise_name]    NVARCHAR(50) NOT NULL,
    [met]              FLOAT        NOT NULL
);
GO

-- 創建 body_metrics 表
CREATE TABLE [body_metrics]
(
    [id]                  INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]             INT   NOT NULL FOREIGN KEY REFERENCES [users] ([user_id]) ON DELETE CASCADE,
    [weight]              FLOAT NOT NULL,
    [body_fat]            FLOAT,
    [muscle_mass]         FLOAT,
    [waist_circumference] FLOAT,
    [hip_circumference]   FLOAT,
    [height]              FLOAT,
    [bmi]                 FLOAT,
    [date_recorded]       DATE DEFAULT GETDATE()
);
GO

-- 創建 exercise_records 表
CREATE TABLE [exercise_records]
(
    [record_id]         INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]           INT          NOT NULL FOREIGN KEY REFERENCES [users] ([user_id]) ON DELETE CASCADE,
    [exercise_type]     NVARCHAR(50) NOT NULL,
    [exercise_duration] INT          NOT NULL,
    [calories_burned]   FLOAT        NOT NULL,
    [exercise_date]     DATE         NOT NULL
);
GO

-- 創建 nutrition_records 表
CREATE TABLE [nutrition_records]
(
    [record_id]   INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]     INT FOREIGN KEY REFERENCES [users] ([user_id]) ON DELETE CASCADE,
    [food_name]   NVARCHAR(255) NOT NULL,
    [calories]    INT           NOT NULL,
    [protein]     FLOAT         NOT NULL,
    [carbs]       FLOAT         NOT NULL,
    [fats]        FLOAT         NOT NULL,
    [mealtime]    NVARCHAR(50) CHECK (mealtime IN ('早餐', '午餐', '晚餐', '點心')),
    [record_date] DATETIME2 DEFAULT GETDATE()
);
GO

-- 創建 fitness_goals 表
CREATE TABLE [fitness_goals]
(
    [goal_id]          INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]          INT          NOT NULL FOREIGN KEY REFERENCES [users] ([user_id]) ON DELETE CASCADE,
    [goal_type]        NVARCHAR(50) NOT NULL CHECK (goal_type IN ('減重', '增肌', '減脂')),
    [target_value]     FLOAT        NOT NULL,
    [current_progress] FLOAT                                                         DEFAULT 0,
    [unit]             NVARCHAR(20) CHECK (unit IN ('公斤', '%')),
    [start_date]       DATE                                                          DEFAULT GETDATE(),
    [end_date]         DATETIME2    NULL,
    [status]           NVARCHAR(20) CHECK (status IN ('進行中', '已完成', '未達成')) DEFAULT '進行中',
    [start_weight] FLOAT NULL,        -- 起始體重 (用於減重)
    [start_body_fat] FLOAT NULL,     -- 起始體脂率 (用於減脂)
    [start_muscle_mass] FLOAT NULL,  -- 起始肌肉量 (用於增肌)
    CONSTRAINT [CK_EndDate] CHECK ([end_date] IS NULL OR [end_date] >= [start_date])
);
GO

-- 創建 achievements 表
CREATE TABLE [achievements]
(
    [achievement_id]   INT PRIMARY KEY IDENTITY (1, 1),
    [user_id]          INT                                                NOT NULL FOREIGN KEY REFERENCES [users] ([user_id]) ON DELETE CASCADE,
    [achievement_type] NVARCHAR(50) ,
    [title]            NVARCHAR(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
    [description]      NVARCHAR(500),
    [achieved_date]    DATE DEFAULT GETDATE(),
    CONSTRAINT [UC_UniqueAchievement] UNIQUE ([user_id], [title])
);
GO
-- 創建 achievement_definitions 表
CREATE TABLE [achievement_definitions] (
    [definition_id] INT PRIMARY KEY IDENTITY(1, 1),
    [achievement_type] NVARCHAR(50) UNIQUE NOT NULL,
    [title] NVARCHAR(100) NOT NULL,
    [description] NVARCHAR(MAX) NULL,
    [trigger_event] VARCHAR(50) NULL,
    [trigger_condition] NVARCHAR(MAX) NULL,
    [image_url] NVARCHAR(255) NULL,
    [points] INT NULL,
    [created_at] DATETIME DEFAULT GETDATE(),
    [updated_at] DATETIME DEFAULT GETDATE()
    );
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
    [updated_at] DATETIME DEFAULT CURRENT_TIMESTAMP,
	[view_count] INT            NOT NULL DEFAULT 0
);
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
);
GO

-- 創建 post_like 表：文章按讚功能
CREATE TABLE [post_like] (
    [user_id]   INT NOT NULL,
    [post_id]   INT NOT NULL,
    [created_at] DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ([user_id], [post_id]),
    FOREIGN KEY ([user_id]) REFERENCES [users]([user_id]),
    FOREIGN KEY ([post_id]) REFERENCES [social_post]([id])
);
GO

-- 創建 user_friend 表：好友關係（雙向）
CREATE TABLE [user_friend] (
    [user_id]   INT NOT NULL,
    [friend_id] INT NOT NULL,
    [created_at] DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ([user_id], [friend_id]),
    FOREIGN KEY ([user_id]) REFERENCES [users]([user_id]),
    FOREIGN KEY ([friend_id]) REFERENCES [users]([user_id])
);
GO

-- 創建 training_invitation 表：訓練邀請功能
CREATE TABLE [training_invitation] (
    [id]         INT PRIMARY KEY IDENTITY(1,1),
    [sender_id]  INT NOT NULL,
    [receiver_id] INT NOT NULL,
    [message]    NVARCHAR(500),
    [status]     VARCHAR(20) DEFAULT 'pending', -- pending / accepted / rejected
    [sent_at]    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ([sender_id]) REFERENCES [users]([user_id]),
    FOREIGN KEY ([receiver_id]) REFERENCES [users]([user_id])
);
GO

-- 創建 user_activity 表：使用者動態紀錄
CREATE TABLE [user_activity] (
    [id]          INT PRIMARY KEY IDENTITY(1,1),
    [user_id]     INT NOT NULL,
    [action_type] VARCHAR(50) NOT NULL,  -- 例如：'post', 'like', 'comment'
    [reference_id] INT,                  -- 關聯的 post_id、comment_id 等
    [created_at]  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ([user_id]) REFERENCES [users]([user_id])
);
GO

-- 圖片與媒體管理資料表
CREATE TABLE [media_file] (
    [id]           INT PRIMARY KEY IDENTITY(1,1),
    [url]          NVARCHAR(1000) NOT NULL,     -- 圖片網址或相對路徑
    [type]         VARCHAR(20) NOT NULL,        -- 'avatar', 'post'
    [ref_id]       INT NOT NULL,                -- user_id or post_id
    [uploaded_at]  DATETIME DEFAULT CURRENT_TIMESTAMP
);
GO

--文章收藏管理資料表
CREATE TABLE [post_favorite] (
    [id]           INT PRIMARY KEY IDENTITY(1,1),     -- 收藏編號
    [user_id]      INT NOT NULL,                     -- 收藏者
    [post_id]      INT NOT NULL,                     -- 被收藏的文章
    [created_at]   DATETIME DEFAULT CURRENT_TIMESTAMP, -- 收藏時間
    CONSTRAINT FK_favorite_user FOREIGN KEY ([user_id]) REFERENCES [users]([user_id]),
    CONSTRAINT FK_favorite_post FOREIGN KEY ([post_id]) REFERENCES [social_post]([id]),
    CONSTRAINT UQ_user_post UNIQUE ([user_id], [post_id]) -- 同一文章不可重複收藏
);
GO
CREATE TABLE [friend_invitation] (
    [id]           INT PRIMARY KEY IDENTITY(1,1),         -- 邀請編號
    [inviter_id]   INT NOT NULL,                          -- 發出邀請的使用者
    [invitee_id]   INT NOT NULL,                          -- 接收邀請的使用者
    [status]       NVARCHAR(20) NOT NULL DEFAULT 'pending', -- 邀請狀態：pending、accepted、rejected
    [created_at]   DATETIME DEFAULT CURRENT_TIMESTAMP,    -- 邀請時間
    CONSTRAINT FK_invitation_inviter FOREIGN KEY ([inviter_id]) REFERENCES [users]([user_id]),
    CONSTRAINT FK_invitation_invitee FOREIGN KEY ([invitee_id]) REFERENCES [users]([user_id]),
    CONSTRAINT UQ_invitation_pair UNIQUE ([inviter_id], [invitee_id]) -- 不可重複邀請
);
GO

-- 外鍵約束設定
ALTER TABLE [user_point]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [order]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [order_item]
    ADD FOREIGN KEY ([order_id]) REFERENCES [order] ([id]);
ALTER TABLE [order_item]
    ADD FOREIGN KEY ([product_id]) REFERENCES [product] ([id]);
ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([product_id]) REFERENCES [product] ([id]);
ALTER TABLE [social_post]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [comment]
    ADD FOREIGN KEY ([post_id]) REFERENCES [social_post] ([id]);
ALTER TABLE [comment]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [payment]
    ADD FOREIGN KEY ([order_id]) REFERENCES [order] ([id]);
ALTER TABLE [course]
    ADD FOREIGN KEY ([coach_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [enrollment]
    ADD FOREIGN KEY ([user_id]) REFERENCES [users] ([user_id]);
ALTER TABLE [enrollment]
    ADD FOREIGN KEY ([course_id]) REFERENCES [course] ([id]);
ALTER TABLE [trial_booking]
	ADD FOREIGN KEY ([user_id]) REFERENCES [users]([user_id]);
ALTER TABLE [trial_booking]
	ADD FOREIGN KEY ([course_id]) REFERENCES [course]([id]);
GO


CREATE OR ALTER VIEW dashboard_stat AS
SELECT (SELECT COUNT(*) FROM [users] WHERE role = 'user')                             AS total_users,
       (SELECT COUNT(*) FROM [exercise_records])                                      AS total_workouts,
       (SELECT SUM([exercise_duration]) FROM [exercise_records])                      AS total_workout_minutes,
       (SELECT SUM([calories_burned]) FROM [exercise_records])                        AS total_calories_burned,
       (SELECT COUNT(*) FROM [users] WHERE DATEDIFF(DAY, last_login, GETDATE()) <= 7) AS active_users_this_week,
       (SELECT COUNT(DISTINCT [user_id])
        FROM [users]
        WHERE YEAR(last_login) = YEAR(GETDATE())
        AND MONTH(last_login) = MONTH(GETDATE()))                                 AS active_users_this_month,
       (SELECT COUNT(DISTINCT [user_id])
        FROM [users]
        WHERE YEAR(last_login) = YEAR(GETDATE()))                                    AS active_users_this_year

FROM (SELECT 1 AS dummy) AS dummy; -- 需要一個假的 FROM 子句來允許子查詢
GO
