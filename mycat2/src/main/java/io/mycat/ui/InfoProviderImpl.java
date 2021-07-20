package io.mycat.ui;

import com.google.common.collect.ImmutableMap;
import io.mycat.MetaClusterCurrent;
import io.mycat.MetadataManager;
import io.mycat.TableHandler;
import io.mycat.calcite.table.SchemaHandler;
import io.mycat.config.*;
import io.mycat.util.NameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InfoProviderImpl implements InfoProvider {
    @Override
    public List<SchemaHandler> schemas() {
        MetadataManager mycatRouterConfig = MetaClusterCurrent.wrapper(MetadataManager.class);
        return new ArrayList<>(mycatRouterConfig.getSchemaMap().values());
    }

    @Override
    public List<ClusterConfig> clusters() {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        return mycatRouterConfig.getClusters();
    }

    @Override
    public List<DatasourceConfig> datasources() {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        return mycatRouterConfig.getDatasources();
    }

    @Override
    public Optional<LogicSchemaConfig> getSchemaConfigByName(String schemaName) {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        for (LogicSchemaConfig schema : mycatRouterConfig.getSchemas()) {
            if (schema.getSchemaName().equals(schemaName)) {
                return Optional.ofNullable(schema);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<TableHandler> getTableConfigByName(String schemaName, String tableName) {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        MetadataManager metadataManager = MetaClusterCurrent.wrapper(MetadataManager.class);
        TableHandler table = metadataManager.getTable(schemaName, tableName);
        return Optional.ofNullable(table);
    }

    @Override
    public Optional<DatasourceConfig> getDatasourceConfigByPath(String name) {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        for (DatasourceConfig datasourceConfig : mycatRouterConfig.getDatasources()) {
            if (name.equals(datasourceConfig.getName())) {
                return Optional.ofNullable(datasourceConfig);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ClusterConfig> getClusterConfigByPath(String path) {
        MycatRouterConfig mycatRouterConfig = MetaClusterCurrent.wrapper(MycatRouterConfig.class);
        for (ClusterConfig cluster : mycatRouterConfig.getClusters()) {
            if (path.equals(cluster.getName())) {
                return Optional.ofNullable(cluster);
            }
        }
        return Optional.empty();


    }

    @Override
    public String translate(String name) {
        return map.get(name, false);
    }

    NameMap<String> map = NameMap.immutableCopyOf((ImmutableMap)
            ImmutableMap.builder()
                    .put("schemaName", "库名")
                    .put("defaultTargetName", "默认映射库目标")
                    .build());

}