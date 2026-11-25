package ru.rooyzee.elytrixairdrop.gui;
import org.bukkit.entity.Player;
import ru.rooyzee.elytrixairdrop.Main;
import ru.rooyzee.elytrixairdrop.utils.ColorUtil;
public class SkySettingsMenu {
 public static final String TITLE=ColorUtil.colorize("&#F8BEFBАирдроп &8» &bВоздушный");
 private final Main plugin;
 public SkySettingsMenu(Main plugin){this.plugin=plugin;}
 public void open(Player player){new AirdropSettingsMenu(plugin,"sky").open(player);}
}
