use health_db
-- 創建 user 表
CREATE TABLE [user]
(
    [id]            INT PRIMARY KEY IDENTITY (1, 1),
    [name]          NVARCHAR(50)         NOT NULL,
    [email]         NVARCHAR(100) UNIQUE NOT NULL,
    [password_hash] VARCHAR(255)         NOT NULL,
    [gender]        CHAR(1),
    [bio]           NVARCHAR(MAX),
    [role]          NVARCHAR(10)         NOT NULL DEFAULT N'user' CHECK ([role] IN (N'user', N'admin')),
    [last_login]    DATETIME             NULL
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
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
GO

ALTER TABLE [order]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
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
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
GO

ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([product_id]) REFERENCES [product] ([id])
GO

ALTER TABLE [cart_item]
    ADD FOREIGN KEY ([course_id]) REFERENCES [course] ([id])
GO

ALTER TABLE [exercise_record]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
GO

ALTER TABLE [social_post]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
GO

ALTER TABLE [comment]
    ADD FOREIGN KEY ([post_id]) REFERENCES [social_post] ([id])
GO

ALTER TABLE [comment]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id])
GO

ALTER TABLE [body_metric]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
GO

ALTER TABLE [nutrition_record]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
GO

ALTER TABLE [fitness_goal]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
GO

ALTER TABLE [achievement]
    ADD FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
GO

-- 創建視圖
CREATE VIEW dashboard_stat AS
SELECT (SELECT COUNT(*) FROM [user] WHERE role = 'user')                             AS total_users,
       (SELECT COUNT(*) FROM exercise_record)                                        AS total_workouts,
       (SELECT SUM(duration) FROM exercise_record)                                   AS total_workout_minutes,
       (SELECT SUM(calories_burned) FROM exercise_record)                            AS total_calories_burned,
       (SELECT COUNT(*) FROM [user] WHERE DATEDIFF(DAY, last_login, GETDATE()) <= 7) AS active_users_this_week;
GO

-- 插入測試數據
-- 插入 user 數據 (20筆)
INSERT INTO [user] ([name], [email], [password_hash], [gender], [bio], [role], [last_login])
--     //TODO: 組長請救場
-- VALUES ('張三', 'zhangsan@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '熱愛健身的上班族', 'user',
--         '2023-01-15 09:30:00'),
--        ('李四', 'lisi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身教練', 'admin',
--         '2023-01-16 10:15:00'),
--        ('王五', 'wangwu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '瑜伽愛好者', 'user',
--         '2023-01-17 14:20:00'),
--        ('趙六', 'zhaoliu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '營養師', 'user',
--         '2023-01-18 16:45:00'),
--        ('錢七', 'qianqi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '馬拉松跑者', 'user',
--         '2023-01-19 08:10:00'),
--        ('孫八', 'sunba@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '健身新手', 'user',
--         '2023-01-20 11:30:00'),
--        ('周九', 'zhoujiu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '重量訓練愛好者', 'user',
--         '2023-01-21 13:15:00'),
--        ('吳十', 'wushi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '舞蹈老師', 'user',
--         '2023-01-22 15:40:00'),
--        ('鄭十一', 'zhengshiyi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '游泳健將', 'user',
--         '2023-01-23 17:25:00'),
--        ('王十二', 'wangshier@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '健身網紅', 'user',
--         '2023-01-24 19:00:00'),
--        ('林十三', 'linshisan@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身器材專家', 'user',
--         '2023-01-25 20:35:00'),
--        ('陳十四', 'chenshisi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '營養學博士', 'user',
--         '2023-01-26 09:50:00'),
--        ('黃十五', 'huangshiwu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身教練', 'admin',
--         '2023-01-27 11:15:00'),
--        ('劉十六', 'liushiliu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '瑜伽導師', 'user',
--         '2023-01-28 14:30:00'),
--        ('高十七', 'gaoshiqi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身器材店老闆', 'user',
--         '2023-01-29 16:45:00'),
--        ('謝十八', 'xieshiba@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '健身營養顧問', 'user',
--         '2023-01-30 18:10:00'),
--        ('羅十九', 'luoshijiu@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身部落客', 'user',
--         '2023-01-31 20:35:00'),
--        ('蔡二十', 'caiershi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '健身模特兒', 'user',
--         '2023-02-01 09:00:00'),
--        ('葉廿一', 'yenianyi@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'M', '健身器材研發', 'user',
--         '2023-02-02 11:25:00'),
--        ('許廿二', 'xunianer@example.com', '5f4dcc3b5aa765d61d8327deb882cf99', 'F', '健身營養品銷售', 'user',
--         '2023-02-03 13:50:00')
GO

