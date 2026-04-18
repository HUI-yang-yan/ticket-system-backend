-- ============================================
-- 12306票务系统 数据库初始化脚本
-- 包含: DDL + 测试数据
-- 生成时间: 2026-04-15
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ticket_12306 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE ticket_system;

-- ============================================
-- DDL 部分
-- ============================================

-- ----------------------------
-- 1. 车厢表
-- ----------------------------
start transaction ;
DROP TABLE IF EXISTS carriage;
CREATE TABLE carriage (
    id             bigint auto_increment comment '主键' primary key,
    train_id       bigint                             not null comment '列车ID',
    carriage_index int                                not null comment '车厢序号',
    carriage_type  varchar(20)                        not null comment '车厢类型',
    seat_count     int                                not null comment '座位数量',
    row_count      int                                null comment '行数',
    column_count   int                                null comment '列数',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_train_carriage unique (train_id, carriage_index)
) comment '车厢表' charset = utf8mb4;

-- ----------------------------
-- 2. 常用联系人表
-- ----------------------------
DROP TABLE IF EXISTS contact;
CREATE TABLE contact (
    id          bigint auto_increment comment '主键' primary key,
    user_id     bigint                   not null comment '所属用户ID',
    name        varchar(32)              not null comment '联系人姓名',
    phone       varchar(20)              not null comment '联系电话',
    id_card     varchar(32)              null comment '身份证号',
    type        tinyint                  not null comment '联系人类型 1-成人 2-儿童 3-学生',
    is_default  tinyint  default 0       not null comment '是否默认联系人 0-否 1-是',
    status      tinyint  default 1       not null comment '状态 0-删除 1-正常',
    create_time datetime default (now()) not null comment '创建时间',
    update_time datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_phone unique (user_id, phone)
) comment '常用联系人表' charset = utf8mb4;

create index idx_user_id on contact (user_id);

-- ----------------------------
-- 3. 订单表
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    id                   bigint auto_increment comment '主键' primary key,
    order_number         varchar(32)                          not null comment '订单号',
    user_id              bigint                               not null comment '用户ID',
    train_id             bigint                               not null comment '列车ID',
    departure_station_id bigint                               not null comment '出发站ID',
    arrival_station_id   bigint                               not null comment '到达站ID',
    departure_date       date                                 not null comment '出发日期',
    departure_time       datetime                             not null comment '出发时间',
    arrival_time         datetime                             not null comment '到达时间',
    seat_type            varchar(20)                          not null comment '座位类型',
    passenger_id_card    varchar(18)                          not null comment '乘客身份证',
    passenger_real_name  varchar(50)                          not null comment '乘客姓名',
    ticket_price         decimal(10, 2)                       not null comment '票价',
    order_status         tinyint(1) default 0                 null comment '订单状态 0:待支付 1:已支付 2:已取消 3:已完成 4:已退款',
    pay_status           tinyint(1) default 0                 null comment '支付状态 0:未支付 1:已支付',
    pay_time             datetime                             null comment '支付时间',
    expire_time          datetime                             not null comment '过期时间',
    create_time          datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time          datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_order_number unique (order_number)
) comment '订单表' charset = utf8mb4;

create index idx_create_time on `order` (create_time);
create index idx_train_date on `order` (train_id, departure_date);
create index idx_user on `order` (user_id);

-- ----------------------------
-- 4. 订单座位表
-- ----------------------------
DROP TABLE IF EXISTS order_seat;
CREATE TABLE order_seat (
    id             bigint auto_increment comment '主键' primary key,
    order_id       bigint                             not null comment '订单ID',
    seat_id        bigint                             not null comment '座位ID',
    carriage_index int                                not null comment '车厢序号',
    seat_number    varchar(10)                        not null comment '座位号',
    ticket_price   decimal(10, 2)                     not null comment '票价',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_order_seat unique (order_id, seat_id),
    constraint uk_seat_train_date unique (seat_id, order_id) comment '防止座位重复销售'
) comment '订单座位表' charset = utf8mb4;

-- ----------------------------
-- 5. 支付记录表
-- ----------------------------
DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
    id             bigint auto_increment comment '主键' primary key,
    order_id       bigint                               not null comment '订单ID',
    payment_number varchar(32)                          not null comment '支付流水号',
    payment_amount decimal(10, 2)                       not null comment '支付金额',
    payment_method varchar(20)                          null comment '支付方式',
    payment_status tinyint(1) default 0                 null comment '支付状态 0:支付中 1:支付成功 2:支付失败',
    payment_time   datetime                             null comment '支付时间',
    create_time    datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    refund_status  tinyint    default 0                 not null comment '退款状态：0-未退款 1-退款中 2-已退款 3-退款失败',
    constraint uk_order_payment unique (order_id),
    constraint uk_payment_number unique (payment_number)
) comment '支付记录表' charset = utf8mb4;

