-- auto-generated definition
create table carriage
(
    id             bigint auto_increment comment '主键'
        primary key,
    train_id       bigint                             not null comment '列车ID',
    carriage_index int                                not null comment '车厢序号',
    carriage_type  varchar(20)                        not null comment '车厢类型',
    seat_count     int                                not null comment '座位数量',
    row_count      int                                null comment '行数',
    column_count   int                                null comment '列数',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_train_carriage
        unique (train_id, carriage_index)
)
    comment '车厢表' charset = utf8mb4;

-- auto-generated definition
create table contact
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                   not null comment '所属用户ID',
    name        varchar(32)              not null comment '联系人姓名',
    phone       varchar(20)              not null comment '联系电话',
    id_card     varchar(32)              null comment '身份证号',
    type        tinyint                  not null comment '联系人类型 1-成人 2-儿童 3-学生',
    is_default  tinyint  default 0       not null comment '是否默认联系人 0-否 1-是',
    status      tinyint  default 1       not null comment '状态 0-删除 1-正常',
    create_time datetime default (now()) not null comment '创建时间',
    update_time datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_phone
        unique (user_id, phone)
)
    comment '常用联系人表' charset = utf8mb4;

create index idx_user_id
    on contact (user_id);



-- auto-generated definition
create table `order`
(
    id                   bigint auto_increment comment '主键'
        primary key,
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
    constraint uk_order_number
        unique (order_number)
)
    comment '订单表' charset = utf8mb4;

create index idx_create_time
    on `order` (create_time);

create index idx_train_date
    on `order` (train_id, departure_date);

create index idx_user
    on `order` (user_id);


-- auto-generated definition
create table order_seat
(
    id             bigint auto_increment comment '主键'
        primary key,
    order_id       bigint                             not null comment '订单ID',
    seat_id        bigint                             not null comment '座位ID',
    carriage_index int                                not null comment '车厢序号',
    seat_number    varchar(10)                        not null comment '座位号',
    ticket_price   decimal(10, 2)                     not null comment '票价',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_order_seat
        unique (order_id, seat_id),
    constraint uk_seat_train_date
        unique (seat_id, order_id) comment '防止座位重复销售'
)
    comment '订单座位表' charset = utf8mb4;

-- auto-generated definition
create table payment
(
    id             bigint auto_increment comment '主键'
        primary key,
    order_id       bigint                               not null comment '订单ID',
    payment_number varchar(32)                          not null comment '支付流水号',
    payment_amount decimal(10, 2)                       not null comment '支付金额',
    payment_method varchar(20)                          null comment '支付方式',
    payment_status tinyint(1) default 0                 null comment '支付状态 0:支付中 1:支付成功 2:支付失败',
    payment_time   datetime                             null comment '支付时间',
    create_time    datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    refund_status  tinyint    default 0                 not null comment '退款状态：0-未退款 1-退款中 2-已退款 3-退款失败',
    constraint uk_order_payment
        unique (order_id),
    constraint uk_payment_number
        unique (payment_number)
)
    comment '支付记录表' charset = utf8mb4;

-- auto-generated definition
create table refund_task
(
    id          bigint auto_increment
        primary key,
    order_id    bigint        not null,
    refund_no   varchar(64)   not null,
    status      tinyint       not null comment '0-待消费 1-处理中 2-成功 3-失败',
    retry_count int default 0 null,
    create_time datetime      null,
    update_time datetime      null,
    constraint uk_refund_no
        unique (refund_no)
);

-- auto-generated definition
create table seat
(
    id          bigint auto_increment comment '主键'
        primary key,
    carriage_id bigint                               not null comment '车厢ID',
    seat_number varchar(10)                          not null comment '座位号',
    seat_type   varchar(20)                          null comment '座位类型',
    row_num     int                                  null comment '行号',
    col_num     int                                  null comment '列号',
    status      tinyint(1) default 1                 null comment '状态 0:不可用 1:可用',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_carriage_seat
        unique (carriage_id, seat_number)
)
    comment '座位表' charset = utf8mb4;

-- auto-generated definition
create table station
(
    id           bigint auto_increment comment '主键'
        primary key,
    station_code varchar(20)                          not null comment '车站编码',
    station_name varchar(50)                          not null comment '车站名称',
    city         varchar(50)                          null comment '所在城市',
    province     varchar(50)                          null comment '所在省份',
    pinyin       varchar(100)                         null comment '拼音',
    pinyin_short varchar(50)                          null comment '拼音简写',
    status       tinyint(1) default 1                 null comment '状态 0:停用 1:启用',
    create_time  datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_station_code
        unique (station_code),
    constraint uk_station_name
        unique (station_name)
)
    comment '车站表' charset = utf8mb4;

