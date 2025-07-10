package com.clinflash.baseai.domain.system.model.enums;

/**
 * <b>sys_settings.value_type</b> 枚举，定义系统支持的参数数据类型。
 *
 * <p>新增类型时请同步更新 SysSetting 解析器。</p>
 */
public enum SettingValueType {

    STRING,
    INT,
    BOOL,
    JSON,
    FLOAT,
    LONG
}