-- 插入 user_point 數據 (20筆)
INSERT INTO [user_point] ([user_id], [points], [last_updated])
VALUES (1, 100, '2023-01-15 09:30:00'),
       (2, 500, '2023-01-16 10:15:00'),
       (3, 200, '2023-01-17 14:20:00'),
       (4, 300, '2023-01-18 16:45:00'),
       (5, 150, '2023-01-19 08:10:00'),
       (6, 50, '2023-01-20 11:30:00'),
       (7, 250, '2023-01-21 13:15:00'),
       (8, 350, '2023-01-22 15:40:00'),
       (9, 400, '2023-01-23 17:25:00'),
       (10, 450, '2023-01-24 19:00:00'),
       (11, 120, '2023-01-25 20:35:00'),
       (12, 220, '2023-01-26 09:50:00'),
       (13, 320, '2023-01-27 11:15:00'),
       (14, 420, '2023-01-28 14:30:00'),
       (15, 520, '2023-01-29 16:45:00'),
       (16, 620, '2023-01-30 18:10:00'),
       (17, 720, '2023-01-31 20:35:00'),
       (18, 820, '2023-02-01 09:00:00'),
       (19, 920, '2023-02-02 11:25:00'),
       (20, 1020, '2023-02-03 13:50:00')
GO

-- 插入 coach 數據 (20筆)
INSERT INTO [coach] ([name], [expertise])
VALUES ('李教練', '重量訓練'),
       ('王教練', '有氧運動'),
       ('張教練', '瑜伽'),
       ('陳教練', '拳擊'),
       ('林教練', '游泳'),
       ('黃教練', '舞蹈'),
       ('吳教練', '功能性訓練'),
       ('劉教練', 'HIIT'),
       ('蔡教練', '普拉提'),
       ('鄭教練', '體能訓練'),
       ('周教練', 'TRX'),
       ('高教練', 'CrossFit'),
       ('謝教練', '拳擊有氧'),
       ('羅教練', '太極'),
       ('葉教練', '街舞'),
       ('許教練', '芭蕾'),
       ('徐教練', '爵士舞'),
       ('蘇教練', '現代舞'),
       ('彭教練', '民族舞'),
       ('曾教練', '肚皮舞')
GO

-- 插入 product 數據 (20筆)
INSERT INTO [product] ([name], [description], [price], [stock_quantity], [image_url])
VALUES ('蛋白粉', '乳清蛋白粉，巧克力口味', 1200.00, 100, 'https://example.com/protein.jpg'),
       ('健身手套', '防滑透氣健身手套', 450.00, 50, 'https://example.com/gloves.jpg'),
       ('瑜伽墊', '加厚防滑瑜伽墊', 800.00, 30, 'https://example.com/mat.jpg'),
       ('啞鈴組', '可調節重量啞鈴組', 2500.00, 20, 'https://example.com/dumbbell.jpg'),
       ('彈力帶', '5種阻力彈力帶組', 600.00, 40, 'https://example.com/band.jpg'),
       ('跳繩', '競速跳繩', 300.00, 60, 'https://example.com/rope.jpg'),
       ('健身腰帶', '舉重腰帶', 900.00, 25, 'https://example.com/belt.jpg'),
       ('運動水壺', '750ml運動水壺', 350.00, 80, 'https://example.com/bottle.jpg'),
       ('健身服套裝', '透氣排汗健身服', 1200.00, 35, 'https://example.com/clothes.jpg'),
       ('運動襪', '防滑運動襪3雙組', 250.00, 90, 'https://example.com/socks.jpg'),
       ('健身背包', '多功能健身背包', 1500.00, 15, 'https://example.com/bag.jpg'),
       ('按摩滾筒', '高密度泡沫滾筒', 700.00, 30, 'https://example.com/roller.jpg'),
       ('運動毛巾', '速乾運動毛巾', 200.00, 70, 'https://example.com/towel.jpg'),
       ('健身日誌', '健身記錄本', 180.00, 50, 'https://example.com/journal.jpg'),
       ('心率錶', '藍牙心率監測錶', 2800.00, 10, 'https://example.com/watch.jpg'),
       ('健身APP會員', '一年期健身APP會員', 1200.00, 999, 'https://example.com/app.jpg'),
       ('運動耳機', '無線運動耳機', 1600.00, 20, 'https://example.com/earphone.jpg'),
       ('健身課程DVD', '全套健身課程DVD', 950.00, 15, 'https://example.com/dvd.jpg'),
       ('健身書籍', '健身營養全書', 550.00, 25, 'https://example.com/book.jpg'),
       ('運動護腕', '支撐型運動護腕', 380.00, 40, 'https://example.com/wristband.jpg')
