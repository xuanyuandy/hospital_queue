drop table if exists get_info;
create table get_info(
    `id` int not null auto_increment,
    `name` varchar (100) not null,
    `arrive_time` int not null,
    primary key(id)
)engine=innodb auto_increment=2 default charset=utf8