-- auto-generated definition
create table ticket_inventory
(
    id          bigint auto_increment comment '主键'
        primary key,
    train_id    bigint                             not null comment '列车ID',
    seat_type   varchar(20)                        not null comment '座位类型',
    total_count int                                not null comment '总票数',
    version     int      default 0                 null comment '版本号',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    price       bigint   default 0                 not null comment '票价',
    status      int      default 0                 not null comment '票状态'
)
    comment '车票库存表' charset = utf8mb4;

-- auto-generated definition
create table train
(
    id               bigint auto_increment comment '主键'
        primary key,
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
    constraint uk_train_number
        unique (train_number)
)
    comment '列车表' charset = utf8mb4;

create index idx_end_station
    on train (end_station_id);

create index idx_start_station
    on train (start_station_id);

-- auto-generated definition
create table train_segment_stock
(
    id               bigint auto_increment comment '主键'
        primary key,
    train_id         bigint                             not null comment '车次ID',
    start_station_id bigint                             not null comment '区间起始站',
    travel_date      date                               not null comment '乘车日期',
    seat_type        varchar(16)                        not null comment '座席类型（二等座/一等座/无座）',
    stock            int                                not null comment '剩余库存',
    create_time      datetime default CURRENT_TIMESTAMP null,
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    price            bigint   default 0                 not null comment '区间票价',
    constraint uk_segment
        unique (train_id, start_station_id, travel_date, seat_type)
)
    comment '车次区间库存表 规定最后一战price=0';

create index idx_train_date
    on train_segment_stock (train_id, travel_date);

-- auto-generated definition
create table train_station
(
    id             bigint auto_increment comment '主键'
        primary key,
    train_id       bigint                             not null comment '列车ID',
    station_id     bigint                             not null comment '车站ID',
    station_index  int                                not null comment '站点序号',
    arrival_time   time                               null comment '到达时间',
    departure_time time                               null comment '发车时间',
    stop_duration  int      default 0                 null comment '停靠时长(分钟)',
    distance       int      default 0                 null comment '距离始发站距离(公里)',
    create_time    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_train_index
        unique (train_id, station_index),
    constraint uk_train_station
        unique (train_id, station_id)
)
    comment '列车站点表' charset = utf8mb4;

create index idx_station
    on train_station (station_id);

-- auto-generated definition
create table user
(
    id          bigint auto_increment comment '主键'
        primary key,
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
    constraint uk_email
        unique (email),
    constraint uk_id_card
        unique (id_card),
    constraint uk_phone
        unique (phone),
    constraint uk_username
        unique (username)
)
    comment '用户表' charset = utf8mb4;

-- auto-generated definition
create table waitlist_order
(
    id               bigint auto_increment
        primary key,
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
)
    comment '候补订单表' charset = utf8mb4;

create index idx_create_time
    on waitlist_order (create_time);

create index idx_status
    on waitlist_order (status);

create index idx_train_date_seat
    on waitlist_order (train_id, departure_date, seat_type);

create index idx_user_id
    on waitlist_order (user_id);

-- auto-generated definition
create table waitlist_ticket
(
    id                    bigint auto_increment
        primary key,
    ticket_number         varchar(50)                           not null comment '车票号码',
    original_order_number varchar(50)                           not null comment '原订单号',
    train_id              bigint                                not null comment '车次ID',
    start_station_id      bigint                                not null comment '出发站ID',
    end_station_id        bigint                                not null comment '到达站ID',
    departure_date        date                                  not null comment '乘车日期',
    seat_type             varchar(20)                           not null comment '座位类型',
    carriage_number       varchar(10)                           null comment '车厢号',
    seat_number           varchar(10)                           null comment '座位号',
    price                 decimal(10, 2)                        null comment '票价',
    status                varchar(20) default 'AVAILABLE'       not null comment '状态：AVAILABLE/CLAIMED/EXPIRED',
    source                varchar(20) default 'REFUND'          null comment '来源：REFUND',
    create_time           timestamp   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time           timestamp   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '候补票池表' charset = utf8mb4;

create index idx_create_time
    on waitlist_ticket (create_time);

create index idx_original_order
    on waitlist_ticket (original_order_number);

create index idx_status
    on waitlist_ticket (status);

create index idx_train_date_seat
    on waitlist_ticket (train_id, departure_date, seat_type);


