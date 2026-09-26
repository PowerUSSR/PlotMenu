package org.mineserver.plotmenu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlotMenuPlugin extends JavaPlugin implements Listener {

    private static final String TITLE_MAIN = "\u00A78\u00A7l\u041C\u0435\u043D\u044E \u0443\u0447\u0430\u0441\u0442\u043A\u043E\u0432";
    private static final String TITLE_PLOTS = "\u00A72\u00A7l\u0423\u0447\u0430\u0441\u0442\u043E\u043A";
    private static final String TITLE_PERSONAL = "\u00A7b\u00A7l\u041B\u0438\u0447\u043D\u044B\u0435 \u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438";
    private static final String TITLE_TOWN = "\u00A76\u00A7l\u0413\u043E\u0440\u043E\u0434";
    private static final String TITLE_INFO = "\u00A7e\u00A7l\u0418\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044F";

    private NamespacedKey actionKey;

    @Override
    public void onEnable() {
        actionKey = new NamespacedKey(this, "action");
        if (getCommand("plotmenu") != null) {
            getCommand("plotmenu").setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PlotMenu loaded!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return false;
        }
        Player player = (Player) sender;

        if (!isPlayerInTown(player)) {
            player.sendMessage("\u00A7cОшибка: \u00A7fВы не состоите в городе!");
            player.sendMessage("\u00A77Меню доступно только жителям города.");
            return false;
        }

        openMainMenu(player);
        return true;
    }

    private boolean isPlayerInTown(Player player) {
        try {
            Class<?> universeClass = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Method getInstance = universeClass.getMethod("getInstance");
            Object universe = getInstance.invoke(null);
            
            Method getResident = universeClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(universe, player.getUniqueId());
            
            if (resident == null) return false;
            
            Method getTownOrNull = resident.getClass().getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);
            
            return town != null;
        } catch (Exception ex) {
            getLogger().warning("Towny check failed: " + ex.getMessage());
            return false;
        }
    }

    // Проверка, является ли игрок мэром своего города (для доступа к разделу "Город")
    private boolean isPlayerMayor(Player player) {
        try {
            Class<?> universeClass = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Method getInstance = universeClass.getMethod("getInstance");
            Object universe = getInstance.invoke(null);

            Method getResident = universeClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(universe, player.getUniqueId());
            if (resident == null) return false;

            try {
                Method isMayor = resident.getClass().getMethod("isMayor");
                return (Boolean) isMayor.invoke(resident);
            } catch (NoSuchMethodException fallback) {
                Method getTownOrNull = resident.getClass().getMethod("getTownOrNull");
                Object town = getTownOrNull.invoke(resident);
                if (town == null) return false;
                Method getMayor = town.getClass().getMethod("getMayor");
                Object mayor = getMayor.invoke(town);
                return mayor != null && mayor.equals(resident);
            }
        } catch (Exception ex) {
            getLogger().warning("Mayor check failed: " + ex.getMessage());
            return false;
        }
    }

    // Проверка, владеет ли игрок участком, на котором стоит (для изменения настроек именно этого участка)
    private boolean isPlotOwner(Player player) {
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);

            Method getTownBlock = apiClass.getMethod("getTownBlock", Location.class);
            Object townBlock = getTownBlock.invoke(api, player.getLocation());
            if (townBlock == null) return false; // игрок не на территории города

            Class<?> universeClass = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object universe = universeClass.getMethod("getInstance").invoke(null);
            Object myResident = universeClass.getMethod("getResident", UUID.class).invoke(universe, player.getUniqueId());
            if (myResident == null) return false;

            Method hasResident = townBlock.getClass().getMethod("hasResident");
            if ((Boolean) hasResident.invoke(townBlock)) {
                // участок приватизирован лично - управлять может только его личный владелец
                Object plotResident = townBlock.getClass().getMethod("getResident").invoke(townBlock);
                return plotResident != null && plotResident.equals(myResident);
            }

            // участок никому лично не принадлежит (общая земля города) - им может управлять мэр этого города
            if (!isPlayerMayor(player)) return false;
            Object townBlockTown = townBlock.getClass().getMethod("getTown").invoke(townBlock);
            Object myTown = myResident.getClass().getMethod("getTownOrNull").invoke(myResident);
            return townBlockTown != null && townBlockTown.equals(myTown);
        } catch (Exception ex) {
            getLogger().warning("Plot owner check failed: " + ex.getMessage());
            return false;
        }
    }

    // Проверка, есть ли у игрока хотя бы один застолбленный участок (для предупреждения в "Личных настройках")
    private boolean hasAnyPlots(Player player) {
        try {
            Class<?> universeClass = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
            Object universe = universeClass.getMethod("getInstance").invoke(null);
            Object resident = universeClass.getMethod("getResident", UUID.class).invoke(universe, player.getUniqueId());
            if (resident == null) return false;

            Object townBlocks = resident.getClass().getMethod("getTownBlocks").invoke(resident);
            return townBlocks != null && !((java.util.Collection<?>) townBlocks).isEmpty();
        } catch (Exception ex) {
            getLogger().warning("Plots count check failed: " + ex.getMessage());
            return true; // при ошибке не мешаем открытию меню, просто не показываем предупреждение
        }
    }

    // ---------- ГЛАВНОЕ МЕНЮ (1 строка) ----------
    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE_MAIN);

        inv.setItem(0, createFiller());
        inv.setItem(1, createItem(Material.GRASS_BLOCK, "\u00A7a\u00A7lУЧАСТОК",
                Arrays.asList("\u00A77Управление вашим участком:", "\u00A77застолбить, продать, права доступа", "", "\u00A7eНажмите, чтобы открыть"),
                "cat:plots"));
        inv.setItem(2, createFiller());
        inv.setItem(3, createHead(player.getName(), "\u00A7b\u00A7lЛИЧНЫЕ НАСТРОЙКИ",
                Arrays.asList("\u00A77Ваши личные настройки:", "\u00A77PvP, взрывы, авто-режимы", "", "\u00A7eНажмите, чтобы открыть"),
                "cat:personal"));
        inv.setItem(4, createFiller());
        if (isPlayerMayor(player)) {
            inv.setItem(5, createItem(Material.BRICKS, "\u00A76\u00A7lГОРОД",
                    Arrays.asList("\u00A77Настройки вашего города:", "\u00A77PvP, взрывы, доступ, казна", "", "\u00A7eНажмите, чтобы открыть"),
                    "cat:town"));
        } else {
            inv.setItem(5, createItem(Material.GRAY_DYE, "\u00A78\u00A7lГОРОД",
                    Arrays.asList("\u00A77Доступно только мэру города"), null));
        }
        inv.setItem(6, createFiller());
        inv.setItem(7, createItem(Material.BOOK, "\u00A7e\u00A7lИНФОРМАЦИЯ",
                Arrays.asList("\u00A77Статус города, участки,", "\u00A77список жителей", "", "\u00A7eНажмите, чтобы открыть"),
                "cat:info"));
        inv.setItem(8, createItem(Material.BARRIER, "\u00A7c\u00A7lЗАКРЫТЬ", null, "nav:close"));

        player.openInventory(inv);
    }

    // ---------- УЧАСТОК (3 строки) ----------
    private void openPlotMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PLOTS);

        inv.setItem(0, createItem(Material.ARROW, "\u00A7f\u00A7lНАЗАД", null, "nav:back"));
        inv.setItem(8, createItem(Material.BARRIER, "\u00A7c\u00A7lЗАКРЫТЬ", null, "nav:close"));
        for (int i : new int[]{1, 2, 3, 4, 5, 6, 7}) inv.setItem(i, createFiller());

        inv.setItem(9, createItem(Material.EMERALD, "\u00A7a\u00A7lЗастолбить", Arrays.asList("\u00A77Забрать этот участок себе"), "cmd:plot claim"));
        inv.setItem(10, createItem(Material.REDSTONE, "\u00A7c\u00A7lОткрепить", Arrays.asList("\u00A77Отказаться от участка", "\u00A78(нужно быть владельцем)"), "pcmd:plot unclaim"));
        inv.setItem(11, createItem(Material.GOLD_INGOT, "\u00A76\u00A7lНа продажу", Arrays.asList("\u00A77Выставить участок на продажу", "\u00A78(нужно быть владельцем)"), "pcmd:plot forsale"));
        inv.setItem(12, createItem(Material.IRON_INGOT, "\u00A77\u00A7lСнять с продажи", Arrays.asList("\u00A77Убрать участок с продажи", "\u00A78(нужно быть владельцем)"), "pcmd:plot notforsale"));
        inv.setItem(13, createItem(Material.FLINT_AND_STEEL, "\u00A7c\u00A7lОгонь", Arrays.asList("\u00A77Разрешить/запретить огонь", "\u00A77на этом участке", "\u00A78(нужно быть владельцем)"), "pcmd:plot toggle fire"));
        inv.setItem(14, createItem(Material.DIAMOND_SWORD, "\u00A74\u00A7lPvP", Arrays.asList("\u00A77Разрешить/запретить PvP", "\u00A77на этом участке", "\u00A78(нужно быть владельцем)"), "pcmd:plot toggle pvp"));
        inv.setItem(15, createItem(Material.TNT, "\u00A7c\u00A7lВзрывы", Arrays.asList("\u00A77Разрешить/запретить взрывы", "\u00A77на этом участке", "\u00A78(нужно быть владельцем)"), "pcmd:plot toggle explosion"));
        inv.setItem(16, createItem(Material.WRITABLE_BOOK, "\u00A7e\u00A7lПрава доступа", Arrays.asList("\u00A77Открыть меню прав доступа", "\u00A78(нужно быть владельцем)"), "pcmd:plot perm gui"));
        inv.setItem(17, createItem(Material.ANVIL, "\u00A77\u00A7lСбросить права", Arrays.asList("\u00A77Сбросить права участка", "\u00A77по умолчанию", "\u00A78(нужно быть владельцем)"), "pcmd:plot perm reset"));

        inv.setItem(18, createItem(Material.PLAYER_HEAD, "\u00A7d\u00A7lДобавить друга", Arrays.asList("\u00A77Чтобы добавить друга, введите:", "\u00A7f/plot trust <ник>", "\u00A78(нужно быть владельцем)"), "phint:\u00A7eЧтобы добавить друга на участок, введите: \u00A7f/plot trust <ник>"));
        inv.setItem(19, createItem(Material.NAME_TAG, "\u00A7f\u00A7lПереименовать", Arrays.asList("\u00A77Чтобы задать имя, введите:", "\u00A7f/plot set name <имя>", "\u00A78(нужно быть владельцем)"), "phint:\u00A7eЧтобы переименовать участок, введите: \u00A7f/plot set name <имя>"));
        for (int i : new int[]{20, 21, 22, 23, 24, 25, 26}) inv.setItem(i, createFiller());

        player.openInventory(inv);
    }

    // ---------- ЛИЧНЫЕ НАСТРОЙКИ (3 строки) ----------
    private void openResidentMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PERSONAL);

        inv.setItem(0, createItem(Material.ARROW, "\u00A7f\u00A7lНАЗАД", null, "nav:back"));
        inv.setItem(8, createItem(Material.BARRIER, "\u00A7c\u00A7lЗАКРЫТЬ", null, "nav:close"));
        for (int i : new int[]{1, 2, 3, 4, 5, 6, 7}) inv.setItem(i, createFiller());

        inv.setItem(9, createItem(Material.COMPASS, "\u00A7b\u00A7lГраницы участков", Arrays.asList("\u00A77Показывать границы участков"), "cmd:resident toggle plotborder"));
        inv.setItem(10, createItem(Material.FILLED_MAP, "\u00A7b\u00A7lГраницы города", Arrays.asList("\u00A77Показывать границы города"), "cmd:resident toggle townborder"));
        inv.setItem(11, createItem(Material.OAK_SIGN, "\u00A7b\u00A7lЗаголовки", Arrays.asList("\u00A77Показывать заголовки", "\u00A77при входе/выходе"), "cmd:resident toggle bordertitles"));
        inv.setItem(12, createItem(Material.MAP, "\u00A7b\u00A7lКарта", Arrays.asList("\u00A77Показывать карту города"), "cmd:resident toggle map"));
        inv.setItem(13, createItem(Material.FLINT_AND_STEEL, "\u00A7c\u00A7lОгонь", Arrays.asList("\u00A77Разрешить/запретить огонь", "\u00A77на всех ваших участках"), "cmd:resident toggle fire"));
        inv.setItem(14, createItem(Material.DIAMOND_SWORD, "\u00A74\u00A7lPvP", Arrays.asList("\u00A77Разрешить/запретить PvP", "\u00A77на всех ваших участках"), "cmd:resident toggle pvp"));
        inv.setItem(15, createItem(Material.TNT, "\u00A7c\u00A7lВзрывы", Arrays.asList("\u00A77Разрешить/запретить взрывы", "\u00A77на всех ваших участках"), "cmd:resident toggle explosion"));
        inv.setItem(16, createItem(Material.ZOMBIE_HEAD, "\u00A72\u00A7lМонстры", Arrays.asList("\u00A77Разрешить/запретить спавн монстров", "\u00A77на всех ваших участках"), "cmd:resident toggle mobs"));
        inv.setItem(17, createItem(Material.GRASS_BLOCK, "\u00A7a\u00A7lАвто-застолбление", Arrays.asList("\u00A77Автоматически застолбливать", "\u00A77участки при ходьбе"), "cmd:resident toggle townclaim"));

        inv.setItem(18, createItem(Material.DIRT, "\u00A77\u00A7lАвто-открепление", Arrays.asList("\u00A77Автоматически откреплять", "\u00A77участки при ходьбе"), "cmd:resident toggle townunclaim"));
        inv.setItem(19, createItem(Material.RED_BED, "\u00A7d\u00A7lТочка возрождения", Arrays.asList("\u00A77Возрождаться на кровати", "\u00A77в городе"), "cmd:resident toggle bedspawn"));
        inv.setItem(20, createItem(Material.BARRIER, "\u00A7c\u00A7lСброс настроек", Arrays.asList("\u00A77Сбросить настройки", "\u00A77по умолчанию"), "cmd:resident toggle reset"));
        for (int i : new int[]{21, 22, 23, 24, 25, 26}) inv.setItem(i, createFiller());

        player.openInventory(inv);
    }

    // ---------- ГОРОД (4 строки, только для мэра) ----------
    private void openTownMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_TOWN);

        inv.setItem(0, createItem(Material.ARROW, "\u00A7f\u00A7lНАЗАД", null, "nav:back"));
        inv.setItem(8, createItem(Material.BARRIER, "\u00A7c\u00A7lЗАКРЫТЬ", null, "nav:close"));
        for (int i : new int[]{1, 2, 3, 4, 5, 6, 7}) inv.setItem(i, createFiller());

        inv.setItem(9, createItem(Material.FLINT_AND_STEEL, "\u00A7c\u00A7lОгонь", Arrays.asList("\u00A77Разрешить/запретить огонь", "\u00A77по всему городу"), "cmd:town toggle fire"));
        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "\u00A74\u00A7lPvP", Arrays.asList("\u00A77Разрешить/запретить PvP", "\u00A77по всему городу"), "cmd:town toggle pvp"));
        inv.setItem(11, createItem(Material.TNT, "\u00A7c\u00A7lВзрывы", Arrays.asList("\u00A77Разрешить/запретить взрывы", "\u00A77по всему городу"), "cmd:town toggle explosion"));
        inv.setItem(12, createItem(Material.ZOMBIE_HEAD, "\u00A72\u00A7lМонстры", Arrays.asList("\u00A77Разрешить/запретить спавн монстров", "\u00A77по всему городу"), "cmd:town toggle mobs"));
        inv.setItem(13, createItem(Material.IRON_DOOR, "\u00A76\u00A7lОткрытый город", Arrays.asList("\u00A77Разрешить всем строить", "\u00A77без приглашения"), "cmd:town toggle open"));
        inv.setItem(14, createItem(Material.OAK_DOOR, "\u00A76\u00A7lПубличный город", Arrays.asList("\u00A77Сделать город видимым", "\u00A77в списке городов"), "cmd:town toggle public"));
        inv.setItem(15, createItem(Material.BEACON, "\u00A7e\u00A7lЗона нации", Arrays.asList("\u00A77Переключить зону нации"), "cmd:town toggle nationzone"));
        inv.setItem(16, createItem(Material.PLAYER_HEAD, "\u00A7d\u00A7lПригласить жителя", Arrays.asList("\u00A77Чтобы пригласить игрока, введите:", "\u00A7f/town invite <ник>"), "hint:\u00A7eЧтобы пригласить игрока в город, введите: \u00A7f/town invite <ник>"));
        inv.setItem(17, createItem(Material.BARRIER, "\u00A7c\u00A7lИсключить жителя", Arrays.asList("\u00A77Чтобы исключить игрока, введите:", "\u00A7f/town kick <ник>"), "hint:\u00A7eЧтобы исключить жителя из города, введите: \u00A7f/town kick <ник>"));

        inv.setItem(18, createItem(Material.GOLDEN_HELMET, "\u00A76\u00A7lНазначить ранг", Arrays.asList("\u00A77Чтобы выдать должность, введите:", "\u00A7f/town rank add <ник> <ранг>"), "hint:\u00A7eЧтобы назначить ранг жителю, введите: \u00A7f/town rank add <ник> <ранг>"));
        inv.setItem(19, createItem(Material.EMERALD, "\u00A7a\u00A7lВнести деньги", Arrays.asList("\u00A77Чтобы пополнить казну, введите:", "\u00A7f/town deposit <сумма>"), "hint:\u00A7eЧтобы внести деньги в казну города, введите: \u00A7f/town deposit <сумма>"));
        inv.setItem(20, createItem(Material.GOLD_INGOT, "\u00A76\u00A7lСнять деньги", Arrays.asList("\u00A77Чтобы снять деньги, введите:", "\u00A7f/town withdraw <сумма>"), "hint:\u00A7eЧтобы снять деньги из казны города, введите: \u00A7f/town withdraw <сумма>"));
        inv.setItem(21, createItem(Material.RED_BED, "\u00A7d\u00A7lТочка возрождения", Arrays.asList("\u00A77Установить точку возрождения", "\u00A77города здесь, где вы стоите"), "cmd:town set spawn"));
        inv.setItem(22, createItem(Material.ENDER_PEARL, "\u00A7b\u00A7lАванпост", Arrays.asList("\u00A77Установить аванпост города", "\u00A77здесь, где вы стоите"), "cmd:town outpost"));
        inv.setItem(23, createItem(Material.OAK_SIGN, "\u00A7f\u00A7lДоска объявлений", Arrays.asList("\u00A77Чтобы задать текст, введите:", "\u00A7f/town set board <текст>"), "hint:\u00A7eЧтобы задать доску объявлений, введите: \u00A7f/town set board <текст>"));
        inv.setItem(24, createItem(Material.NAME_TAG, "\u00A7f\u00A7lТег города", Arrays.asList("\u00A77Чтобы задать короткий тег, введите:", "\u00A7f/town set tag <тег>"), "hint:\u00A7eЧтобы задать тег города, введите: \u00A7f/town set tag <тег>"));
        inv.setItem(25, createItem(Material.CLOCK, "\u00A7b\u00A7lОнлайн жители", Arrays.asList("\u00A77Показать жителей города,", "\u00A77которые сейчас на сервере"), "cmd:town online"));

        for (int i : new int[]{26, 27, 28, 29, 30, 31, 32, 33, 34, 35}) inv.setItem(i, createFiller());

        player.openInventory(inv);
    }

    // ---------- ИНФОРМАЦИЯ (1 строка) ----------
    private void openInfoMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE_INFO);

        inv.setItem(0, createItem(Material.ARROW, "\u00A7f\u00A7lНАЗАД", null, "nav:back"));
        inv.setItem(1, createFiller());
        inv.setItem(2, createItem(Material.WRITTEN_BOOK, "\u00A7e\u00A7lСтатус города", Arrays.asList("\u00A77Показать общую информацию", "\u00A77о вашем городе"), "cmd:town"));
        inv.setItem(3, createFiller());
        inv.setItem(4, createItem(Material.CLOCK, "\u00A76\u00A7lОнлайн жители", Arrays.asList("\u00A77Показать жителей города,", "\u00A77которые сейчас на сервере"), "cmd:town online"));
        inv.setItem(5, createFiller());
        inv.setItem(6, createItem(Material.PLAYER_HEAD, "\u00A7b\u00A7lЖители", Arrays.asList("\u00A77Показать полный список жителей", "\u00A77города"), "cmd:town reslist"));
        inv.setItem(7, createFiller());
        inv.setItem(8, createItem(Material.BARRIER, "\u00A7c\u00A7lЗАКРЫТЬ", null, "nav:close"));

        player.openInventory(inv);
    }

    private boolean isOurMenu(String title) {
        return title.equals(TITLE_MAIN) || title.equals(TITLE_PLOTS) || title.equals(TITLE_PERSONAL)
                || title.equals(TITLE_TOWN) || title.equals(TITLE_INFO);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (!isOurMenu(title)) return;

        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        if (action.equals("nav:close")) {
            p.closeInventory();
        } else if (action.equals("nav:back")) {
            openMainMenu(p);
        } else if (action.startsWith("cat:")) {
            switch (action.substring(4)) {
                case "plots": openPlotMenu(p); break;
                case "personal":
                    if (!hasAnyPlots(p)) {
                        p.sendMessage("§e⚠ У вас пока нет ни одного участка.");
                        p.sendMessage("§7Эти настройки применятся автоматически, когда вы застолбите первый участок (§f/plot claim§7).");
                    }
                    openResidentMenu(p);
                    break;
                case "town":
                    if (isPlayerMayor(p)) {
                        openTownMenu(p);
                    } else {
                        p.sendMessage("§cТолько мэр города может открыть это меню.");
                    }
                    break;
                case "info": openInfoMenu(p); break;
            }
        } else if (action.startsWith("cmd:")) {
            p.performCommand(action.substring(4));
            p.closeInventory();
        } else if (action.startsWith("hint:")) {
            p.closeInventory();
            p.sendMessage(action.substring(5));
        } else if (action.startsWith("pcmd:")) {
            p.closeInventory();
            if (!isPlotOwner(p)) {
                p.sendMessage("§cВы не являетесь владельцем этого участка!");
                p.sendMessage("§7Сначала займите его: §f/plot claim");
                return;
            }
            p.performCommand(action.substring(5));
        } else if (action.startsWith("phint:")) {
            p.closeInventory();
            if (!isPlotOwner(p)) {
                p.sendMessage("§cВы не являетесь владельцем этого участка!");
                p.sendMessage("§7Сначала займите его: §f/plot claim");
                return;
            }
            p.sendMessage(action.substring(6));
        }
    }

    private ItemStack createItem(Material mat, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHead(String ownerName, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