GO

-- 插入 course 數據 (20筆)
INSERT INTO [course] ([name], [description], [date], [coach_id], [duration], [max_capacity])
VALUES ('初級瑜伽', '適合初學者的瑜伽課程', '2023-03-01', 3, 60, 15),
       ('重量訓練入門', '學習基本重量訓練技巧', '2023-03-02', 1, 90, 10),
       ('有氧舞蹈', '燃脂有氧舞蹈課程', '2023-03-03', 6, 60, 20),
       ('高強度間歇訓練', 'HIIT高強度間歇訓練', '2023-03-04', 8, 45, 12),
       ('普拉提核心訓練', '強化核心肌群的普拉提', '2023-03-05', 9, 60, 15),
       ('拳擊有氧', '結合拳擊的有氧運動', '2023-03-06', 4, 60, 12),
       ('TRX全身訓練', '懸吊訓練系統課程', '2023-03-07', 11, 60, 10),
       ('功能性訓練', '提升日常功能性的訓練', '2023-03-08', 7, 60, 12),
       ('游泳技巧', '提升游泳技巧的課程', '2023-03-09', 5, 90, 8),
       ('舞蹈基礎', '各種舞蹈基礎訓練', '2023-03-10', 6, 60, 15),
       ('進階瑜伽', '適合有經驗者的瑜伽', '2023-03-11', 3, 75, 12),
       ('重量訓練進階', '進階重量訓練技巧', '2023-03-12', 1, 90, 8),
       ('有氧搏擊', '燃脂有氧搏擊課程', '2023-03-13', 13, 60, 15),
       ('CrossFit基礎', 'CrossFit基礎訓練', '2023-03-14', 12, 90, 10),
       ('太極養生', '太極養生課程', '2023-03-15', 14, 60, 15),
       ('街舞入門', '街舞基礎課程', '2023-03-16', 15, 60, 15),
       ('芭蕾塑形', '芭蕾舞塑形課程', '2023-03-17', 16, 60, 12),
       ('爵士舞', '爵士舞課程', '2023-03-18', 17, 60, 15),
       ('現代舞', '現代舞課程', '2023-03-19', 18, 60, 12),
       ('肚皮舞', '肚皮舞課程', '2023-03-20', 20, 60, 15)
GO

-- 插入 order 數據 (20筆)
INSERT INTO [order] ([user_id], [total_amount], [status], [created_at])
VALUES (1, 1200.00, 'completed', '2023-02-01 10:00:00'),
       (2, 1650.00, 'completed', '2023-02-02 11:15:00'),
       (3, 800.00, 'completed', '2023-02-03 14:30:00'),
       (4, 2500.00, 'pending', '2023-02-04 16:45:00'),
       (5, 600.00, 'completed', '2023-02-05 08:10:00'),
       (6, 300.00, 'completed', '2023-02-06 11:30:00'),
       (7, 900.00, 'completed', '2023-02-07 13:15:00'),
       (8, 350.00, 'completed', '2023-02-08 15:40:00'),
       (9, 1200.00, 'completed', '2023-02-09 17:25:00'),
       (10, 250.00, 'completed', '2023-02-10 19:00:00'),
       (11, 1500.00, 'pending', '2023-02-11 20:35:00'),
       (12, 700.00, 'completed', '2023-02-12 09:50:00'),
       (13, 200.00, 'completed', '2023-02-13 11:15:00'),
       (14, 180.00, 'completed', '2023-02-14 14:30:00'),
       (15, 2800.00, 'completed', '2023-02-15 16:45:00'),
       (16, 1200.00, 'completed', '2023-02-16 18:10:00'),
       (17, 1600.00, 'completed', '2023-02-17 20:35:00'),
       (18, 950.00, 'pending', '2023-02-18 09:00:00'),
       (19, 550.00, 'completed', '2023-02-19 11:25:00'),
       (20, 380.00, 'completed', '2023-02-20 13:50:00')
GO

