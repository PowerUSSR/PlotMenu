package org.mineserver.plotmenu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotMenuPlugin extends JavaPlugin implements Listener {
    private static final Map<Player, Integer> PLAYER_PAGES = new HashMap<>();
    private static final int MAIN_PAGE = 0;
    private static final int PLOT_PAGE = 1;
    private static final int RESIDENT_PAGE = 2;
    private static final int TOWN_PAGE = 3;

    @Override
    public void onEnable() {
        getCommand("plotmenu").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("§a✓ PlotMenu loaded! Use §f/plotmenu");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players!");
            return false;
        }
        Player player = (Player) sender;
        PLAYER_PAGES.put(player, MAIN_PAGE);
        openMainMenu(player);
        return true;
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§6§lТОУНИ МЕНЮ");
        
        // Заголовок строка
        inv.setItem(0, createBarItem(Material.GOLD_BLOCK, "§6TOWNY МЕНЮ"));
        inv.setItem(1, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(2, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(3, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(4, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(5, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(6, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(7, createBarItem(Material.GOLD_BLOCK, ""));
        inv.setItem(8, createBarItem(Material.GOLD_BLOCK, ""));

        // Основные категории (3x3)
        // Участок (Plot)
        inv.setItem(11, createCategoryItem(Material.GRASS_BLOCK, "§e§lУЧАСТОК", 
            "Управление личным участком\n" +
            "§7• Захват/освобождение\n" +
            "§7• Защита участка\n" +
            "§7• Права доступа"));

        // Персональные настройки (Resident)
        inv.setItem(13, createCategoryItem(Material.PLAYER_HEAD, "§b§lЛИЧНЫЕ", 
            "Ваши персональные\n" +
            "§7• Границы участков\n" +
            "§7• Общие переключатели"));

        // Город (Town)
        inv.setItem(15, createCategoryItem(Material.BRICKS, "§9§lГОРОД", 
            "Настройки города\n" +
            "§7• Защита города\n" +
            "§7• Опции для мэра"));

        // Справка
        inv.setItem(29, createCategoryItem(Material.PAPER, "§d§lСПРАВКА", 
            "Полезная информация\n" +
            "§7• Команды Towny\n" +
            "§7• Советы и хинты"));

        // Нижняя строка оформления
        for (int i = 36; i <= 44; i++) {
            inv.setItem(i, createBarItem(Material.GOLD_BLOCK, ""));
        }

        // Закрыть кнопка
        inv.setItem(40, createCloseItem());

        player.openInventory(inv);
    }

    private void openPlotMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6УЧАСТОК - Управление");
        
        // Заголовок
        addMenuHeader(inv, "§eУЧАСТОК");

        // Ряд 1: Базовые действия
        inv.setItem(10, createActionItem(Material.EMERALD_ORE, "§a§lЗАХВАТ", 
            "Захватить участок\n§7/plot claim", "plot_claim"));
        inv.setItem(12, createActionItem(Material.REDSTONE_ORE, "§c§lОСВОБОДИТЬ", 
            "Освободить участок\n§7/plot unclaim", "plot_unclaim"));
        inv.setItem(14, createActionItem(Material.DIAMOND_ORE, "§b§lПРОДАТЬ", 
            "Выставить на продажу\n§7/plot forsale", "plot_forsale"));
        inv.setItem(16, createActionItem(Material.IRON_ORE, "§f§lСНЯТЬ С ПРОДАЖИ", 
            "Снять с продажи\n§7/plot notforsale", "plot_nfs"));

        // Ряд 2: Защита участка
        inv.setItem(19, createToggleItem(Material.FIRE_CHARGE, "§c§lОГОНЬ", 
            "Защита от огня\n§7/plot toggle fire", "plot_toggle_fire"));
        inv.setItem(21, createToggleItem(Material.DIAMOND_SWORD, "§4§lPVP", 
            "Защита от PVP\n§7/plot toggle pvp", "plot_toggle_pvp"));
        inv.setItem(23, createToggleItem(Material.TNT, "§e§lВЗРЫВЫ", 
            "Защита от взрывов\n§7/plot toggle explosion", "plot_toggle_explosion"));
        inv.setItem(25, createToggleItem(Material.ZOMBIE_HEAD, "§5§lМОБЫ", 
            "Защита от мобов\n§7/plot toggle mob", "plot_toggle_mob"));

        // Ряд 3: Расширенные настройки
        inv.setItem(28, createActionItem(Material.WRITABLE_BOOK, "§6§lПРАВА ДОСТУПА", 
            "Настроить права\n§7/plot perm gui", "plot_perm_gui"));
        inv.setItem(30, createActionItem(Material.GOLD_BLOCK, "§e§lТИП УЧАСТКА", 
            "Shop/Inn/Jail/Farm/Bank\n§7/plot set", "plot_set_type"));
        inv.setItem(32, createActionItem(Material.HEART_OF_THE_SEA, "§b§lДОВЕРИЕ", 
            "Доверить другому игроку\n§7/plot trust add", "plot_trust"));
        inv.setItem(34, createActionItem(Material.CLOCK, "§d§lНАИМЕНОВАНИЕ", 
            "Назвать участок\n§7/plot set name", "plot_rename"));

        // Навигация и закрыть
        inv.setItem(47, createNavItem(Material.ARROW, "§a◀ НАЗАД", "main_menu"));
        inv.setItem(49, createCloseItem());
        inv.setItem(51, createEmptyItem());

        player.openInventory(inv);
    }

    private void openResidentMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§b§lЛИЧНЫЕ НАСТРОЙКИ");
        
        addMenuHeader(inv, "§bЛИЧНО");

        // Ряд 1: Границы
        inv.setItem(10, createToggleItem(Material.COMPASS, "§e§lГРАНИЦА УЧАСТКА", 
            "Показывать границы\n§7/resident toggle plotborder", "res_toggle_border"));
        inv.setItem(12, createToggleItem(Material.CYAN_CONCRETE, "§b§lГРАНИЦА ГОРОДА", 
            "Показывать границы города\n§7/resident toggle townborder", "res_toggle_town_border"));
        inv.setItem(14, createToggleItem(Material.BLUE_CONCRETE, "§d§lНАЗВАНИЯ ГРАНИЦ", 
            "Показывать названия\n§7/resident toggle bordertitles", "res_toggle_titles"));
        inv.setItem(16, createToggleItem(Material.MAP, "§6§lКАРТА", 
            "Автообновляемая карта\n§7/resident toggle map", "res_toggle_map"));

        // Ряд 2: Персональная защита
        inv.setItem(19, createToggleItem(Material.FIRE_CHARGE, "§c§lОГОНЬ", 
            "Защита участков от огня\n§7/resident toggle fire", "res_toggle_fire"));
        inv.setItem(21, createToggleItem(Material.DIAMOND_SWORD, "§4§lPVP", 
            "Защита участков от PVP\n§7/resident toggle pvp", "res_toggle_pvp"));
        inv.setItem(23, createToggleItem(Material.TNT, "§e§lВЗРЫВЫ", 
            "Защита от взрывов\n§7/resident toggle explosion", "res_toggle_explosion"));
        inv.setItem(25, createToggleItem(Material.ZOMBIE_HEAD, "§5§lМОБЫ", 
            "Защита от враждебных мобов\n§7/resident toggle mobs", "res_toggle_mobs"));

        // Ряд 3: Режимы строительства
        inv.setItem(28, createToggleItem(Material.GRASS_BLOCK, "§a§lАВТОЗАХВАТ", 
            "Автоматический захват при ходьбе\n§7/resident toggle townclaim", "res_auto_claim"));
        inv.setItem(30, createToggleItem(Material.DIRT, "§8§lАВТООСВОБОЖДЕНИЕ", 
            "Автоматическое освобождение\n§7/resident toggle townunclaim", "res_auto_unclaim"));
        inv.setItem(32, createToggleItem(Material.BED, "§c§lСПАВН НА КРОВАТИ", 
            "Спавниться на кровати\n§7/resident toggle bedspawn", "res_bedspawn"));
        inv.setItem(34, createToggleItem(Material.BARRIER, "§4§lRESET", 
            "Отключить все режимы\n§7/resident toggle reset", "res_reset"));

        inv.setItem(47, createNavItem(Material.ARROW, "§a◀ НАЗАД", "main_menu"));
        inv.setItem(49, createCloseItem());

        player.openInventory(inv);
    }

    private void openTownMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§9§lГОРОД - НАСТРОЙКИ");
        
        addMenuHeader(inv, "§9ГОРОД");

        // Защита города
        inv.setItem(10, createToggleItem(Material.FIRE_CHARGE, "§c§lОГОНЬ", 
            "Защита города от огня\n§7/town toggle fire", "town_toggle_fire"));
        inv.setItem(12, createToggleItem(Material.DIAMOND_SWORD, "§4§lPVP", 
            "Защита города от PVP\n§7/town toggle pvp", "town_toggle_pvp"));
        inv.setItem(14, createToggleItem(Material.TNT, "§e§lВЗРЫВЫ", 
            "Защита от взрывов\n§7/town toggle explosion", "town_toggle_explosion"));
        inv.setItem(16, createToggleItem(Material.ZOMBIE_HEAD, "§5§lМОБЫ", 
            "Защита от мобов\n§7/town toggle mobs", "town_toggle_mobs"));

        // Настройки доступа
        inv.setItem(19, createToggleItem(Material.IRON_DOOR, "§f§lОТКРЫТЫЙ ГОРОД", 
            "Открыть для присоединения\n§7/town toggle open", "town_toggle_open"));
        inv.setItem(21, createToggleItem(Material.OAK_DOOR, "§8§lПУБЛИЧНЫЙ ГОРОД", 
            "Показывать в списке\n§7/town toggle public", "town_toggle_public"));
        inv.setItem(23, createActionItem(Material.GOLD_BLOCK, "§6§lНИЦОНЭЛ ЗОНА", 
            "Настройки национальной зоны\n§7/town toggle nationzone", "town_nation_zone"));

        // Информация для мэров
        inv.setItem(28, createActionItem(Material.WRITABLE_BOOK, "§e§lСОСТАНИЕ ГОРОДА", 
            "Информация о городе\n§7/town status", "town_status"));
        inv.setItem(30, createActionItem(Material.CHEST, "§6§lУПРАВЛЕНИЕ УЧАСТКАМИ", 
            "Список участков города\n§7/town plots", "town_plots"));
        inv.setItem(32, createActionItem(Material.BOOK, "§d§lРЕСИДЕНТЫ", 
            "Список жителей\n§7/town reslist", "town_reslist"));

        inv.setItem(47, createNavItem(Material.ARROW, "§a◀ НАЗАД", "main_menu"));
        inv.setItem(49, createCloseItem());

        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        
        if (!e.getView().getTitle().contains("МЕНЮ") && 
            !e.getView().getTitle().contains("УЧАСТОК") && 
            !e.getView().getTitle().contains("ЛИЧНЫЕ") && 
            !e.getView().getTitle().contains("ГОРОД")) return;
        
        e.setCancelled(true);
        
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String action = clicked.getItemMeta().getDisplayName();
        
        // Категории главного меню
        if (e.getView().getTitle().contains("TOWNY МЕНЮ")) {
            if (action.contains("УЧАСТОК")) {
                PLAYER_PAGES.put(p, PLOT_PAGE);
                openPlotMenu(p);
            } else if (action.contains("ЛИЧНЫЕ")) {
                PLAYER_PAGES.put(p, RESIDENT_PAGE);
                openResidentMenu(p);
            } else if (action.contains("ГОРОД")) {
                PLAYER_PAGES.put(p, TOWN_PAGE);
                openTownMenu(p);
            }
            return;
        }

        String cmd = getCmdFromAction(action);
        if (cmd != null) {
            p.performCommand(cmd);
            p.closeInventory();
        } else if (action.contains("◀ НАЗАД")) {
            PLAYER_PAGES.put(p, MAIN_PAGE);
            openMainMenu(p);
        } else if (action.contains("CLOSE")) {
            p.closeInventory();
        }
    }

    private String getCmdFromAction(String action) {
        // Parse command from action string
        if (action.contains("ЗАХВАТ")) return "plot claim";
        if (action.contains("ОСВОБОДИТЬ")) return "plot unclaim";
        if (action.contains("ПРОДАТЬ")) return "plot forsale";
        if (action.contains("СНЯТЬ")) return "plot notforsale";
        if (action.contains("plot_toggle_fire")) return "plot toggle fire";
        if (action.contains("plot_toggle_pvp")) return "plot toggle pvp";
        if (action.contains("plot_toggle_explosion")) return "plot toggle explosion";
        if (action.contains("plot_toggle_mob")) return "plot toggle mob";
        if (action.contains("plot_perm_gui")) return "plot perm gui";
        if (action.contains("plot_trust")) return "plot trust";
        if (action.contains("res_toggle_border")) return "resident toggle plotborder";
        if (action.contains("res_toggle_town_border")) return "resident toggle townborder";
        if (action.contains("res_toggle_titles")) return "resident toggle bordertitles";
        if (action.contains("res_toggle_map")) return "resident toggle map";
        if (action.contains("res_toggle_fire")) return "resident toggle fire";
        if (action.contains("res_toggle_pvp")) return "resident toggle pvp";
        if (action.contains("res_toggle_explosion")) return "resident toggle explosion";
        if (action.contains("res_toggle_mobs")) return "resident toggle mobs";
        if (action.contains("res_auto_claim")) return "resident toggle townclaim";
        if (action.contains("res_auto_unclaim")) return "resident toggle townunclaim";
        if (action.contains("res_bedspawn")) return "resident toggle bedspawn";
        if (action.contains("res_reset")) return "resident toggle reset";
        if (action.contains("town_toggle_fire")) return "town toggle fire";
        if (action.contains("town_toggle_pvp")) return "town toggle pvp";
        if (action.contains("town_toggle_explosion")) return "town toggle explosion";
        if (action.contains("town_toggle_mobs")) return "town toggle mobs";
        if (action.contains("town_toggle_open")) return "town toggle open";
        if (action.contains("town_toggle_public")) return "town toggle public";
        return null;
    }

    private void addMenuHeader(Inventory inv, String title) {
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, createBarItem(Material.GOLD_BLOCK, title));
        }
    }

    private ItemStack createBarItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCategoryItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lores = new ArrayList<>();
            for (String line : lore.split("\n")) {
                lores.add(line);
            }
            meta.setLore(lores);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createActionItem(Material mat, String name, String lore, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " " + action);
            List<String> lores = new ArrayList<>();
            for (String line : lore.split("\n")) {
                lores.add(line);
            }
            meta.setLore(lores);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleItem(Material mat, String name, String lore, String action) {
        return createActionItem(mat, name, lore + "\n§7Нажми для переключения", action);
    }

    private ItemStack createNavItem(Material mat, String name, String page) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " " + page);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c✕ CLOSE");
            List<String> lores = new ArrayList<>();
            lores.add("§7Закрыть меню");
            meta.setLore(lores);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEmptyItem() {
        return new ItemStack(Material.AIR);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        PLAYER_PAGES.remove(e.getPlayer());
    }
}
