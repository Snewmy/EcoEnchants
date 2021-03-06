package com.willfp.ecoenchants.enchantments.ecoenchants.normal;

import com.willfp.ecoenchants.enchantments.EcoEnchant;
import com.willfp.ecoenchants.enchantments.EcoEnchants;
import com.willfp.ecoenchants.enchantments.util.EnchantmentUtils;
import com.willfp.ecoenchants.util.LightningUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
public final class Zeus extends EcoEnchant {
    public Zeus() {
        super(
                "zeus", EnchantmentType.NORMAL
        );
    }

    // START OF LISTENERS


    @Override
    public void onArrowDamage(LivingEntity attacker, LivingEntity victim, Arrow arrow, int level, EntityDamageByEntityEvent event) {
        if(!EnchantmentUtils.passedChance(this, level))
            return;

        double damage = this.getConfig().getDouble(EcoEnchants.CONFIG_LOCATION + "lightning-damage");

        LightningUtils.strike(victim, damage);
    }
}
