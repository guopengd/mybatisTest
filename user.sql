-- auto-generated definition
create table User
(
    id     int auto_increment comment '主键id'
        primary key,
    name   varchar(20) default '' not null comment '姓名',
    age    int         default 0  not null comment '年龄',
    sex    int         default 0  not null comment '0：未知；1：男；2：女',
    mobile varchar(20) default '' not null comment '手机号'
)
    comment '用户表';