-- 插入 order_item 數據 (20筆)
INSERT INTO [order_item] ([order_id], [product_id], [course_id], [quantity], [subtotal])
VALUES (1, 1, NULL, 1, 1200.00),
       (2, 2, NULL, 1, 450.00),
       (2, 9, NULL, 1, 1200.00),
       (3, 3, NULL, 1, 800.00),
       (4, 4, NULL, 1, 2500.00),
       (5, 5, NULL, 1, 600.00),
       (6, 6, NULL, 1, 300.00),
       (7, 7, NULL, 1, 900.00),
       (8, 8, NULL, 1, 350.00),
       (9, 9, NULL, 1, 1200.00),
       (10, 10, NULL, 1, 250.00),
       (11, 11, NULL, 1, 1500.00),
       (12, 12, NULL, 1, 700.00),
       (13, 13, NULL, 1, 200.00),
       (14, 14, NULL, 1, 180.00),
       (15, 15, NULL, 1, 2800.00),
       (16, 16, NULL, 1, 1200.00),
       (17, 17, NULL, 1, 1600.00),
       (18, 18, NULL, 1, 950.00),
       (19, 19, NULL, 1, 550.00),
       (20, 20, NULL, 1, 380.00)
GO

-- 插入 cart_item 數據 (20筆)
INSERT INTO [cart_item] ([user_id], [product_id], [course_id], [quantity], [added_at])
VALUES (1, 1, NULL, 1, '2023-02-21 10:00:00'),
       (2, NULL, 1, 1, '2023-02-21 11:15:00'),
       (3, 2, NULL, 1, '2023-02-21 14:30:00'),
       (4, NULL, 2, 1, '2023-02-21 16:45:00'),
       (5, 3, NULL, 1, '2023-02-22 08:10:00'),
       (6, NULL, 3, 1, '2023-02-22 11:30:00'),
       (7, 4, NULL, 1, '2023-02-22 13:15:00'),
       (8, NULL, 4, 1, '2023-02-22 15:40:00'),
       (9, 5, NULL, 1, '2023-02-23 17:25:00'),
       (10, NULL, 5, 1, '2023-02-23 19:00:00'),
       (11, 6, NULL, 1, '2023-02-23 20:35:00'),
       (12, NULL, 6, 1, '2023-02-24 09:50:00'),
       (13, 7, NULL, 1, '2023-02-24 11:15:00'),
       (14, NULL, 7, 1, '2023-02-24 14:30:00'),
       (15, 8, NULL, 1, '2023-02-25 16:45:00'),
       (16, NULL, 8, 1, '2023-02-25 18:10:00'),
       (17, 9, NULL, 1, '2023-02-25 20:35:00'),
       (18, NULL, 9, 1, '2023-02-26 09:00:00'),
       (19, 10, NULL, 1, '2023-02-26 11:25:00'),
       (20, NULL, 10, 1, '2023-02-26 13:50:00')
GO

-- 插入 body_metric 數據 (20筆)
INSERT INTO [body_metric] ([user_id], [weight], [body_fat], [muscle_mass], [waist_circumference], [hip_circumference],
                           [height], [date_recorded])
VALUES (1, 70.5, 15.0, 30.5, 80.0, 95.0, 175.0, '2023-02-01 10:00:00'),
       (2, 65.2, 18.2, 29.3, 78.0, 92.5, 168.0, '2023-02-02 11:15:00'),
       (3, 80.3, 22.5, 32.0, 85.0, 100.0, 180.0, '2023-02-03 14:30:00'),
       (4, 55.0, 12.5, 24.5, 70.0, 88.0, 160.0, '2023-02-04 16:45:00'),
       (5, 90.0, 25.0, 40.0, 90.0, 105.0, 185.0, '2023-02-05 08:10:00'),
       (6, 68.0, 16.0, 28.0, 77.0, 93.0, 172.0, '2023-02-06 11:30:00'),
       (7, 72.3, 19.0, 31.0, 82.0, 96.0, 178.0, '2023-02-07 13:15:00'),
       (8, 58.5, 14.0, 26.5, 75.0, 90.0, 165.0, '2023-02-08 15:40:00'),
       (9, 77.0, 21.5, 35.0, 84.0, 98.0, 182.0, '2023-02-09 17:25:00'),
       (10, 64.5, 17.5, 27.0, 76.5, 91.5, 170.0, '2023-02-10 19:00:00'),
       (11, 71.0, 15.5, 30.0, 81.0, 96.0, 176.0, '2023-02-11 20:35:00'),
       (12, 66.0, 17.0, 28.5, 79.0, 94.0, 173.0, '2023-02-12 09:50:00'),
       (13, 83.0, 23.0, 33.0, 86.0, 101.0, 181.0, '2023-02-13 11:15:00'),
       (14, 56.0, 13.0, 25.0, 71.0, 89.0, 161.0, '2023-02-14 14:30:00'),
       (15, 89.0, 24.5, 39.0, 89.0, 104.0, 184.0, '2023-02-15 16:45:00'),
       (16, 67.0, 15.5, 27.5, 78.0, 94.0, 173.0, '2023-02-16 18:10:00'),
       (17, 73.0, 19.5, 31.5, 83.0, 97.0, 179.0, '2023-02-17 20:35:00'),
       (18, 59.0, 14.5, 27.0, 76.0, 91.0, 166.0, '2023-02-18 09:00:00'),
       (19, 78.0, 22.0, 36.0, 85.0, 99.0, 183.0, '2023-02-19 11:25:00'),
       (20, 65.0, 18.0, 28.0, 77.0, 92.0, 171.0, '2023-02-20 13:50:00')