-- ----------------------------
-- 6. 退款任务表
-- ----------------------------
DROP TABLE IF EXISTS refund_task;
CREATE TABLE refund_task (
    id          bigint auto_increment primary key,
    order_id    bigint        not null,
    refund_no   varchar(64)   not null,
    status      tinyint       not null comment '0-待消费 1-处理中 2-成功 3-失败',
    retry_count int default 0 null,
    create_time datetime      null,
    update_time datetime      null,
    constraint uk_refund_no unique (refund_no)
);

-- ----------------------------
-- 7. 座位表
-- ----------------------------
DROP TABLE IF EXISTS seat;
CREATE TABLE seat (
    id          bigint auto_increment comment '主键' primary key,
    carriage_id bigint                               not null comment '车厢ID',
    seat_number varchar(10)                          not null comment '座位号',
    seat_type   varchar(20)                          null comment '座位类型',
    row_num     int                                  null comment '行号',
    col_num     int                                  null comment '列号',
    status      tinyint(1) default 1                 null comment '状态 0:不可用 1:可用',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_carriage_seat unique (carriage_id, seat_number)
) comment '座位表' charset = utf8mb4;

-- ----------------------------
-- 8. 车站表
-- ----------------------------
DROP TABLE IF EXISTS station;
CREATE TABLE station (
    id           bigint auto_increment comment '主键' primary key,
    station_code varchar(20)                          not null comment '车站编码',
    station_name varchar(50)                          not null comment '车站名称',
    city         varchar(50)                          null comment '所在城市',
    province     varchar(50)                          null comment '所在省份',
    pinyin       varchar(100)                         null comment '拼音',
    pinyin_short varchar(50)                          null comment '拼音简写',
    status       tinyint(1) default 1                 null comment '状态 0:停用 1:启用',
    create_time  datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_station_code unique (station_code),
    constraint uk_station_name unique (station_name)
) comment '车站表' charset = utf8mb4;

-- ----------------------------
-- 9. 车票库存表
-- ----------------------------
DROP TABLE IF EXISTS ticket_inventory;
CREATE TABLE ticket_inventory (
    id          bigint auto_increment comment '主键' primary key,
    train_id    bigint                             not null comment '列车ID',
    seat_type   varchar(20)                        not null comment '座位类型',
    total_count int                                not null comment '总票数',
    version     int      default 0                 null comment '版本号',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    price       bigint   default 0                 not null comment '票价',
    status      int      default 0                 not null comment '票状态'
) comment '车票库存表' charset = utf8mb4;

-- ----------------------------
-- 10. 列车表
-- ----------------------------
DROP TABLE IF EXISTS train;
CREATE TABLE train (
    id               bigint auto_increment comment '主键' primary key,
    train_number     varchar(20)                          not null comment '车次号',
    train_type       varchar(20)                          null comment '列车类型',
    start_station_id bigint                               not null comment '始发站ID',
    end_station_id   bigint                               not null comment '终点站ID',
    start_time       time                                 not null comment '发车时间',
    end_time         time                                 not null comment '到达时间',
    duration         int                                  null comment '运行时长(分钟)',
    status           tinyint(1) default 1                 not null comment '状态 0:停运 1:运行',
    create_time      datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_train_number unique (train_number)
) comment '列车表' charset = utf8mb4;

create index idx_end_station on train (end_station_id);
create index idx_start_station on train (start_station_id);

-- ----------------------------
-- 11. 车次区间库存表
-- ----------------------------
DROP TABLE IF EXISTS train_segment_stock;
CREATE TABLE train_segment_stock (
    id               bigint auto_increment comment '主键' primary key,
    train_id         bigint                             not null comment '车次ID',
    start_station_id bigint                             not null comment '区间起始站',
    travel_date      date                               not null comment '乘车日期',
    seat_type        varchar(16)                        not null comment '座席类型（二等座/一等座/无座）',
    stock            int                                not null comment '剩余库存',
    create_time      datetime default CURRENT_TIMESTAMP null,
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    price            bigint   default 0                 not null comment '区间票价',
    constraint uk_segment unique (train_id, start_station_id, travel_date, seat_type)
) comment '车次区间库存表 规定最后一站price=0';

create index idx_train_date on train_segment_stock (train_id, travel_date);

-- ----------------------------
-- 12. 列车站点表
-- ----------------------------
DROP TABLE IF EXISTS train_station;
CREATE TABLE train_station (
    id             bigint auto_increment comment '主键' primary key,
    train_id       bigint                             not null comment '列车ID',
    station_id     bigint                             not null comment '车站ID',
    station_index  int                                not null comment '站点序号',
    arrival_time   time                               null comment '到达时间',
    departure_time time                               null comment '发车时间',
    stop_duration  int      default 0                 null comment '停靠时长(分钟)',
    distance       int      default 0                 null comment '距离始发站距离(公里)',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_train_index unique (train_id, station_index),
    constraint uk_train_station unique (train_id, station_id)
) comment '列车站点表' charset = utf8mb4;

create index idx_station on train_station (station_id);

