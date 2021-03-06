package com.willfp.ecoenchants.display.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.willfp.ecoenchants.display.AbstractPacketAdapter;
import com.willfp.ecoenchants.display.EnchantDisplay;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;

public final class PacketSetSlot extends AbstractPacketAdapter {
    public PacketSetSlot() {
        super(PacketType.Play.Server.SET_SLOT);
    }

    @Override
    public void onSend(PacketContainer packet) {
        packet.getItemModifier().modify(0, (item) -> {
            boolean hideEnchants = false;

            if(item == null)
                return item;

            if(item.getItemMeta() != null) {
                hideEnchants = item.getItemMeta().getItemFlags().contains(ItemFlag.HIDE_ENCHANTS);
                if(hideEnchants && item.getItemMeta().getPersistentDataContainer().has(EnchantDisplay.KEY, PersistentDataType.INTEGER))
                    hideEnchants = false;
            }

            item = EnchantDisplay.displayEnchantments(item, hideEnchants);
            return item;
        });
    }
}