GO

-- 插入 exercise_record 數據 (20筆)
INSERT INTO [exercise_record] ([user_id], [exercise_type], [duration], [calories_burned], [date])
VALUES (1, 'weight_training', 60, 300.00, '2023-02-01'),
       (2, 'yoga', 45, 200.00, '2023-02-02'),
       (3, 'running', 30, 250.00, '2023-02-03'),
       (4, 'swimming', 60, 400.00, '2023-02-04'),
       (5, 'cycling', 45, 350.00, '2023-02-05'),
       (6, 'hiit', 30, 300.00, '2023-02-06'),
       (7, 'weight_training', 60, 320.00, '2023-02-07'),
       (8, 'yoga', 45, 210.00, '2023-02-08'),
       (9, 'running', 30, 260.00, '2023-02-09'),
       (10, 'swimming', 60, 410.00, '2023-02-10'),
       (11, 'cycling', 45, 360.00, '2023-02-11'),
       (12, 'hiit', 30, 310.00, '2023-02-12'),
       (13, 'weight_training', 60, 330.00, '2023-02-13'),
       (14, 'yoga', 45, 220.00, '2023-02-14'),
       (15, 'running', 30, 270.00, '2023-02-15'),
       (16, 'swimming', 60, 420.00, '2023-02-16'),
       (17, 'cycling', 45, 370.00, '2023-02-17'),
       (18, 'hiit', 30, 320.00, '2023-02-18'),
       (19, 'weight_training', 60, 340.00, '2023-02-19'),
       (20, 'yoga', 45, 230.00, '2023-02-20')
GO

-- 插入 nutrition_record 數據 (20筆)
INSERT INTO [nutrition_record] ([user_id], [food_name], [calories], [protein], [carbs], [fats], [mealtime],
                                [date_recorded])
VALUES (1, '雞胸肉', 200, 40.0, 0.0, 5.0, 'lunch', '2023-02-01 12:00:00'),
       (2, '糙米飯', 150, 3.0, 30.0, 1.0, 'dinner', '2023-02-02 18:30:00'),
       (3, '水煮蛋', 70, 6.0, 0.6, 5.0, 'breakfast', '2023-02-03 08:00:00'),
       (4, '蔬菜沙拉', 100, 2.0, 10.0, 5.0, 'lunch', '2023-02-04 12:30:00'),
       (5, '鮭魚', 250, 25.0, 0.0, 15.0, 'dinner', '2023-02-05 19:00:00'),
       (6, '全麥吐司', 120, 4.0, 20.0, 2.0, 'breakfast', '2023-02-06 08:30:00'),
       (7, '牛肉', 300, 35.0, 0.0, 18.0, 'lunch', '2023-02-07 13:00:00'),
       (8, '地瓜', 130, 2.0, 30.0, 0.2, 'dinner', '2023-02-08 18:00:00'),
       (9, '希臘優格', 150, 15.0, 8.0, 5.0, 'breakfast', '2023-02-09 09:00:00'),
       (10, '雞肉沙拉三明治', 350, 25.0, 30.0, 12.0, 'lunch', '2023-02-10 12:15:00'),
       (11, '義大利麵', 400, 15.0, 60.0, 10.0, 'dinner', '2023-02-11 19:30:00'),
       (12, '燕麥粥', 180, 6.0, 30.0, 3.0, 'breakfast', '2023-02-12 08:45:00'),
       (13, '牛排', 450, 40.0, 0.0, 30.0, 'lunch', '2023-02-13 13:30:00'),
       (14, '烤蔬菜', 120, 3.0, 15.0, 5.0, 'dinner', '2023-02-14 18:15:00'),
       (15, '水果沙拉', 150, 1.0, 35.0, 0.5, 'breakfast', '2023-02-15 09:30:00'),
       (16, '雞肉炒飯', 400, 25.0, 45.0, 12.0, 'lunch', '2023-02-16 12:45:00'),
       (17, '烤鮭魚', 300, 30.0, 0.0, 18.0, 'dinner', '2023-02-17 19:45:00'),
       (18, '蛋白煎餅', 200, 20.0, 15.0, 5.0, 'breakfast', '2023-02-18 09:15:00'),
       (19, '牛肉漢堡', 500, 30.0, 40.0, 25.0, 'lunch', '2023-02-19 13:15:00'),
       (20, '蔬菜湯', 150, 5.0, 20.0, 5.0, 'dinner', '2023-02-20 18:30:00')
