-- 登录 MySQL 服务器
mysql -hlocalhost -uroot -pDjangobbs

-- 创建数据库 study_coupon_data
CREATE DATABASE IF NOT EXISTS study_coupon_data;

-- 登录 MySQL 服务器, 并进入到 study_coupon_data 数据库中
mysql -hlocalhost -uroot -p123456 -Dstudy_coupon_data