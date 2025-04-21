/*
 * Copyright 2025 CodeThink Technologies and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- This file is part of the Xiaoming Kotlin SDK Local Core Data SQL module.

-- Must replace following placeholders with actual values:
-- ${table_name_prefix}

create table if not exists ${table_name_prefix}subject
(
    id      int unsigned auto_increment primary key,
    type    varchar(255) not null,
    remove  bool         not null default false,
    content json         not null,

    index idx_type (type),
    index idx_remove (remove)
);

create table if not exists ${table_name_prefix}permission_bundle
(
    id         int unsigned auto_increment primary key,
    subject_id int unsigned not null,
    remove     bool         not null default false,

    index idx_subject_id (subject_id),
    index idx_remove (remove)
);

create table if not exists ${table_name_prefix}permission_entry
(
    id                      int unsigned auto_increment primary key,
    bundle_id               int unsigned not null,

    matcher_type            varchar(255) not null,

    matcher_inherited_id    int unsigned null     default null,
    matcher_wild_card_id    varchar(255) null     default null,
    matcher_wild_card_value bool         null     default null,

    constraints             json         not null,

    operation_id            int unsigned null,
    operation_operator_id   int unsigned not null,
    operation_cause         json         not null,
    operation_time          long         not null,

    remove                  bool         not null default false,

    index idx_bundle_id (bundle_id),
    index idx_operation_id (operation_id),
    index idx_operation_operator_id (operation_operator_id),
    index idx_operation_time (operation_time),
    index idx_remove (remove)
);