GO

-- 插入 fitness_goal 數據 (20筆)
INSERT INTO [fitness_goal] ([user_id], [type], [target_value], [current_progress], [unit], [start_date], [end_date],
                            [status], [frequency_goal], [frequency_duration], [reference_table])
VALUES (1, 'weight_loss', 5.0, 2.5, 'kg', '2023-01-01', '2023-04-01', 'in_progress', 3, 12, 'body_metric'),
       (2, 'muscle_gain', 3.0, 1.5, 'kg', '2023-01-15', '2023-04-15', 'in_progress', 4, 12, 'body_metric'),
       (3, 'cardio', 30.0, 15.0, 'min', '2023-02-01', '2023-05-01', 'in_progress', 5, 12, 'exercise_record'),
       (4, 'weight_loss', 4.0, 1.0, 'kg', '2023-01-20', '2023-04-20', 'in_progress', 3, 12, 'body_metric'),
       (5, 'muscle_gain', 5.0, 2.0, 'kg', '2023-02-01', '2023-05-01', 'in_progress', 4, 12, 'body_metric'),
       (6, 'cardio', 20.0, 10.0, 'min', '2023-02-05', '2023-05-05', 'in_progress', 3, 12, 'exercise_record'),
       (7, 'weight_loss', 3.0, 1.5, 'kg', '2023-01-10', '2023-04-10', 'in_progress', 4, 12, 'body_metric'),
       (8, 'muscle_gain', 2.0, 0.5, 'kg', '2023-01-25', '2023-04-25', 'in_progress', 3, 12, 'body_metric'),
       (9, 'cardio', 25.0, 12.5, 'min', '2023-02-10', '2023-05-10', 'in_progress', 4, 12, 'exercise_record'),
       (10, 'weight_loss', 6.0, 3.0, 'kg', '2023-01-05', '2023-04-05', 'in_progress', 5, 12, 'body_metric'),
       (11, 'muscle_gain', 4.0, 2.0, 'kg', '2023-02-15', '2023-05-15', 'in_progress', 3, 12, 'body_metric'),
       (12, 'cardio', 15.0, 7.5, 'min', '2023-02-20', '2023-05-20', 'in_progress', 4, 12, 'exercise_record'),
       (13, 'weight_loss', 5.0, 2.0, 'kg', '2023-01-15', '2023-04-15', 'in_progress', 3, 12, 'body_metric'),
       (14, 'muscle_gain', 3.0, 1.0, 'kg', '2023-02-01', '2023-05-01', 'in_progress', 4, 12, 'body_metric'),
       (15, 'cardio', 30.0, 15.0, 'min', '2023-02-05', '2023-05-05', 'in_progress', 5, 12, 'exercise_record'),
       (16, 'weight_loss', 4.0, 2.0, 'kg', '2023-01-20', '2023-04-20', 'in_progress', 3, 12, 'body_metric'),
       (17, 'muscle_gain', 5.0, 2.5, 'kg', '2023-02-10', '2023-05-10', 'in_progress', 4, 12, 'body_metric'),
       (18, 'cardio', 20.0, 10.0, 'min', '2023-02-15', '2023-05-15', 'in_progress', 3, 12, 'exercise_record'),
       (19, 'weight_loss', 3.0, 1.5, 'kg', '2023-01-25', '2023-04-25', 'in_progress', 4, 12, 'body_metric'),
       (20, 'muscle_gain', 4.0, 2.0, 'kg', '2023-02-20', '2023-05-20', 'in_progress', 3, 12, 'body_metric')
GO

