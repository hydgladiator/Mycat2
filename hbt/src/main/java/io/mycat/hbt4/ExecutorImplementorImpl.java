/**
 * Copyright (C) <2020>  <chen junwen>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
package io.mycat.hbt4;

import io.mycat.MycatDataContext;
import io.mycat.beans.mycat.MycatRowMetaData;
import io.mycat.calcite.resultset.CalciteRowMetaData;
import io.mycat.calcite.table.MycatTransientSQLTableScan;
import io.mycat.hbt3.MycatLookUpView;
import io.mycat.hbt3.View;
import io.mycat.hbt4.executor.*;
import io.mycat.hbt4.logical.rel.MycatInsertRel;
import io.mycat.hbt4.logical.rel.MycatUpdateRel;
import io.mycat.metadata.QueryBuilder;

public abstract class ExecutorImplementorImpl extends BaseExecutorImplementor {
    private final MycatDataContext context;
    protected final DataSourceFactory factory;


    public ExecutorImplementorImpl(MycatDataContext context, DataSourceFactory factory,
                                   TempResultSetFactory tempResultSetFactory) {
        super(tempResultSetFactory);
        this.context = context;
        this.factory = factory;
    }



    @Override
    public Executor implement(View view) {
        return ViewExecutor.create(context,view, forUpdate, params, factory);
    }

    @Override
    public Executor implement(MycatTransientSQLTableScan tableScan) {
        MycatRowMetaData calciteRowMetaData = new CalciteRowMetaData(tableScan.getRowType().getFieldList());
        return TmpSqlExecutor.create(context,calciteRowMetaData, tableScan.getTargetName(), tableScan.getSql(), factory,params);
    }

    @Override
    public Executor implement(MycatLookUpView mycatLookUpView) {
        return MycatLookupExecutor.create(context,mycatLookUpView.getRelNode(), factory, params);
    }

    @Override
    public Executor implement(MycatInsertRel mycatInsertRel) {
        return MycatInsertExecutor.create(context,mycatInsertRel, factory, params);
    }

    @Override
    public Executor implement(MycatUpdateRel mycatUpdateRel) {
        if (mycatUpdateRel.isGlobal()){
            return new MycatGlobalUpdateExecutor(context,mycatUpdateRel.getValues(),
                    mycatUpdateRel.getSqlStatement(),
                    params,factory);
        }
        return MycatUpdateExecutor.create(context,mycatUpdateRel.getValues(),
                mycatUpdateRel.getSqlStatement(),
                factory,
                params
        );
    }
    @Override
    public Executor implement(QueryBuilder  builder){
       return builder.run();
    }

}