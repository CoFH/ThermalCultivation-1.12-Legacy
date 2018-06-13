package cofh.thermalcultivation.proxy;

import cofh.thermalcultivation.gui.container.ContainerSeedBag;
import cofh.thermalcultivation.item.ItemSeedBag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventHandler {

	public static final EventHandler INSTANCE = new EventHandler();

	@SubscribeEvent
	public void handleEntityItemPickup(EntityItemPickupEvent event) {

		EntityPlayer player = event.getEntityPlayer();
		if (player.openContainer instanceof ContainerSeedBag) {
			return;
		}
		InventoryPlayer inventory = player.inventory;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.getItem() instanceof ItemSeedBag && ItemSeedBag.onItemPickup(event, stack)) {
				event.setCanceled(true);
				return;
			}
		}
	}

}