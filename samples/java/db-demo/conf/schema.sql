drop table users;
drop table tokens;

/*
create table linked_account (
  id                        bigint auto_increment not null,
  user_id                   bigint,
  provider_user_id          varchar(255),
  provider_key              varchar(255),
  constraint pk_linked_account primary key (id))
;

create table security_role (
  id                        bigint auto_increment not null,
  role_name                 varchar(255),
  constraint pk_security_role primary key (id))
;

create table token_action (
  id                        bigint auto_increment not null,
  token                     varchar(255),
  type                      varchar(2),
  created                   datetime,
  expires                   datetime,
  constraint ck_token_action_type check (type in ('EV','PR')),
  constraint uq_token_action_token unique (token),
  constraint pk_token_action primary key (id))
;
*/

create table tokens (
  id                        bigint auto_increment not null,
  email                     varchar(255),
  uuid                      varchar(32),
  creationTime              datetime,
  expirationTime            datetime,
  isSignUp                  char,
  constraint pk_users primary key (id))
;

create table users (
  id                        bigint auto_increment not null,
  email                     varchar(255),
  userId                    varchar(255),
  providerId                varchar(255),
  avatarUrl                 varchar(1024),
  fullName                  varchar(255),
  oAuth1Info_secret                varchar(255),
  oAuth1Info_token                varchar(255),
  oAuth1Info_productPrefix                varchar(255),
  constraint pk_users primary key (id))
;