-- 插入 achievement 數據 (20筆)
INSERT INTO [achievement] ([user_id], [type], [title], [description], [date_achieved])
VALUES (1, 'goal_completed', '減重5公斤達成', '成功達成減重目標', '2023-01-15 10:00:00'),
       (2, 'general_reward', '連續打卡7天', '連續7天完成運動打卡', '2023-01-16 11:15:00'),
       (3, 'goal_completed', '跑步30分鐘達成', '成功達成跑步目標', '2023-01-17 14:30:00'),
       (4, 'general_reward', '購買第一堂課程', '首次購買健身課程', '2023-01-18 16:45:00'),
       (5, 'goal_completed', '增肌3公斤達成', '成功達成增肌目標', '2023-01-19 08:10:00'),
       (6, 'general_reward', '連續打卡14天', '連續14天完成運動打卡', '2023-01-20 11:30:00'),
       (7, 'goal_completed', '體脂降到15%', '成功達成體脂目標', '2023-01-21 13:15:00'),
       (8, 'general_reward', '完成10次訓練', '完成10次健身訓練', '2023-01-22 15:40:00'),
       (9, 'goal_completed', '游泳1公里達成', '成功達成游泳目標', '2023-01-23 17:25:00'),
       (10, 'general_reward', '購買第一件商品', '首次購買健身商品', '2023-01-24 19:00:00'),
       (11, 'goal_completed', '深蹲100公斤', '成功達成深蹲目標', '2023-01-25 20:35:00'),
       (12, 'general_reward', '連續打卡21天', '連續21天完成運動打卡', '2023-01-26 09:50:00'),
       (13, 'goal_completed', '臥推80公斤', '成功達成臥推目標', '2023-01-27 11:15:00'),
       (14, 'general_reward', '完成20次訓練', '完成20次健身訓練', '2023-01-28 14:30:00'),
       (15, 'goal_completed', '硬舉120公斤', '成功達成硬舉目標', '2023-01-29 16:45:00'),
       (16, 'general_reward', '連續打卡30天', '連續30天完成運動打卡', '2023-01-30 18:10:00'),
       (17, 'goal_completed', '體脂降到12%', '成功達成體脂目標', '2023-01-31 20:35:00'),
       (18, 'general_reward', '完成30次訓練', '完成30次健身訓練', '2023-02-01 09:00:00'),
       (19, 'goal_completed', '增肌5公斤達成', '成功達成增肌目標', '2023-02-02 11:25:00'),
       (20, 'general_reward', '購買5件商品', '累計購買5件健身商品', '2023-02-03 13:50:00')
GO

-- 插入 exercise_type_coefficient 數據 (20筆)
INSERT INTO [exercise_type_coefficient] ([name], [met])
VALUES ('weight_training', 6.0),
       ('yoga', 2.5),
       ('running', 7.0),
       ('swimming', 8.0),
       ('cycling', 6.5),
       ('hiit', 9.0),
       ('walking', 3.0),
       ('dancing', 5.0),
       ('boxing', 8.5),
       ('pilates', 3.5),
       ('crossfit', 9.5),
       ('jumping_rope', 8.0),
       ('rowing', 7.5),
       ('stair_climbing', 6.0),
       ('elliptical', 5.5),
       ('kickboxing', 8.0),
       ('zumba', 5.0),
       ('tai_chi', 2.0),
       ('circuit_training', 7.0),
       ('functional_training', 6.5)
GO