-- ----------------------------
-- 13. 用户表
-- ----------------------------
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          bigint auto_increment comment '主键' primary key,
    username    varchar(50)                          not null comment '用户名',
    password    varchar(255)                         not null comment '密码',
    id_card     varchar(18)                          not null comment '身份证号',
    real_name   varchar(50)                          not null comment '真实姓名',
    phone       varchar(11)                          null comment '手机号',
    email       varchar(100)                         null comment '邮箱',
    user_type   tinyint(1) default 0                 null comment '用户类型 0:普通用户 1:管理员',
    status      tinyint(1) default 1                 null comment '状态 0:禁用 1:正常',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_email unique (email),
    constraint uk_id_card unique (id_card),
    constraint uk_phone unique (phone),
    constraint uk_username unique (username)
) comment '用户表' charset = utf8mb4;

-- ----------------------------
-- 14. 候补订单表
-- ----------------------------
DROP TABLE IF EXISTS waitlist_order;
CREATE TABLE waitlist_order (
    id               bigint auto_increment primary key,
    waitlist_number  varchar(50)                           not null comment '候补单号',
    user_id          bigint                                not null comment '用户ID',
    train_id         bigint                                not null comment '车次ID',
    start_station_id bigint                                not null comment '出发站ID',
    end_station_id   bigint                                not null comment '到达站ID',
    seat_type        varchar(20)                           not null comment '座位类型',
    departure_date   date                                  not null comment '期望乘车日期',
    status           varchar(20) default 'PENDING'         not null comment '状态：PENDING/MATCHED/SUCCESS/EXPIRED/CANCELLED',
    order_number     varchar(50)                           null comment '关联正式订单号/候补票号',
    create_time      timestamp   default CURRENT_TIMESTAMP null comment '创建时间',
    expire_time      timestamp                             null comment '过期时间（匹配后24小时）',
    update_time      timestamp   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '候补订单表' charset = utf8mb4;

create index idx_create_time on waitlist_order (create_time);
create index idx_status on waitlist_order (status);
create index idx_train_date_seat on waitlist_order (train_id, departure_date, seat_type);
create index idx_user_id on waitlist_order (user_id);

-- ----------------------------
-- 15. 候补票池表
-- ----------------------------
DROP TABLE IF EXISTS waitlist_ticket;
CREATE TABLE waitlist_ticket (
    id                    bigint auto_increment primary key,
    ticket_number         varchar(50)                           not null comment '车票号码',
    original_order_number varchar(50)                           not null comment '原订单号',
    train_id              bigint                                not null comment '车次ID',
    start_station_id      bigint                                not null comment '出发站ID',
    end_station_id        bigint                                not null comment '到达站ID',
    departure_date        date                                  not null comment '乘车日期',
    seat_type             varchar(20)                           not null comment '座位类型',
    carriage_number       varchar(10)                           null comment '车厢号',
    seat_number           varchar(10)                            null comment '座位号',
    price                 decimal(10, 2)                        null comment '票价',
    status                varchar(20) default 'AVAILABLE'       not null comment '状态：AVAILABLE/CLAIMED/EXPIRED',
    source                varchar(20) default 'REFUND'          null comment '来源：REFUND',
    create_time           timestamp   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time           timestamp   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '候补票池表' charset = utf8mb4;

create index idx_create_time on waitlist_ticket (create_time);
create index idx_original_order on waitlist_ticket (original_order_number);
create index idx_status on waitlist_ticket (status);
create index idx_train_date_seat on waitlist_ticket (train_id, departure_date, seat_type);

-- ============================================
-- 测试数据部分
-- ============================================

START TRANSACTION;

-- ----------------------------
-- 1. 车站数据 (10条)
-- ----------------------------
INSERT INTO station (station_code, station_name, city, province, pinyin, pinyin_short, status, create_time, update_time) VALUES
('BJP', '北京', '北京市', '北京市', 'BEIJING', 'BJ', 1, NOW(), NOW()),
('SHH', '上海', '上海市', '上海市', 'SHANGHAI', 'SH', 1, NOW(), NOW()),
('GZH', '广州', '广州市', '广东省', 'GUANGZHOU', 'GZ', 1, NOW(), NOW()),
('SZU', '深圳', '深圳市', '广东省', 'SHENZHEN', 'SZ', 1, NOW(), NOW()),
('WHP', '武汉', '武汉市', '湖北省', 'WUHAN', 'WH', 1, NOW(), NOW()),
('NJH', '南京', '南京市', '江苏省', 'NANJING', 'NJ', 1, NOW(), NOW()),
('CDB', '成都', '成都市', '四川省', 'CHENGDU', 'CD', 1, NOW(), NOW()),
('XAB', '西安', '西安市', '陕西省', 'XIAN', 'XA', 1, NOW(), NOW()),
('HZD', '杭州', '杭州市', '浙江省', 'HANGZHOU', 'HZ', 1, NOW(), NOW()),
('TJH', '天津', '天津市', '天津市', 'TIANJIN', 'TJ', 1, NOW(), NOW());

-- ----------------------------
-- 2. 用户数据 (6条)
-- ----------------------------
INSERT INTO user (username, password, id_card, real_name, phone, email, user_type, status, create_time, update_time) VALUES
('test001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199001011234', '张三', '13800138001', 'zhangsan@test.com', 0, 1, NOW(), NOW()),
('test002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199002021234', '李四', '13800138002', 'lisi@test.com', 0, 1, NOW(), NOW()),
('test003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199003031234', '王五', '13800138003', 'wangwu@test.com', 0, 1, NOW(), NOW()),
('test004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199004041234', '赵六', '13800138004', 'zhaoliu@test.com', 0, 1, NOW(), NOW()),
('test005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199005051234', '钱七', '13800138005', 'qianqi@test.com', 0, 1, NOW(), NOW()),
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '110101199000000001', '管理员', '13900000001', 'admin@test.com', 1, 1, NOW(), NOW());

-- ----------------------------
-- 3. 联系人数据 (6条)
-- ----------------------------
INSERT INTO contact (user_id, name, phone, id_card, type, is_default, status, create_time, update_time) VALUES
(1, '张三', '13800138001', '110101199001011234', 1, 1, 1, NOW(), NOW()),
(1, '张小文', '13800138011', '110101199501011234', 2, 0, 1, NOW(), NOW()),
(2, '李四', '13800138002', '110101199002021234', 1, 1, 1, NOW(), NOW()),
(3, '王五', '13800138003', '110101199003031234', 1, 1, 1, NOW(), NOW()),
(4, '赵六', '13800138004', '110101199004041234', 1, 1, 1, NOW(), NOW()),
(5, '钱七', '13800138005', '110101199005051234', 1, 1, 1, NOW(), NOW());

-- ----------------------------
-- 4. 车次数据 (10条)
-- ----------------------------
INSERT INTO train (train_number, train_type, start_station_id, end_station_id, start_time, end_time, duration, status, create_time, update_time) VALUES
('G1', 'G', 1, 2, '08:00:00', '12:30:00', 270, 1, NOW(), NOW()),
('G2', 'G', 2, 1, '14:00:00', '18:30:00', 270, 1, NOW(), NOW()),
('D1', 'D', 1, 5, '09:00:00', '13:00:00', 240, 1, NOW(), NOW()),
('D2', 'D', 5, 1, '15:00:00', '19:00:00', 240, 1, NOW(), NOW()),
('G3', 'G', 3, 8, '08:30:00', '14:00:00', 330, 1, NOW(), NOW()),
('G4', 'G', 8, 3, '16:00:00', '21:30:00', 330, 1, NOW(), NOW()),
('K1', 'K', 1, 7, '07:00:00', '18:00:00', 660, 1, NOW(), NOW()),
('K2', 'K', 7, 1, '08:00:00', '19:00:00', 660, 1, NOW(), NOW()),
('G5', 'G', 4, 9, '09:30:00', '11:30:00', 120, 1, NOW(), NOW()),
('G6', 'G', 9, 4, '13:00:00', '15:00:00', 120, 1, NOW(), NOW());

-- ----------------------------
-- 5. 列车站点数据 (26条)
-- ----------------------------
INSERT INTO train_station (train_id, station_id, station_index, arrival_time, departure_time, stop_duration, distance, create_time) VALUES
-- G1: 北京 -> 南京 -> 上海
(1, 1, 1, NULL, '08:00:00', 0, 0, NOW()),
(1, 6, 2, '10:30:00', '10:35:00', 5, 600, NOW()),
(1, 2, 3, '12:30:00', NULL, 0, 1000, NOW()),
-- G2: 上海 -> 南京 -> 北京
(2, 2, 1, NULL, '14:00:00', 0, 0, NOW()),
(2, 6, 2, '16:00:00', '16:05:00', 5, 400, NOW()),
(2, 1, 3, '18:30:00', NULL, 0, 1000, NOW()),
-- D1: 北京 -> 武汉
(3, 1, 1, NULL, '09:00:00', 0, 0, NOW()),
(3, 5, 2, '13:00:00', NULL, 0, 800, NOW()),
-- D2: 武汉 -> 北京
(4, 5, 1, NULL, '15:00:00', 0, 0, NOW()),
(4, 1, 2, '19:00:00', NULL, 0, 800, NOW()),
-- G3: 广州 -> 武汉 -> 西安
(5, 3, 1, NULL, '08:30:00', 0, 0, NOW()),
(5, 5, 2, '11:00:00', '11:10:00', 10, 500, NOW()),
(5, 8, 3, '14:00:00', NULL, 0, 1100, NOW()),
-- G4: 西安 -> 武汉 -> 广州
(6, 8, 1, NULL, '16:00:00', 0, 0, NOW()),
(6, 5, 2, '19:00:00', '19:10:00', 10, 600, NOW()),
(6, 3, 3, '21:30:00', NULL, 0, 1100, NOW()),
-- K1: 北京 -> 武汉 -> 成都
(7, 1, 1, NULL, '07:00:00', 0, 0, NOW()),
(7, 5, 2, '12:00:00', '12:30:00', 30, 800, NOW()),
(7, 7, 3, '18:00:00', NULL, 0, 1500, NOW()),
-- K2: 成都 -> 武汉 -> 北京
(8, 7, 1, NULL, '08:00:00', 0, 0, NOW()),
(8, 5, 2, '14:00:00', '14:30:00', 30, 700, NOW()),
(8, 1, 3, '19:00:00', NULL, 0, 1500, NOW()),
-- G5: 深圳 -> 杭州
(9, 4, 1, NULL, '09:30:00', 0, 0, NOW()),
(9, 9, 2, '11:30:00', NULL, 0, 600, NOW()),
-- G6: 杭州 -> 深圳
(10, 9, 1, NULL, '13:00:00', 0, 0, NOW()),
(10, 4, 2, '15:00:00', NULL, 0, 600, NOW());

-- ----------------------------
-- 6. 车厢数据 (18条)
-- ----------------------------
INSERT INTO carriage (train_id, carriage_index, carriage_type, seat_count, row_count, column_count, create_time) VALUES
-- G1 车厢
(1, 1, 'BUSINESS', 10, 2, 5, NOW()),
(1, 2, 'FIRST_CLASS', 50, 10, 5, NOW()),
(1, 3, 'SECOND_CLASS', 90, 18, 5, NOW()),
(1, 4, 'SECOND_CLASS', 90, 18, 5, NOW()),
(1, 5, 'SECOND_CLASS', 90, 18, 5, NOW()),
(1, 6, 'SECOND_CLASS', 90, 18, 5, NOW()),
(1, 7, 'SECOND_CLASS', 90, 18, 5, NOW()),
(1, 8, 'SECOND_CLASS', 90, 18, 5, NOW()),
-- G2 车厢
(2, 1, 'BUSINESS', 10, 2, 5, NOW()),
(2, 2, 'FIRST_CLASS', 50, 10, 5, NOW()),
(2, 3, 'SECOND_CLASS', 90, 18, 5, NOW()),
(2, 4, 'SECOND_CLASS', 90, 18, 5, NOW()),
-- D1 车厢
(3, 1, 'FIRST_CLASS', 60, 12, 5, NOW()),
(3, 2, 'SECOND_CLASS', 100, 20, 5, NOW()),
(3, 3, 'SECOND_CLASS', 100, 20, 5, NOW()),
-- G5 车厢
(9, 1, 'BUSINESS', 8, 2, 4, NOW()),
(9, 2, 'FIRST_CLASS', 40, 10, 4, NOW()),
(9, 3, 'SECOND_CLASS', 80, 20, 4, NOW());

-- ----------------------------
-- 7. 座位数据 (34条)
-- ----------------------------
INSERT INTO seat (carriage_id, seat_number, seat_type, row_num, col_num, status, create_time) VALUES
-- G1 BUSINESS 车厢 (carriage_id=1)
(1, '01A', 'BUSINESS', 1, 1, 1, NOW()),
(1, '01C', 'BUSINESS', 1, 3, 1, NOW()),
(1, '01D', 'BUSINESS', 1, 4, 1, NOW()),
(1, '01F', 'BUSINESS', 1, 6, 1, NOW()),
(1, '02A', 'BUSINESS', 2, 1, 1, NOW()),
(1, '02C', 'BUSINESS', 2, 3, 1, NOW()),
(1, '02D', 'BUSINESS', 2, 4, 1, NOW()),
(1, '02F', 'BUSINESS', 2, 6, 1, NOW()),
-- G1 FIRST_CLASS 车厢 (carriage_id=2)
(2, '01A', 'FIRST_CLASS', 1, 1, 1, NOW()),
(2, '01C', 'FIRST_CLASS', 1, 3, 1, NOW()),
(2, '01D', 'FIRST_CLASS', 1, 4, 1, NOW()),
(2, '01F', 'FIRST_CLASS', 1, 6, 1, NOW()),
(2, '02A', 'FIRST_CLASS', 2, 1, 1, NOW()),
(2, '02C', 'FIRST_CLASS', 2, 3, 1, NOW()),
(2, '02D', 'FIRST_CLASS', 2, 4, 1, NOW()),
(2, '02F', 'FIRST_CLASS', 2, 6, 1, NOW()),
-- G1 SECOND_CLASS 车厢 (carriage_id=3)
(3, '01A', 'SECOND_CLASS', 1, 1, 1, NOW()),
(3, '01B', 'SECOND_CLASS', 1, 2, 1, NOW()),
(3, '01C', 'SECOND_CLASS', 1, 3, 1, NOW()),
(3, '01D', 'SECOND_CLASS', 1, 4, 1, NOW()),
(3, '01F', 'SECOND_CLASS', 1, 6, 1, NOW()),
-- G5 BUSINESS 车厢 (carriage_id=16)
(16, '01A', 'BUSINESS', 1, 1, 1, NOW()),
(16, '01B', 'BUSINESS', 1, 2, 1, NOW()),
(16, '01D', 'BUSINESS', 1, 4, 1, NOW()),
(16, '01F', 'BUSINESS', 1, 6, 1, NOW()),
-- G5 FIRST_CLASS 车厢 (carriage_id=17)
(17, '01A', 'FIRST_CLASS', 1, 1, 1, NOW()),
(17, '01C', 'FIRST_CLASS', 1, 3, 1, NOW()),
(17, '01D', 'FIRST_CLASS', 1, 4, 1, NOW()),
(17, '01F', 'FIRST_CLASS', 1, 6, 1, NOW()),
-- G5 SECOND_CLASS 车厢 (carriage_id=18)
(18, '01A', 'SECOND_CLASS', 1, 1, 1, NOW()),
(18, '01B', 'SECOND_CLASS', 1, 2, 1, NOW()),
(18, '01C', 'SECOND_CLASS', 1, 3, 1, NOW()),
(18, '01D', 'SECOND_CLASS', 1, 4, 1, NOW()),
(18, '01F', 'SECOND_CLASS', 1, 6, 1, NOW());

-- ----------------------------
-- 8. 车次区间库存数据 (相邻站点区间)
-- ----------------------------
INSERT INTO train_segment_stock (train_id, start_station_id, travel_date, seat_type, stock, price, create_time, update_time) VALUES
-- G1: 北京(1) -> 南京(6) -> 上海(2)
-- 区间1: 北京-南京
(1, 1, '2026-04-16', 'SECOND', 80, 309, NOW(), NOW()),
(1, 1, '2026-04-16', 'FIRST', 40, 522, NOW(), NOW()),
(1, 1, '2026-04-16', 'BUSINESS', 8, 998, NOW(), NOW()),
(1, 1, '2026-04-17', 'SECOND', 90, 309, NOW(), NOW()),
(1, 1, '2026-04-17', 'FIRST', 45, 522, NOW(), NOW()),
-- 区间2: 南京-上海
(1, 6, '2026-04-16', 'SECOND', 75, 244, NOW(), NOW()),
(1, 6, '2026-04-16', 'FIRST', 38, 411, NOW(), NOW()),
(1, 6, '2026-04-16', 'BUSINESS', 6, 750, NOW(), NOW()),
(1, 6, '2026-04-17', 'SECOND', 85, 244, NOW(), NOW()),

-- G2: 上海(2) -> 南京(6) -> 北京(1)
-- 区间1: 上海-南京
(2, 2, '2026-04-16', 'SECOND', 70, 244, NOW(), NOW()),
(2, 2, '2026-04-16', 'FIRST', 35, 411, NOW(), NOW()),
(2, 2, '2026-04-16', 'BUSINESS', 5, 750, NOW(), NOW()),
-- 区间2: 南京-北京
(2, 6, '2026-04-16', 'SECOND', 68, 553, NOW(), NOW()),
(2, 6, '2026-04-16', 'FIRST', 33, 933, NOW(), NOW()),

-- D1: 北京(1) -> 武汉(5)
(3, 1, '2026-04-16', 'SECOND', 100, 429, NOW(), NOW()),
(3, 1, '2026-04-16', 'FIRST', 55, 724, NOW(), NOW()),
(3, 1, '2026-04-17', 'SECOND', 115, 429, NOW(), NOW()),
(3, 1, '2026-04-17', 'FIRST', 58, 724, NOW(), NOW()),

-- D2: 武汉(5) -> 北京(1)
(4, 5, '2026-04-16', 'SECOND', 95, 429, NOW(), NOW()),
(4, 5, '2026-04-16', 'FIRST', 50, 724, NOW(), NOW()),

-- G3: 广州(3) -> 武汉(5) -> 西安(8)
-- 区间1: 广州-武汉
(5, 3, '2026-04-16', 'SECOND', 70, 399, NOW(), NOW()),
(5, 3, '2026-04-16', 'FIRST', 35, 673, NOW(), NOW()),
(5, 3, '2026-04-16', 'BUSINESS', 5, 1268, NOW(), NOW()),
-- 区间2: 武汉-西安
(5, 5, '2026-04-16', 'SECOND', 65, 260, NOW(), NOW()),
(5, 5, '2026-04-16', 'FIRST', 30, 432, NOW(), NOW()),
(5, 5, '2026-04-16', 'BUSINESS', 4, 813, NOW(), NOW()),

-- G4: 西安(8) -> 武汉(5) -> 广州(3)
-- 区间1: 西安-武汉
(6, 8, '2026-04-16', 'SECOND', 60, 260, NOW(), NOW()),
(6, 8, '2026-04-16', 'FIRST', 28, 432, NOW(), NOW()),
-- 区间2: 武汉-广州
(6, 5, '2026-04-16', 'SECOND', 55, 399, NOW(), NOW()),
(6, 5, '2026-04-16', 'FIRST', 25, 673, NOW(), NOW()),

-- K1: 北京(1) -> 武汉(5) -> 成都(7)
-- 区间1: 北京-武汉
(7, 1, '2026-04-16', 'SECOND', 80, 327, NOW(), NOW()),
(7, 1, '2026-04-16', 'HARD_SEAT', 100, 192, NOW(), NOW()),
-- 区间2: 武汉-成都
(7, 5, '2026-04-16', 'SECOND', 75, 380, NOW(), NOW()),
(7, 5, '2026-04-16', 'HARD_SEAT', 95, 224, NOW(), NOW()),

-- K2: 成都(7) -> 武汉(5) -> 北京(1)
-- 区间1: 成都-武汉
(8, 7, '2026-04-16', 'SECOND', 70, 380, NOW(), NOW()),
(8, 7, '2026-04-16', 'HARD_SEAT', 90, 224, NOW(), NOW()),
-- 区间2: 武汉-北京
(8, 5, '2026-04-16', 'SECOND', 65, 327, NOW(), NOW()),
(8, 5, '2026-04-16', 'HARD_SEAT', 85, 192, NOW(), NOW()),

-- G5: 深圳(4) -> 杭州(9)
(9, 4, '2026-04-16', 'SECOND', 65, 321, NOW(), NOW()),
(9, 4, '2026-04-16', 'FIRST', 30, 546, NOW(), NOW()),
(9, 4, '2026-04-16', 'BUSINESS', 6, 1045, NOW(), NOW()),
(9, 4, '2026-04-17', 'SECOND', 78, 321, NOW(), NOW()),
(9, 4, '2026-04-17', 'FIRST', 38, 546, NOW(), NOW()),

-- G6: 杭州(9) -> 深圳(4)
(10, 9, '2026-04-16', 'SECOND', 70, 321, NOW(), NOW()),
(10, 9, '2026-04-16', 'FIRST', 32, 546, NOW(), NOW()),
(10, 9, '2026-04-16', 'BUSINESS', 5, 1045, NOW(), NOW());

-- ----------------------------
-- 9. 订单数据 (6条)
-- order_status: 0-待支付 1-已支付 2-已取消 3-已完成 4-已退款
-- pay_status: 0-未支付 1-已支付
-- ----------------------------
INSERT INTO `order` (order_number, user_id, train_id, departure_station_id, arrival_station_id, departure_date, departure_time, arrival_time, seat_type, passenger_id_card, passenger_real_name, ticket_price, order_status, pay_status, pay_time, expire_time, create_time, update_time) VALUES
('ORD2026041600001', 1, 1, 1, 2, '2026-04-16', '2026-04-16 08:00:00', '2026-04-16 12:30:00', 'SECOND', '110101199001011234', '张三', 553.00, 1, 1, '2026-04-15 10:00:00', '2026-04-15 10:30:00', '2026-04-15 09:30:00', NOW()),
('ORD2026041600002', 2, 1, 1, 2, '2026-04-16', '2026-04-16 08:00:00', '2026-04-16 12:30:00', 'FIRST', '110101199002021234', '李四', 933.00, 1, 1, '2026-04-15 11:00:00', '2026-04-15 11:30:00', '2026-04-15 10:30:00', NOW()),
('ORD2026041600003', 3, 3, 1, 5, '2026-04-16', '2026-04-16 09:00:00', '2026-04-16 13:00:00', 'SECOND', '110101199003031234', '王五', 429.00, 1, 1, '2026-04-15 12:00:00', '2026-04-15 12:30:00', '2026-04-15 11:30:00', NOW()),
('ORD2026041600004', 4, 5, 3, 8, '2026-04-16', '2026-04-16 08:30:00', '2026-04-16 14:00:00', 'SECOND', '110101199004041234', '赵六', 659.00, 0, 0, NULL, '2026-04-15 14:00:00', '2026-04-15 13:00:00', NOW()),
('ORD2026041500005', 5, 9, 4, 9, '2026-04-16', '2026-04-16 09:30:00', '2026-04-16 11:30:00', 'FIRST', '110101199005051234', '钱七', 546.00, 1, 1, '2026-04-15 09:00:00', '2026-04-15 09:30:00', '2026-04-15 08:30:00', NOW()),
('ORD2026041600006', 1, 1, 6, 2, '2026-04-16', '2026-04-16 10:35:00', '2026-04-16 12:30:00', 'BUSINESS', '110101199001011234', '张三', 750.00, 1, 1, '2026-04-15 11:30:00', '2026-04-15 12:00:00', '2026-04-15 10:30:00', NOW());

-- ----------------------------
-- 10. 支付数据 (6条)
-- payment_status: 0-支付中 1-支付成功 2-支付失败
-- refund_status: 0-未退款 1-退款中 2-已退款 3-退款失败
-- ----------------------------
INSERT INTO payment (order_id, payment_number, payment_amount, payment_method, payment_status, payment_time, refund_status, create_time, update_time) VALUES
(1, 'PAY2026041500001', 553.00, 'ALIPAY', 1, '2026-04-15 10:00:00', 0, '2026-04-15 09:30:00', NOW()),
(2, 'PAY2026041500002', 933.00, 'WECHAT', 1, '2026-04-15 11:00:00', 0, '2026-04-15 10:30:00', NOW()),
(3, 'PAY2026041500003', 429.00, 'ALIPAY', 1, '2026-04-15 12:00:00', 0, '2026-04-15 11:30:00', NOW()),
(4, 'PAY2026041500004', 659.00, 'WECHAT', 0, NULL, 0, '2026-04-15 13:00:00', NOW()),
(5, 'PAY2026041500005', 546.00, 'ALIPAY', 1, '2026-04-15 09:00:00', 0, '2026-04-15 08:30:00', NOW()),
(6, 'PAY2026041500006', 750.00, 'ALIPAY', 1, '2026-04-15 11:30:00', 0, '2026-04-15 10:30:00', NOW());

-- ----------------------------
-- 11. 订单座位数据 (5条)
-- ----------------------------
INSERT INTO order_seat (order_id, seat_id, carriage_index, seat_number, ticket_price, create_time) VALUES
(1, 17, 3, '01A', 553.00, NOW()),
(2, 9, 2, '01A', 933.00, NOW()),
(3, 16, 3, '01D', 429.00, NOW()),
(5, 26, 17, '01A', 546.00, NOW()),
(6, 1, 1, '01A', 750.00, NOW());

-- ----------------------------
-- 12. 候补订单数据 (5条)
-- status: PENDING/MATCHED/SUCCESS/EXPIRED/CANCELLED
-- ----------------------------
INSERT INTO waitlist_order (waitlist_number, user_id, train_id, start_station_id, end_station_id, seat_type, departure_date, status, order_number, create_time, expire_time, update_time) VALUES
('WL202604150001', 1, 1, 1, 2, 'BUSINESS', '2026-04-16', 'PENDING', NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),
('WL202604150002', 2, 3, 1, 5, 'FIRST', '2026-04-16', 'PENDING', NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),
('WL202604150003', 3, 5, 3, 8, 'BUSINESS', '2026-04-16', 'MATCHED', NULL, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW()),
('WL202604150004', 4, 9, 4, 9, 'SECOND', '2026-04-17', 'PENDING', NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),
('WL202604150005', 5, 2, 2, 1, 'FIRST', '2026-04-16', 'EXPIRED', NULL, NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ----------------------------
-- 13. 候补票池数据 (5条)
-- status: AVAILABLE/CLAIMED/EXPIRED
-- source: REFUND
-- ----------------------------
INSERT INTO waitlist_ticket (ticket_number, original_order_number, train_id, start_station_id, end_station_id, departure_date, seat_type, carriage_number, seat_number, price, status, source, create_time, update_time) VALUES
('TK202604150001', 'ORD2026041000001', 1, 1, 2, '2026-04-16', 'SECOND', '05', '12A', 553.00, 'AVAILABLE', 'REFUND', NOW(), NOW()),
('TK202604150002', 'ORD2026041000002', 1, 1, 2, '2026-04-16', 'SECOND', '05', '12B', 553.00, 'AVAILABLE', 'REFUND', NOW(), NOW()),
('TK202604150003', 'ORD2026041000003', 3, 1, 5, '2026-04-16', 'FIRST', '03', '08C', 724.00, 'AVAILABLE', 'REFUND', NOW(), NOW()),
('TK202604150004', 'ORD2026041000004', 5, 3, 8, '2026-04-16', 'BUSINESS', '01', '04A', 2081.00, 'CLAIMED', 'REFUND', NOW(), NOW()),
('TK202604150005', 'ORD2026041000005', 9, 4, 9, '2026-04-17', 'SECOND', '08', '15F', 321.00, 'AVAILABLE', 'REFUND', NOW(), NOW());

-- ----------------------------
-- 14. 退款任务数据 (2条)
-- status: 0-待消费 1-处理中 2-成功 3-失败
-- ----------------------------
INSERT INTO refund_task (order_id, refund_no, status, retry_count, create_time, update_time) VALUES
(1, 'REFUND202604150001', 2, 0, NOW(), NOW()),
(6, 'REFUND202604150002', 2, 0, NOW(), NOW());

COMMIT;

-- ============================================
-- 数据验证
-- ============================================
SELECT 'station' as tbl, COUNT(*) as cnt FROM station
UNION ALL SELECT 'user', COUNT(*) FROM user
UNION ALL SELECT 'contact', COUNT(*) FROM contact
UNION ALL SELECT 'train', COUNT(*) FROM train
UNION ALL SELECT 'train_station', COUNT(*) FROM train_station
UNION ALL SELECT 'carriage', COUNT(*) FROM carriage
UNION ALL SELECT 'seat', COUNT(*) FROM seat
UNION ALL SELECT 'train_segment_stock', COUNT(*) FROM train_segment_stock
UNION ALL SELECT 'order', COUNT(*) FROM `order`
UNION ALL SELECT 'payment', COUNT(*) FROM payment
UNION ALL SELECT 'order_seat', COUNT(*) FROM order_seat
UNION ALL SELECT 'waitlist_order', COUNT(*) FROM waitlist_order
UNION ALL SELECT 'waitlist_ticket', COUNT(*) FROM waitlist_ticket
UNION ALL SELECT 'refund_task', COUNT(*) FROM refund_task;
