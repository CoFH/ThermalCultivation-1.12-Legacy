package cofh.thermalcultivation.gui;

import cofh.core.util.helpers.ItemHelper;
import cofh.thermalcultivation.gui.client.GuiSeedBag;
import cofh.thermalcultivation.gui.container.ContainerSeedBag;
import cofh.thermalcultivation.init.TCItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {

    public static final int SEED_BAG_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {

        switch (id) {
            case SEED_BAG_ID:
                if(ItemHelper.isPlayerHoldingMainhand(TCItems.itemSeedBag, player)) {
                    return new ContainerSeedBag(player.getHeldItemMainhand(), player.inventory);
                }
                return null;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {

        switch (id) {
            case SEED_BAG_ID:
                if(ItemHelper.isPlayerHoldingMainhand(TCItems.itemSeedBag, player)) {
                    return new GuiSeedBag(player.inventory, new ContainerSeedBag(player.getHeldItemMainhand(), player.inventory));
                }
                return null;
            default:
                return null;
        }
    }
}