-- 插入 social_post 數據 (20筆)
INSERT INTO [social_post] ([category], [title], [content], [user_id], [created_at], [updated_at])
VALUES ('fitness', '我的健身旅程', '分享我從開始健身到現在的心得...', 1, '2023-02-01 10:00:00', '2023-02-01 10:00:00'),
       ('nutrition', '健康飲食建議', '分享一些我認為很有效的健康飲食方法...', 2, '2023-02-02 11:15:00',
        '2023-02-02 11:15:00'),
       ('recipe', '高蛋白早餐食譜', '這是我每天早上都會吃的高蛋白早餐...', 3, '2023-02-03 14:30:00',
        '2023-02-03 14:30:00'),
       ('fitness', '重量訓練技巧', '分享一些重量訓練的基本技巧...', 4, '2023-02-04 16:45:00', '2023-02-04 16:45:00'),
       ('motivation', '堅持運動的秘訣', '如何保持運動習慣不間斷...', 5, '2023-02-05 08:10:00', '2023-02-05 08:10:00'),
       ('nutrition', '增肌飲食計劃', '我的增肌飲食計劃分享...', 6, '2023-02-06 11:30:00', '2023-02-06 11:30:00'),
       ('fitness', '在家健身指南', '沒有時間去健身房？在家也能有效健身...', 7, '2023-02-07 13:15:00',
        '2023-02-07 13:15:00'),
       ('recipe', '減脂餐食譜', '分享我的減脂餐食譜...', 8, '2023-02-08 15:40:00', '2023-02-08 15:40:00'),
       ('fitness', '跑步訓練計劃', '我的半馬訓練計劃分享...', 9, '2023-02-09 17:25:00', '2023-02-09 17:25:00'),
       ('motivation', '克服平台期', '如何克服健身平台期...', 10, '2023-02-10 19:00:00', '2023-02-10 19:00:00'),
       ('nutrition', '運動後補充', '運動後該如何正確補充營養...', 11, '2023-02-11 20:35:00', '2023-02-11 20:35:00'),
       ('fitness', '核心訓練指南', '強化核心肌群的訓練方法...', 12, '2023-02-12 09:50:00', '2023-02-12 09:50:00'),
       ('recipe', '健康點心食譜', '分享幾款健康的點心食譜...', 13, '2023-02-13 11:15:00', '2023-02-13 11:15:00'),
       ('fitness', '彈力帶訓練', '如何用彈力帶進行全身訓練...', 14, '2023-02-14 14:30:00', '2023-02-14 14:30:00'),
       ('motivation', '設定健身目標', '如何設定合理的健身目標...', 15, '2023-02-15 16:45:00', '2023-02-15 16:45:00'),
       ('nutrition', '蛋白質攝取指南', '每天該攝取多少蛋白質...', 16, '2023-02-16 18:10:00', '2023-02-16 18:10:00'),
       ('fitness', '啞鈴訓練計劃', '我的啞鈴全身訓練計劃...', 17, '2023-02-17 20:35:00', '2023-02-17 20:35:00'),
       ('recipe', '增肌餐食譜', '分享我的增肌餐食譜...', 18, '2023-02-18 09:00:00', '2023-02-18 09:00:00'),
       ('fitness', 'HIIT訓練指南', '高強度間歇訓練的注意事項...', 19, '2023-02-19 11:25:00', '2023-02-19 11:25:00'),
       ('motivation', '保持運動熱情', '如何長期保持運動熱情...', 20, '2023-02-20 13:50:00', '2023-02-20 13:50:00')
GO

-- 插入 comment 數據 (20筆)
INSERT INTO [comment] ([post_id], [user_id], [text], [created_at], [updated_at])
VALUES (1, 2, '寫得真好，我也要開始健身了！', '2023-02-01 11:00:00', '2023-02-01 11:00:00'),
       (2, 3, '這些飲食建議很實用，謝謝分享！', '2023-02-02 12:15:00', '2023-02-02 12:15:00'),
       (3, 4, '明天就來試試這個食譜！', '2023-02-03 15:30:00', '2023-02-03 15:30:00'),
       (4, 5, '重量訓練的技巧很專業！', '2023-02-04 17:45:00', '2023-02-04 17:45:00'),
       (5, 6, '堅持運動真的很重要！', '2023-02-05 09:10:00', '2023-02-05 09:10:00'),
       (6, 7, '增肌飲食計劃很詳細！', '2023-02-06 12:30:00', '2023-02-06 12:30:00'),
       (7, 8, '在家健身的指南很實用！', '2023-02-07 14:15:00', '2023-02-07 14:15:00'),
       (8, 9, '減脂餐看起來很好吃！', '2023-02-08 16:40:00', '2023-02-08 16:40:00'),
       (9, 10, '跑步計劃很科學！', '2023-02-09 18:25:00', '2023-02-09 18:25:00'),
       (10, 11, '克服平台期的方法很有幫助！', '2023-02-10 20:00:00', '2023-02-10 20:00:00'),
       (11, 12, '運動後補充的建議很專業！', '2023-02-11 21:35:00', '2023-02-11 21:35:00'),
       (12, 13, '核心訓練的方法很有效！', '2023-02-12 10:50:00', '2023-02-12 10:50:00'),
       (13, 14, '健康點心食譜很適合我！', '2023-02-13 12:15:00', '2023-02-13 12:15:00'),
       (14, 15, '彈力帶訓練很方便！', '2023-02-14 15:30:00', '2023-02-14 15:30:00'),
       (15, 16, '設定目標真的很重要！', '2023-02-15 17:45:00', '2023-02-15 17:45:00'),
       (16, 17, '蛋白質攝取的指南很詳細！', '2023-02-16 19:10:00', '2023-02-16 19:10:00'),
       (17, 18, '啞鈴訓練計劃很全面！', '2023-02-17 21:35:00', '2023-02-17 21:35:00'),
       (18, 19, '增肌餐看起來很美味！', '2023-02-18 10:00:00', '2023-02-18 10:00:00'),
       (19, 20, 'HIIT訓練真的很有效！', '2023-02-19 12:25:00', '2023-02-19 12:25:00'),
       (20, 1, '保持運動熱情的方法很棒！', '2023-02-20 14:50:00', '2023-02-20 14:50:00')
GO