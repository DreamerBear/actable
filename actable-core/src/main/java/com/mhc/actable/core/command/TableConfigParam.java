package com.mhc.actable.core.command;

import com.mhc.actable.annotation.Table;
import lombok.*;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.mhc.actable.command
 * @author: xuchao（xuchao@maihaoche.com）
 * @date: 2019-07-04 16:52
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableConfigParam {
    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    public TableConfigParam(Table table) {
        tableName = table.name();
        tableComment = table.comment();
    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == this.getClass()) {
            TableConfigParam target = (TableConfigParam) obj;
            return tableName.equalsIgnoreCase(target.getTableName());
        }
        return false;
    }

    @Override
    public String toString() {
        return tableName + "[" + tableComment + "]";
    }
}
