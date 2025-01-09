package fun.xiantiao.mcpacker.configs;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.SubSection;

@Deprecated
@SuppressWarnings("unused")
@ConfHeader({"# 配置文件 MCPacker Build Setting ver 1.0.0 by xiantiao","# 关于旧插件迁移"})
public interface BuildSetting {
    @ConfDefault.DefaultBoolean(true)
    @ConfComments({"\n# 是否启用 ","# 执行/itemkeep migrate 迁移"})
    boolean enable();
}
