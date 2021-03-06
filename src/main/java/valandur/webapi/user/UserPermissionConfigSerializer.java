package valandur.webapi.user;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import valandur.webapi.WebAPI;
import valandur.webapi.api.util.TreeNode;

public class UserPermissionConfigSerializer implements TypeSerializer<UserPermission> {

    @Override
    public UserPermission deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        String username = value.getKey().toString();
        String password = value.getNode("password").getString();

        TreeNode<String, Boolean> perms = WebAPI.getPermissionService().permissionTreeFromConfig(value.getNode("permissions"));

        return new UserPermission(username, password, perms);
    }

    @Override
    public void serialize(TypeToken<?> type, UserPermission obj, ConfigurationNode value) throws ObjectMappingException {
        value.getNode("password").setValue(obj.getPassword());
        WebAPI.getPermissionService().permissionTreeToConfig(value.getNode("permissions"), obj.getPermissions());
    }
}
